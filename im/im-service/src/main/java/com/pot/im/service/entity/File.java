package com.pot.im.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@TableName("im_file")
public class File implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

        @TableField(value = "gmt_create", fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

        @TableField(value = "gmt_modified", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

        @TableField("file_id")
    private Long fileId;

        @TableField("filename")
    private String filename;

        @TableField("original_name")
    private String originalName;

        @TableField("file_size")
    private Long fileSize;

        @TableField("file_type")
    private String fileType;

        @TableField("mime_type")
    private String mimeType;

        @TableField("file_path")
    private String filePath;

        @TableField("thumbnail_path")
    private String thumbnailPath;

        @TableField("preview_path")
    private String previewPath;

        @TableField("width")
    private Integer width;

        @TableField("height")
    private Integer height;

        @TableField("duration")
    private Integer duration;

        @TableField("file_hash")
    private String fileHash;

        @TableField("uploader_id")
    private Long uploaderId;

        @TableField("upload_ip")
    private String uploadIp;

        @TableField("download_count")
    private Integer downloadCount;

        @TableField("status")
    private Integer status;

        @TableField("expire_at")
    private LocalDateTime expireAt;

        @TableField("extend_json")
    private String extendJson;
}
