package com.pot.im.service.protocol.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.zip.CRC32;

/**
 * @author: Pot
 * @created: 2025/8/10 22:47
 * @description: 自定义协议解码器
 */
public class ProtocolEncoder extends MessageToByteEncoder<ProtocolMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) throws Exception {
        // 计算校验和
        CRC32 crc32 = new CRC32();
        crc32.update(msg.getData());
        msg.getHeader().setCheckSum((int) crc32.getValue());

        // 设置消息体长度
        msg.getHeader().setLength(msg.getData().length);

        // 写入协议头
        out.writeInt(msg.getHeader().getMagicNumber());
        out.writeByte(msg.getHeader().getVersion());
        out.writeByte(msg.getHeader().getMsgType());
        out.writeByte(msg.getHeader().getFlags());
        out.writeByte(msg.getHeader().getReserved());
        out.writeLong(msg.getHeader().getSequence());
        out.writeLong(msg.getHeader().getTimestamp());
        out.writeInt(msg.getHeader().getCheckSum());
        out.writeInt(msg.getHeader().getLength());

        // 写入消息体
        out.writeBytes(msg.getData());
    }
}
