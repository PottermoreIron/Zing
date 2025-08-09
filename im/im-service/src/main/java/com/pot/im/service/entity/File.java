package com.pot.im.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文件信息表
 * </p>
 *
 * @author Pot
 * @since 2025-08-10 01:03:52
 */
@Getter
@Setter
@ToString
@TableName("im_file")
public class File implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 文件唯一标识
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 存储文件名
     */
    @TableField("filename")
    private String filename;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 文件大小(字节)
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件类型分类
     */
    @TableField("file_type")
    private String fileType;

    /**
     * MIME类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件存储路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 缩略图路径
     */
    @TableField("thumbnail_path")
    private String thumbnailPath;

    /**
     * 预览图路径
     */
    @TableField("preview_path")
    private String previewPath;

    /**
     * 图片/视频宽度
     */
    @TableField("width")
    private Integer width;

    /**
     * 图片/视频高度
     */
    @TableField("height")
    private Integer height;

    /**
     * 音视频时长(秒)
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 文件哈希值（用于去重）
     */
    @TableField("file_hash")
    private String fileHash;

    /**
     * 上传者ID
     */
    @TableField("uploader_id")
    private Long uploaderId;

    /**
     * 上传IP
     */
    @TableField("upload_ip")
    private String uploadIp;

    /**
     * 下载次数
     */
    @TableField("download_count")
    private Integer downloadCount;

    /**
     * 状态: 0-已删除, 1-正常, 2-审核中, 3-审核失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 过期时间
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 业务扩展json
     */
    @TableField("extend_json")
    private String extendJson;
}
