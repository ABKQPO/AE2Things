package com.asdflj.ae2thing.port;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Guards the bytecode-level contracts needed by the AE2/Thaumic Energistics stack-type port. */
public class StoragePortCompatibilityTest {

    private static final String TILE = "com/asdflj/ae2thing/common/tile/TileEssentiaDiscretizer.class";
    private static final String PHIAL_INVENTORY = "com/asdflj/ae2thing/common/tile/TileEssentiaDiscretizer$PhialDiscretizingInventory.class";
    private static final String ESSENTIA_CRAFTING_INVENTORY = "com/asdflj/ae2thing/common/tile/TileEssentiaDiscretizer$EssentiaCraftingInventory.class";
    private static final String INFINITY_FLUID = "com/asdflj/ae2thing/common/storage/infinityCell/InfinityFluidStorageCellInventory.class";
    private static final String CELL_HANDLER = "com/asdflj/ae2thing/common/storage/CellHandler.class";
    private static final String ESSENTIA_FIND = "com/asdflj/ae2thing/api/adapter/findit/EssentiaStorageBusAdapter.class";

    @Test
    public void discretizerRegistersAndBridgesTheEssentiaStackType() throws IOException {
        assertTrue(
            hasMethod(readClass(TILE), "getCellArray", "(Lappeng/api/storage/data/IAEStackType;)Ljava/util/List;"));

        assertEquals(
            1,
            countCalls(
                readClass(ESSENTIA_CRAFTING_INVENTORY),
                "injectItems",
                "(Lthaumicenergistics/common/storage/AEEssentiaStack;Lappeng/api/config/Actionable;"
                    + "Lappeng/api/networking/security/BaseActionSource;)"
                    + "Lthaumicenergistics/common/storage/AEEssentiaStack;",
                "appeng/me/cache/CraftingGridCache",
                "injectItems",
                "(Lappeng/api/storage/data/IAEStack;Lappeng/api/config/Actionable;"
                    + "Lappeng/api/networking/security/BaseActionSource;)Lappeng/api/storage/data/IAEStack;"));
    }

    @Test
    public void discretizerPreservesSignedEssentiaDeltas() throws IOException {
        byte[] phialInventory = readClass(PHIAL_INVENTORY);
        String method = "postChange";
        String descriptor = "(Lappeng/api/networking/storage/IBaseMonitor;Ljava/lang/Iterable;"
            + "Lappeng/api/networking/security/BaseActionSource;)V";

        assertEquals(
            1,
            countCalls(
                phialInventory,
                method,
                descriptor,
                "appeng/api/storage/IMEMonitor",
                "getAvailableItem",
                "(Lappeng/api/storage/data/IAEStack;I)Lappeng/api/storage/data/IAEStack;"));
        assertEquals(
            1,
            countCalls(
                phialInventory,
                method,
                descriptor,
                "com/asdflj/ae2thing/common/item/ItemPhial",
                "newAeDeltaStack",
                "(Lthaumicenergistics/common/storage/AEEssentiaStack;"
                    + "Lthaumicenergistics/common/storage/AEEssentiaStack;)"
                    + "Lappeng/api/storage/data/IAEItemStack;"));
    }

    @Test
    public void infinityFluidLookupReturnsACopy() throws IOException {
        assertEquals(
            1,
            countCalls(
                readClass(INFINITY_FLUID),
                "getAvailableItem",
                "(Lappeng/api/storage/data/IAEFluidStack;I)Lappeng/api/storage/data/IAEFluidStack;",
                "appeng/api/storage/data/IAEFluidStack",
                "copy",
                "()Lappeng/api/storage/data/IAEFluidStack;"));
    }

    @Test
    public void playerBackpackTerminalIsRejectedAsAWorldCell() throws IOException {
        byte[] cellHandler = readClass(CELL_HANDLER);
        assertTrue(
            countTypeInstructions(
                cellHandler,
                "isCell",
                "(Lnet/minecraft/item/ItemStack;)Z",
                Opcodes.INSTANCEOF,
                "com/asdflj/ae2thing/common/item/ItemBackpackTerminal") > 0);
        assertTrue(
            countTypeInstructions(
                cellHandler,
                "getCellInventory",
                "(Lnet/minecraft/item/ItemStack;Lappeng/api/storage/ISaveProvider;"
                    + "Lappeng/api/storage/StorageChannel;)Lappeng/api/storage/IMEInventoryHandler;",
                Opcodes.INSTANCEOF,
                "com/asdflj/ae2thing/common/item/ItemBackpackTerminal") > 0);
    }

    @Test
    public void essentiaFindItQueriesTheEssentiaHandler() throws IOException {
        byte[] adapter = readClass(ESSENTIA_FIND);
        String method = "getStorageProver";
        String descriptor = "(Lappeng/api/networking/IGrid;Lappeng/api/networking/IGridNode;"
            + "Lappeng/api/storage/data/IAEItemStack;Z)Ljava/util/List;";

        assertEquals(
            1,
            countCalls(
                adapter,
                method,
                descriptor,
                "thaumicenergistics/common/storage/AEEssentiaStackType",
                "getStackFromContainerItem",
                "(Lnet/minecraft/item/ItemStack;)Lthaumicenergistics/common/storage/AEEssentiaStack;"));
        assertEquals(
            1,
            countCalls(
                adapter,
                method,
                descriptor,
                "appeng/api/storage/IMEInventoryHandler",
                "getAvailableItem",
                "(Lappeng/api/storage/data/IAEStack;I)Lappeng/api/storage/data/IAEStack;"));
    }

    private static byte[] readClass(String resource) throws IOException {
        InputStream input = StoragePortCompatibilityTest.class.getClassLoader()
            .getResourceAsStream(resource);
        assertNotNull("Missing test classpath resource: " + resource, input);
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static boolean hasMethod(byte[] bytecode, String methodName, String methodDescriptor) {
        boolean[] found = { false };
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                if (methodName.equals(name) && methodDescriptor.equals(descriptor)) found[0] = true;
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return found[0];
    }

    private static int countCalls(byte[] bytecode, String methodName, String methodDescriptor, String owner,
        String calledMethod, String calledDescriptor) {
        int[] calls = { 0 };
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) return parent;
                return new MethodVisitor(Opcodes.ASM5, parent) {

                    @Override
                    public void visitMethodInsn(int opcode, String invocationOwner, String invocationName,
                        String invocationDescriptor, boolean isInterface) {
                        if (owner.equals(invocationOwner) && calledMethod.equals(invocationName)
                            && calledDescriptor.equals(invocationDescriptor)) {
                            calls[0]++;
                        }
                        super.visitMethodInsn(
                            opcode,
                            invocationOwner,
                            invocationName,
                            invocationDescriptor,
                            isInterface);
                    }
                };
            }
        }, 0);
        return calls[0];
    }

    private static int countTypeInstructions(byte[] bytecode, String methodName, String methodDescriptor, int opcode,
        String type) {
        int[] instructions = { 0 };
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) return parent;
                return new MethodVisitor(Opcodes.ASM5, parent) {

                    @Override
                    public void visitTypeInsn(int instructionOpcode, String instructionType) {
                        if (opcode == instructionOpcode && type.equals(instructionType)) instructions[0]++;
                        super.visitTypeInsn(instructionOpcode, instructionType);
                    }
                };
            }
        }, 0);
        return instructions[0];
    }
}
