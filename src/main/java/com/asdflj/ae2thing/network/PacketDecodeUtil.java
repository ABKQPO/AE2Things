package com.asdflj.ae2thing.network;

import java.io.IOException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public final class PacketDecodeUtil {

    private static final int MAX_COMPRESSED_NBT_BYTES = 2 * 1024 * 1024;
    private static final int MAX_UNCOMPRESSED_NBT_BYTES = 16 * 1024 * 1024;

    private PacketDecodeUtil() {}

    static <E extends Enum<E>> E readIntEnum(ByteBuf buf, E[] values, String name) {
        return getEnum(values, buf.readInt(), name);
    }

    static <E extends Enum<E>> E readByteEnum(ByteBuf buf, E[] values, String name) {
        return getEnum(values, buf.readUnsignedByte(), name);
    }

    private static <E extends Enum<E>> E getEnum(E[] values, int ordinal, String name) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException("Invalid " + name + " ordinal: " + ordinal);
        }
        return values[ordinal];
    }

    static String readUtf16(ByteBuf buf, int maxLength, String name) {
        int length = buf.readInt();
        return readUtf16(buf, length, maxLength, name);
    }

    static String readUtf16(ByteBuf buf, int length, int maxLength, String name) {
        if (length < 0 || length > maxLength || length > buf.readableBytes() / Character.BYTES) {
            throw new DecoderException("Invalid " + name + " length: " + length);
        }
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(buf.readChar());
        }
        return value.toString();
    }

    public static NBTTagCompound readCompressedNbt(ByteBuf buf) throws IOException {
        int length = buf.readableBytes();
        if (length < 0 || length > MAX_COMPRESSED_NBT_BYTES) {
            throw new DecoderException("Compressed NBT payload is too large: " + length);
        }
        byte[] compressed = new byte[length];
        buf.readBytes(compressed);
        return CompressedStreamTools.func_152457_a(compressed, new NBTSizeTracker(MAX_UNCOMPRESSED_NBT_BYTES));
    }
}
