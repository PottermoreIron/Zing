package com.pot.im.service.protocol.serializer;

import lombok.Data;

@Data
public class ProtocolHeader {
        private int magicNumber = 0x12345678;

        private byte version = 1;

        private byte msgType;

        private byte flags;

        private byte reserved;

        private long sequence;

        private long timestamp;

        private int checkSum;

        private int length;

    public boolean isCompressed() {
        return (flags & 0x01) != 0;
    }

    public void setCompressed(boolean compressed) {
        if (compressed) {
            flags |= 0x01;
        } else {
            flags &= ~0x01;
        }
    }

    public boolean isEncrypted() {
        return (flags & 0x02) != 0;
    }

    public void setEncrypted(boolean encrypted) {
        if (encrypted) {
            flags |= 0x02;
        } else {
            flags &= ~0x02;
        }
    }

    public boolean isFragmented() {
        return (flags & 0x04) != 0;
    }

    public void setFragmented(boolean fragmented) {
        if (fragmented) {
            flags |= 0x04;
        } else {
            flags &= ~0x04;
        }
    }

    public byte getPriority() {
        return (byte) ((flags >> 3) & 0x03);
    }

    public void setPriority(byte priority) {
        flags = (byte) ((flags & 0xE7) | ((priority & 0x03) << 3));
    }

    public byte getProtocolExtension() {
        return (byte) ((reserved >> 4) & 0x0F);
    }

    public void setProtocolExtension(byte extension) {
        reserved = (byte) ((reserved & 0x0F) | ((extension & 0x0F) << 4));
    }

    public SerializerType getSerializerType() {
        byte code = (byte) (reserved & 0x0F);
        return SerializerType.fromCode(code);
    }

    public void setSerializerType(SerializerType type) {
        reserved = (byte) ((reserved & 0xF0) | (type.getCode() & 0x0F));
    }
}
