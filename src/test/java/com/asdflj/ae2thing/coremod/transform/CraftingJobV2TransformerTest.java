package com.asdflj.ae2thing.coremod.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CraftingJobV2TransformerTest {

    private static final String CRAFTING_JOB = "appeng/crafting/v2/CraftingJobV2.class";
    private static final String ITEM_REPO = "appeng/client/me/ItemRepo.class";
    private static final String CRAFTING_JOB_CONSTRUCTOR = "(Lnet/minecraft/world/World;Lappeng/api/networking/IGrid;"
        + "Lappeng/api/networking/security/BaseActionSource;Lappeng/api/storage/data/IAEStack;"
        + "Lappeng/api/config/CraftingMode;Lappeng/api/networking/crafting/ICraftingCallback;)V";
    private static final String CRAFTING_HELPER_DESCRIPTOR = "(Lappeng/crafting/v2/CraftingJobV2;Lnet/minecraft/world/World;Lappeng/api/networking/IGrid;"
        + "Lappeng/api/networking/security/BaseActionSource;Lappeng/api/storage/data/IAEStack;"
        + "Lappeng/api/config/CraftingMode;Lappeng/api/networking/crafting/ICraftingCallback;)V";

    @Test
    public void injectsDebugCallbackIntoGenericStackConstructor() throws IOException {
        byte[] transformed = CraftingJobV2Transformer.INSTANCE.transformClass(readClass(CRAFTING_JOB));

        assertEquals(
            1,
            countCalls(
                transformed,
                "<init>",
                CRAFTING_JOB_CONSTRUCTOR,
                "com/asdflj/ae2thing/api/CraftingDebugHelper",
                "craftingHelper",
                CRAFTING_HELPER_DESCRIPTOR));
    }

    @Test
    public void rewritesGenericStackSearchHooks() throws IOException {
        byte[] transformed = PlatformTransformer.INSTANCE.transformClass(readClass(ITEM_REPO));

        assertEquals(
            1,
            countCalls(
                transformed,
                "com/asdflj/ae2thing/coremod/hooker/CoreModHooksClient",
                "getModId",
                "(Lappeng/api/storage/data/IAEStack;)Ljava/lang/String;"));
        assertEquals(
            1,
            countCalls(
                transformed,
                "com/asdflj/ae2thing/coremod/hooker/CoreModHooksClient",
                "getItemDisplayName",
                "(Ljava/lang/Object;)Ljava/lang/String;"));
        assertEquals(
            2,
            countCalls(
                transformed,
                "com/asdflj/ae2thing/coremod/hooker/CoreModHooksClient",
                "getTooltip",
                "(Ljava/lang/Object;)Ljava/util/List;"));
    }

    private static byte[] readClass(String resource) throws IOException {
        InputStream input = CraftingJobV2TransformerTest.class.getClassLoader()
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

    private static int countCalls(byte[] bytecode, String methodName, String methodDescriptor, String owner,
        String calledMethod, String calledDescriptor) {
        return countCalls(bytecode, methodName, methodDescriptor, owner, calledMethod, calledDescriptor, true);
    }

    private static int countCalls(byte[] bytecode, String owner, String calledMethod, String calledDescriptor) {
        return countCalls(bytecode, null, null, owner, calledMethod, calledDescriptor, false);
    }

    private static int countCalls(byte[] bytecode, String methodName, String methodDescriptor, String owner,
        String calledMethod, String calledDescriptor, boolean filterMethod) {
        int[] calls = { 0 };
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (filterMethod && (!methodName.equals(name) || !methodDescriptor.equals(descriptor))) return parent;
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
}
