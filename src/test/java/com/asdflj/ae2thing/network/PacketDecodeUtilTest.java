package com.asdflj.ae2thing.network;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

public class PacketDecodeUtilTest {

    @Test
    public void readsCompressedNbtFromDirectBuffer() throws Exception {
        NBTTagCompound expected = new NBTTagCompound();
        expected.setString("value", "direct-buffer");
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(expected, compressed);

        ByteBuf direct = Unpooled.directBuffer();
        try {
            direct.writeBytes(compressed.toByteArray());
            NBTTagCompound actual = PacketDecodeUtil.readCompressedNbt(direct);
            assertEquals("direct-buffer", actual.getString("value"));
        } finally {
            direct.release();
        }
    }

    @Test(expected = DecoderException.class)
    public void rejectsOversizedCompressedNbtBeforeDecompression() throws Exception {
        ByteBuf oversized = Unpooled.buffer(2 * 1024 * 1024 + 1);
        try {
            oversized.writerIndex(oversized.capacity());
            PacketDecodeUtil.readCompressedNbt(oversized);
        } finally {
            oversized.release();
        }
    }
}
