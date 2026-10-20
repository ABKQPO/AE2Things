package com.asdflj.ae2thing.util.enhancedsearch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

@SideOnly(Side.CLIENT)
public final class RecipeSearchAlias {

    private static final String ALIAS_FILE = "search_alias.txt";
    private static final String INDEX_FILE = "search_index.txt";
    private static final String INDEX_HEADER = "# key=";
    private static final int PREFIX_LIMIT = 32;

    private static final Logger LOG = LogManager.getLogger(AE2Thing.MODID);

    private static final String[] TEMPLATE = { "# AE2Things - dual interface terminal, enhanced search aliases",
        "# AE2Things - 双接口终端 强化搜索别名", "#", "# 格式 / format:   <alias> = <identifier>", "#",
        "#   alias      = 你在搜索框 @ 后面输入的短名", "#   identifier = 被它代表的 handler id / overlay id / GT 配方分类", "#",
        "# 例 / example:   1 = smelting", "#   之后输入 @1 等价于 @smelting, 只显示把 smelting 登记为催化剂的接口(原版熔炉, GT 电炉/蒸汽熔炉/工业熔炉等)",
        "#", "# identifier 怎么找 / where to find it:",
        "#   NEI 页面:   IRecipeHandler#getOverlayIdentifier(), 例如 smelting / brewing",
        "#   GT 配方页:  recipeCategory.unlocalizedName, 例如 gt.recipe.assembler", "#",
        "# 搜索框的页标题与分类名由内置索引自动提供, 不需要写在这里 / page titles and category names are indexed",
        "# automatically, only write your own short names here.",
        "# 只有在\"强化搜索\"开启且查询以 @ 开头时才会用到别名 / only used by the @ query of the enhanced search.",
        "# 文件按修改时间热重载 / hot reloaded by the file modification time.", "" };

    private static final Object2ObjectOpenHashMap<String, String> userAliases = new Object2ObjectOpenHashMap<>();
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
        final String user = userAliases.get(key);
        if (user != null) return Collections.singletonList(user);
        ensureIndex();
        final List<String> exact = index.get(key);
        if (exact != null) return exact;
        if (hasCatalysts(head.trim())) return Collections.singletonList(head.trim());
        return prefix(key);
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
            final String identifier = trimmed.substring(equals + 1)
                .trim();
            if (alias.isEmpty() || identifier.isEmpty()) {
                LOG.warn("{}:{} is missing the alias or the identifier, skipped", ALIAS_FILE, lineNumber);
                continue;
            }
            userAliases.put(alias, identifier);
        }
    }

    public static void loadCache() {
        final File target = indexFile();
        if (!target.isFile()) return;
        final String expected = INDEX_HEADER + fingerprint();
        final Object2ObjectOpenHashMap<String, List<String>> loaded = new Object2ObjectOpenHashMap<>();
        final Object2ObjectOpenHashMap<String, String> loadedTitles = new Object2ObjectOpenHashMap<>();
        boolean matched = false;
        for (String line : readLines(target)) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
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
            if (!key.equals(normalize(identifier)) && !loadedTitles.containsKey(identifier)) {
                loadedTitles.put(identifier, key);
            }
        }
        if (!matched) return;
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
        int scanned = 0;
        for (IRecipeHandler handler : handlers) {
            try {
                final String identifier = RecipeCatalysts.getRecipeID(handler);
                if (identifier == null || identifier.trim()
                    .isEmpty()) continue;
                final String trimmedId = identifier.trim();
                addValue(index, normalize(trimmedId), trimmedId);
                final String title = handler.getRecipeTabName();
                if (title != null && !title.trim()
                    .isEmpty()) {
                    final String trimmedTitle = title.trim();
                    addValue(index, normalize(trimmedTitle), trimmedId);
                    if (!indexTitles.containsKey(trimmedId)) indexTitles.put(trimmedId, trimmedTitle);
                }
                scanned++;
            } catch (Throwable ignored) {}
        }
        return scanned;
    }

    private static void saveCache() {
        final String separator = System.lineSeparator();
        final StringBuilder builder = new StringBuilder();
        builder.append("# ae2thing search index v1")
            .append(separator);
        builder.append(INDEX_HEADER)
            .append(fingerprint())
            .append(separator);
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

    private static List<String> prefix(String key) {
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
