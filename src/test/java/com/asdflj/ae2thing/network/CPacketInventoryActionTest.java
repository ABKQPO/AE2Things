package com.asdflj.ae2thing.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CPacketInventoryActionTest {

    @Test
    public void usesGenericStackPacketCodecForFluidCraftingTargets() throws IOException {
        InputStream input = getClass().getClassLoader()
            .getResourceAsStream("com/asdflj/ae2thing/network/CPacketInventoryAction.class");
        assertNotNull(input);

        boolean[] genericStackField = { false };
        int[] genericWrites = { 0 };
        int[] genericReads = { 0 };
        try (InputStream bytecode = input) {
            new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                    Object value) {
                    if (name.equals("stack") && descriptor.equals("Lappeng/api/storage/data/IAEStack;")) {
                        genericStackField[0] = true;
                    }
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    return new MethodVisitor(
                        Opcodes.ASM5,
                        super.visitMethod(access, name, descriptor, signature, exceptions)) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                            String methodDescriptor, boolean isInterface) {
                            if (owner.equals("appeng/api/storage/data/IAEStack")) {
                                if (methodName.equals("writeToPacketGeneric")) genericWrites[0]++;
                                if (methodName.equals("fromPacketGeneric")) genericReads[0]++;
                            }
                            super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        }
                    };
                }
            }, 0);
        }

        assertTrue(genericStackField[0]);
        assertEquals(1, genericWrites[0]);
        assertEquals(1, genericReads[0]);
    }
}
