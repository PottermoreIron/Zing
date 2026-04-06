package com.pot.im.service.protocol.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.zip.CRC32;

@Slf4j
public class ProtocolDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 32;
    private static final int MAGIC_NUMBER = 0x12345678;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }
        in.markReaderIndex();
        ProtocolHeader header = new ProtocolHeader();
        int magicNumber = in.readInt();
        if (magicNumber != MAGIC_NUMBER) {
            log.error("invalid magic number: {}", Integer.toHexString(magicNumber));
            ctx.close();
            return;
        }
        header.setMagicNumber(magicNumber);
        header.setVersion(in.readByte());
        header.setMsgType(in.readByte());
        header.setFlags(in.readByte());
        header.setReserved(in.readByte());
        header.setSequence(in.readLong());
        header.setTimestamp(in.readLong());
        header.setCheckSum(in.readInt());
        header.setLength(in.readInt());
        if (header.getLength() < 0 || in.readableBytes() < header.getLength()) {
            log.error("invalid data length: {}", header.getLength());
            ctx.close();
            return;
        }
        if (in.readableBytes() < header.getLength()) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[header.getLength()];
        in.readBytes(data);

        if (!verifyChecksum(header, data)) {
            log.error("Checksum verification failed");
            ctx.close();
            return;
        }

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
