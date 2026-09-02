package com.asdflj.ae2thing.inventory.gui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class GuiTypeOptionalDependencyTest {

    private static final String GUI_TYPE = "com.asdflj.ae2thing.inventory.gui.GuiType";
    private static final String GUI_PACKAGE = "com.asdflj.ae2thing.inventory.gui.";
    private static final String WCT_PACKAGE = "net.p455w0rd.wirelesscraftingterminal.";

    @Test
    public void initializesAllGuiTypesWithoutWirelessCraftingTerminal() throws Exception {
        ClassLoader loader = new WithoutWCTClassLoader(getClass().getClassLoader());

        Class<?> guiType = Class.forName(GUI_TYPE, true, loader);
        Object[] values = (Object[]) guiType.getMethod("values")
            .invoke(null);

        assertTrue(values.length > 0);
        assertNotNull(
            guiType.getField("WCT_CRAFTING_TERMINAL_BRIDGE")
                .get(null));
    }

    private static final class WithoutWCTClassLoader extends ClassLoader {

        private WithoutWCTClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    if (name.startsWith(WCT_PACKAGE)) {
                        throw new ClassNotFoundException("WCT hidden for regression test: " + name);
                    }
                    loaded = name.startsWith(GUI_PACKAGE) ? findProjectClass(name) : super.loadClass(name, false);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> findProjectClass(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream input = getParent().getResourceAsStream(resource)) {
                if (input == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = input.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
