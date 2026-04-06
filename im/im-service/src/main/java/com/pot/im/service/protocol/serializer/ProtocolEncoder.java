package com.pot.im.service.protocol.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.CRC32;

public class ProtocolEncoder extends MessageToByteEncoder<ProtocolMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) throws Exception {
        CRC32 crc32 = new CRC32();
        crc32.update(msg.getData());
        msg.getHeader().setCheckSum((int) crc32.getValue());

        msg.getHeader().setLength(msg.getData().length);

        out.writeInt(msg.getHeader().getMagicNumber());
        out.writeByte(msg.getHeader().getVersion());
        out.writeByte(msg.getHeader().getMsgType());
        out.writeByte(msg.getHeader().getFlags());
        out.writeByte(msg.getHeader().getReserved());
        out.writeLong(msg.getHeader().getSequence());
        out.writeLong(msg.getHeader().getTimestamp());
        out.writeInt(msg.getHeader().getCheckSum());
        out.writeInt(msg.getHeader().getLength());

        out.writeBytes(msg.getData());
    }
}
