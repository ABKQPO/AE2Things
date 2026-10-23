package com.asdflj.ae2thing.util.enhancedsearch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.asdflj.ae2thing.AE2Thing;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@SideOnly(Side.CLIENT)
public final class RecipeSearchAlias {

    private static final String ALIAS_FILE = "search_alias.txt";
    private static final String INDEX_FILE = "search_index.txt";
    private static final String INDEX_HEADER = "# key=";
    private static final String INDEX_VERSION = "# ae2thing search index v2";
    private static final String TITLE_LINE = "#title\t";
    private static final int PREFIX_LIMIT = 32;

    private static final Logger LOG = LogManager.getLogger(AE2Thing.MODID);

    private static final String[] ID_PREFIXES = { "gt.recipe.category.", "gt.recipe.", "gregtech.recipe.category.",
        "gregtech.recipe." };

    private static final String[][] VARIANT_WORDS = { { "recycling", "回收", "回收利用" }, { "recycle", "回收" },
        { "recycler", "回收" }, { "recycle_recipe", "回收" } };

    private static final String[] TEMPLATE = { "# AE2Things - dual interface terminal, enhanced search aliases",
        "# AE2Things - 双接口终端 强化搜索别名", "#", "# 格式 / format:   <alias> = <identifier>[, <identifier>...]", "#",
        "#   alias      = 你在搜索框 @ 后面输入的短名", "#   identifier = 被它代表的 handler id / overlay id / GT 配方分类",
        "#   一个别名可以对应多个 identifier, 用英文逗号分隔, 也可以写多行同名别名", "#", "# 例 / example:   1 = smelting",
        "#   之后输入 @1 等价于 @smelting, 只显示把 smelting 登记为催化剂的接口(原版熔炉, GT 电炉/蒸汽熔炉/工业熔炉等)", "#",
        "# 例 / example:   提取机 = gt.recipe.fluidextractor, gt.recipe.category.fluid_extractor_recycling",
        "#   基础页和派生页(回收页)一起匹配 / base page and its derived pages are matched together", "#",
        "# identifier 怎么找 / where to find it:",
        "#   NEI 页面:   IRecipeHandler#getOverlayIdentifier(), 例如 smelting / brewing",
        "#   GT 配方页:  recipeCategory.unlocalizedName, 例如 gt.recipe.assembler", "#",
        "# 搜索框的页标题与分类名由内置索引自动提供, 不需要写在这里 / page titles and category names are indexed",
        "# automatically, only write your own short names here.",
        "# 只有在\"强化搜索\"开启且查询以 @ 开头时才会用到别名 / only used by the @ query of the enhanced search.",
        "# 文件按修改时间热重载 / hot reloaded by the file modification time.", "" };

    private static final Object2ObjectOpenHashMap<String, List<String>> userAliases = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, List<String>> index = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, String> indexTitles = new Object2ObjectOpenHashMap<>();

    private static File aliasFile;
    private static File indexFile;
    private static boolean aliasTemplateWritten;
    private static long aliasStamp = Long.MIN_VALUE;
    private static boolean indexReady;

    private RecipeSearchAlias() {}

    public static List<String> resolve(String head) {
        if (head == null) return Collections.emptyList();
        final String key = normalize(head);
        if (key.isEmpty()) return Collections.emptyList();
        loadAliases();
        final List<String> user = userAliases.get(key);
        if (user != null && !user.isEmpty()) return copyDistinct(user);
        ensureIndex();
        final List<String> exact = index.get(key);
        if (exact != null && !exact.isEmpty()) return copyDistinct(exact);
        if (hasCatalysts(head.trim())) return Collections.singletonList(head.trim());
        final List<String> family = familyByPrefix(key);
        if (!family.isEmpty()) return family;
        return contains(key);
    }

    public static String titleOf(String id) {
        if (id == null) return null;
        ensureIndex();
        return indexTitles.get(id);
    }

    public static List<String> suggestions(String head, int limit) {
        final List<String> result = new ObjectArrayList<>();
        final String key = normalize(head);
        if (key.isEmpty()) return result;
        loadAliases();
        ensureIndex();
        for (String candidate : userAliases.keySet()) {
            addSuggestion(result, candidate, key, limit);
        }
        for (String candidate : index.keySet()) {
            addSuggestion(result, candidate, key, limit);
        }
        return result;
    }

    public static void loadAliases() {
        final File target = aliasFile();
        if (!target.isFile() && !aliasTemplateWritten) {
            aliasTemplateWritten = true;
            writeText(target, String.join(System.lineSeparator(), TEMPLATE));
        }
        final long modified = target.isFile() ? target.lastModified() : 0L;
        if (modified == aliasStamp) return;
        aliasStamp = modified;
        userAliases.clear();
        if (!target.isFile()) return;
        int lineNumber = 0;
        for (String line : readLines(target)) {
            lineNumber++;
            final String trimmed = line.trim();
            if (isComment(trimmed)) continue;
            final int equals = trimmed.indexOf('=');
            if (equals <= 0) {
                LOG.warn("{}:{} is not an 'alias = identifier' line, skipped", ALIAS_FILE, lineNumber);
                continue;
            }
            final String alias = normalize(trimmed.substring(0, equals));
            final String identifiers = trimmed.substring(equals + 1);
            if (alias.isEmpty() || identifiers.trim()
                .isEmpty()) {
                LOG.warn("{}:{} is missing the alias or the identifier, skipped", ALIAS_FILE, lineNumber);
                continue;
            }
            for (String raw : identifiers.split(",")) {
                final String identifier = raw.trim();
                if (identifier.isEmpty()) continue;
                addValue(userAliases, alias, identifier);
            }
        }
    }

    public static void loadCache() {
        final File target = indexFile();
        if (!target.isFile()) return;
        final String expected = INDEX_HEADER + fingerprint();
        final Object2ObjectOpenHashMap<String, List<String>> loaded = new Object2ObjectOpenHashMap<>();
        final Object2ObjectOpenHashMap<String, String> loadedTitles = new Object2ObjectOpenHashMap<>();
        boolean matched = false;
        boolean versionOk = false;
        for (String line : readLines(target)) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.equals(INDEX_VERSION)) {
                versionOk = true;
                continue;
            }
            if (trimmed.startsWith(TITLE_LINE)) {
                final String payload = trimmed.substring(TITLE_LINE.length());
                final int tab = payload.indexOf('\t');
                if (tab <= 0) continue;
                final String identifier = payload.substring(0, tab)
                    .trim();
                final String title = payload.substring(tab + 1)
                    .trim();
                if (!identifier.isEmpty() && !title.isEmpty()) loadedTitles.put(identifier, title);
                continue;
            }
            if (trimmed.startsWith("#")) {
                if (trimmed.equals(expected)) matched = true;
                continue;
            }
            final int equals = trimmed.indexOf('=');
            if (equals <= 0) continue;
            final String key = normalize(trimmed.substring(0, equals));
            final String identifier = trimmed.substring(equals + 1)
                .trim();
            if (key.isEmpty() || identifier.isEmpty()) continue;
            addValue(loaded, key, identifier);
        }
        if (!matched || !versionOk) return;
        index.clear();
        index.putAll(loaded);
        indexTitles.clear();
        indexTitles.putAll(loadedTitles);
        indexReady = true;
    }

    private static void ensureIndex() {
        if (indexReady) return;
        index.clear();
        indexTitles.clear();
        final int scanned = scanHandlers();
        if (scanned <= 0) return;
        indexReady = true;
        saveCache();
    }

    private static int scanHandlers() {
        final ObjectLinkedOpenHashSet<IRecipeHandler> handlers = new ObjectLinkedOpenHashSet<>();
        try {
            handlers.addAll(GuiCraftingRecipe.craftinghandlers);
        } catch (Throwable ignored) {}
        try {
            handlers.addAll(GuiCraftingRecipe.serialCraftingHandlers);
        } catch (Throwable ignored) {}
        try {
            handlers.addAll(GuiUsageRecipe.usagehandlers);
        } catch (Throwable ignored) {}
        try {
            handlers.addAll(GuiUsageRecipe.serialUsageHandlers);
        } catch (Throwable ignored) {}

        final ObjectLinkedOpenHashSet<String> identifiers = new ObjectLinkedOpenHashSet<>();
        final Object2ObjectOpenHashMap<String, String> titles = new Object2ObjectOpenHashMap<>();
        for (IRecipeHandler handler : handlers) {
            try {
                final String identifier = RecipeCatalysts.getRecipeID(handler);
                if (identifier == null || identifier.trim()
                    .isEmpty()) continue;
                final String trimmedId = identifier.trim();
                if (!identifiers.add(trimmedId)) continue;
                final String title = handler.getRecipeTabName();
                if (title != null && !title.trim()
                    .isEmpty()) {
                    titles.put(trimmedId, title.trim());
                }
            } catch (Throwable ignored) {}
        }

        for (String identifier : identifiers) {
            indexIdentifier(identifier, titles.get(identifier), titles);
        }
        indexTitles.putAll(titles);
        return identifiers.size();
    }

    private static void indexIdentifier(String id, String title, Map<String, String> titles) {
        addValue(index, normalize(id), id);
        for (String variant : idVariants(id)) {
            addValue(index, variant, id);
        }
        if (title != null) {
            addValue(index, normalize(title), id);
        }
        final String baseId = GTRecipeUtil.baseMapId(id);
        String baseTitle = null;
        if (baseId != null) {
            addValue(index, normalize(baseId), id);
            baseTitle = titles.get(baseId);
            if (baseTitle != null) {
                addValue(index, normalize(baseTitle), id);
                indexDerivedWord(id, baseTitle, title);
            }
        }
        indexSuffixWords(id, baseTitle);
    }

    private static void indexDerivedWord(String id, String baseTitle, String title) {
        if (title == null || !title.startsWith(baseTitle) || title.length() <= baseTitle.length()) return;
        final String word = title.substring(baseTitle.length())
            .trim();
        if (!word.isEmpty()) addValue(index, normalize(word), id);
    }

    private static void indexSuffixWords(String id, String baseTitle) {
        final String suffix = lastSegment(id);
        if (suffix == null) return;
        for (String[] entry : VARIANT_WORDS) {
            if (!entry[0].equalsIgnoreCase(suffix)) continue;
            for (String word : Arrays.asList(entry)
                .subList(1, entry.length)) {
                addValue(index, normalize(word), id);
                if (baseTitle != null) addValue(index, normalize(baseTitle + word), id);
            }
        }
    }

    private static List<String> idVariants(String id) {
        final List<String> result = new ObjectArrayList<>();
        for (String prefix : ID_PREFIXES) {
            if (!id.startsWith(prefix)) continue;
            final String stripped = id.substring(prefix.length());
            addVariant(result, stripped);
            addVariant(result, stripped.replace('_', ' '));
        }
        addVariant(result, id.replace('_', ' '));
        return result;
    }

    private static void addVariant(List<String> result, String value) {
        final String normalized = normalize(value);
        if (!normalized.isEmpty() && !result.contains(normalized)) result.add(normalized);
    }

    private static String lastSegment(String id) {
        final int dot = id.lastIndexOf('.');
        final String tail = dot < 0 ? id : id.substring(dot + 1);
        if (tail.isEmpty()) return null;
        final int underscore = tail.lastIndexOf('_');
        return underscore < 0 ? tail : tail.substring(underscore + 1);
    }

    private static void saveCache() {
        final String separator = System.lineSeparator();
        final StringBuilder builder = new StringBuilder();
        builder.append(INDEX_VERSION)
            .append(separator);
        builder.append(INDEX_HEADER)
            .append(fingerprint())
            .append(separator);
        for (Map.Entry<String, String> entry : new TreeSet<>(indexTitles.entrySet())) {
            builder.append(TITLE_LINE)
                .append(entry.getKey())
                .append('\t')
                .append(entry.getValue())
                .append(separator);
        }
        for (String key : new TreeSet<>(index.keySet())) {
            for (String identifier : index.get(key)) {
                builder.append(key)
                    .append(" = ")
                    .append(identifier)
                    .append(separator);
            }
        }
        writeText(indexFile(), builder.toString());
    }

    private static String fingerprint() {
        final StringBuilder builder = new StringBuilder();
        try {
            builder.append(Minecraft.getMinecraft().gameSettings.language);
        } catch (Throwable ignored) {}
        final TreeSet<String> mods = new TreeSet<>();
        try {
            for (ModContainer mod : Loader.instance()
                .getModList()) {
                mods.add(mod.getModId() + "@" + mod.getVersion());
            }
        } catch (Throwable ignored) {}
        builder.append('|')
            .append(mods);
        return Integer.toHexString(
            builder.toString()
                .hashCode());
    }

    private static boolean hasCatalysts(String identifier) {
        try {
            return !RecipeCatalysts.getRecipeCatalysts(identifier)
                .isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<String> familyByPrefix(String key) {
        String best = null;
        for (String candidate : new ObjectOpenHashSet<>(index.keySet())) {
            if (candidate.length() >= key.length()) continue;
            if (!key.startsWith(candidate)) continue;
            if (best == null || candidate.length() > best.length()) best = candidate;
        }
        if (best == null) return Collections.emptyList();
        final List<String> values = index.get(best);
        return values == null ? Collections.emptyList() : copyDistinct(values);
    }

    private static List<String> contains(String key) {
        final List<String> result = new ObjectArrayList<>();
        for (Map.Entry<String, List<String>> entry : index.entrySet()) {
            if (!entry.getKey()
                .contains(key)) continue;
            for (String identifier : entry.getValue()) {
                if (!result.contains(identifier)) result.add(identifier);
            }
            if (result.size() >= PREFIX_LIMIT) break;
        }
        return result;
    }

    private static List<String> copyDistinct(List<String> values) {
        final List<String> result = new ObjectArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isEmpty() && !result.contains(value)) result.add(value);
        }
        return result;
    }

    private static void addSuggestion(List<String> result, String candidate, String key, int limit) {
        if (result.size() >= limit) return;
        if (candidate.contains(key) && !result.contains(candidate)) result.add(candidate);
    }

    private static void addValue(Map<String, List<String>> target, String key, String value) {
        List<String> values = target.computeIfAbsent(key, _ -> new ObjectArrayList<>());
        if (!values.contains(value)) values.add(value);
    }

    private static boolean isComment(String trimmed) {
        return trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.startsWith("//");
    }

    private static List<String> readLines(File target) {
        try {
            return Files.readAllLines(target.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read {}", target, e);
            return Collections.emptyList();
        }
    }

    private static void writeText(File target, String content) {
        final File dir = target.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            LOG.warn("Failed to create the directory {}", dir);
            return;
        }
        try {
            Files.writeString(target.toPath(), content);
        } catch (IOException e) {
            LOG.warn("Failed to write {}", target, e);
        }
    }

    private static String normalize(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static File aliasFile() {
        if (aliasFile == null) aliasFile = new File(configDir(), ALIAS_FILE);
        return aliasFile;
    }

    private static File indexFile() {
        if (indexFile == null) indexFile = new File(configDir(), INDEX_FILE);
        return indexFile;
    }

    private static File configDir() {
        final File configDir = new File((File) FMLInjectionData.data()[6], "config");
        return new File(configDir, AE2Thing.MODID);
    }
}
