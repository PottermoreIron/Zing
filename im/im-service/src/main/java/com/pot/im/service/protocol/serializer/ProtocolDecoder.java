package com.pot.im.service.protocol.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.zip.CRC32;

/**
 * @author: Pot
 * @created: 2025/8/10 22:33
 * @description: 协议解码器
 */
@Slf4j
public class ProtocolDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 32;
    private static final int MAGIC_NUMBER = 0x12345678;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }
        // 标记读取位置
        in.markReaderIndex();
        ProtocolHeader header = new ProtocolHeader();
        // 读取魔数
        int magicNumber = in.readInt();
        if (magicNumber != MAGIC_NUMBER) {
            // 如果魔数不匹配，丢弃数据
            log.error("invalid magic number: {}", Integer.toHexString(magicNumber));
            ctx.close();
            return;
        }
        header.setMagicNumber(magicNumber);
        // 读取版本号
        header.setVersion(in.readByte());
        // 读取消息类型
        header.setMsgType(in.readByte());
        // 读取标志位
        header.setFlags(in.readByte());
        // 读取保留字段
        header.setReserved(in.readByte());
        // 读取序列号
        header.setSequence(in.readLong());
        // 读取时间戳
        header.setTimestamp(in.readLong());
        // 读取校验和
        header.setCheckSum(in.readInt());
        // 读取数据长度
        header.setLength(in.readInt());
        if (header.getLength() < 0 || in.readableBytes() < header.getLength()) {
            // 如果数据长度不合法，重置读取位置并丢弃数据
            log.error("invalid data length: {}", header.getLength());
            ctx.close();
            return;
        }
        if (in.readableBytes() < header.getLength()) {
            // 如果数据长度不够，重置读取位置并等待更多数据
            in.resetReaderIndex();
            return;
        }
        // 读取数据
        byte[] data = new byte[header.getLength()];
        in.readBytes(data);

        // 验证校验和
        if (!verifyChecksum(header, data)) {
            log.error("Checksum verification failed");
            ctx.close();
            return;
        }

        // 构造协议消息
        ProtocolMessage message = new ProtocolMessage();
        message.setHeader(header);
        message.setData(data);

        out.add(message);
    }

    private boolean verifyChecksum(ProtocolHeader header, byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue() == (header.getCheckSum() & 0xFFFFFFFFL);
    }
}
