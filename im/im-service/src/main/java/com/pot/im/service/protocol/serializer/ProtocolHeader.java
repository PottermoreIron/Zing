package com.pot.im.service.protocol.serializer;

import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/8/10 17:40
 * @description: 自定义协议头, 总长度: 32字节固定头部
 */
@Data
public class ProtocolHeader {
    /**
     * 魔数 - 4字节
     * 用于识别协议类型和版本兼容性
     */
    private int magicNumber = 0x12345678;

    /**
     * 版本号 - 1字节
     * 协议版本，用于向后兼容
     */
    private byte version = 1;

    /**
     * 消息类型 - 1字节
     */
    private byte msgType;

    /**
     * 标志位 - 1字节
     * bit 0: 是否压缩
     * bit 1: 是否加密
     * bit 2: 是否分片
     * bit 3-4: 优先级(00:低, 01:普通, 10:高, 11:紧急)
     * bit 5-7: 预留
     */
    private byte flags;

    /**
     * 保留字段 - 1字节
     * 高4位: 协议扩展标识
     * 低4位: 编码格式标识
     */
    private byte reserved;

    /**
     * 序列号 - 8字节
     * 用于消息去重、排序和确认
     */
    private long sequence;

    /**
     * 时间戳 - 8字节
     * 消息发送时间，用于超时处理
     */
    private long timestamp;

    /**
     * 校验和 - 4字节
     * 整个数据包的CRC32校验
     */
    private int checkSum;

    /**
     * 消息体长度 - 4字节
     * body部分的字节长度
     */
    private int length;

    // 标志位操作方法
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
