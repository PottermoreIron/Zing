package com.pot.member.service.converter;

import com.pot.member.facade.dto.SocialConnectionDTO;
import com.pot.member.service.entity.SocialConnection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社交账号连接转换器
 * <p>
 * 负责SocialConnection实体和SocialConnectionDTO之间的转换
 * </p>
 *
 * @author Zing
 * @since 2025-11-04
 */
@Component
public class SocialConnectionConverter {

    /**
     * 实体转DTO
     *
     * @param entity 实体对象
     * @return DTO对象
     */
    public SocialConnectionDTO toDTO(SocialConnection entity) {
        if (entity == null) {
            return null;
        }

        return SocialConnectionDTO.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .provider(entity.getProvider())
                .providerMemberId(entity.getProviderMemberId())
                .providerUsername(entity.getProviderUsername())
                .providerEmail(entity.getProviderEmail())
                .avatarUrl(extractAvatarUrl(entity.getExtendJson()))
                .isActive(entity.isActive())
                .boundAt(entity.getGmtCreatedAt())
                .updatedAt(entity.getGmtUpdatedAt())
                .lastUsedAt(entity.getGmtLastSyncAt() != null ?
                        convertLocalDateTimeToTimestamp(entity.getGmtLastSyncAt()) : null)
                .isPrimary(false) // TODO: 如果需要主账号功能，需要在实体中添加字段
                .status(entity.isActive() ? "ACTIVE" : "INACTIVE")
                .build();
    }

    /**
     * 实体列表转DTO列表
     *
     * @param entities 实体列表
     * @return DTO列表
     */
    public List<SocialConnectionDTO> toDTOList(List<SocialConnection> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 从扩展JSON中提取头像URL
     * <p>
     * 扩展JSON格式示例：{"avatar":"https://...","nickname":"..."}
     * </p>
     *
     * @param extendJson 扩展JSON字符串
     * @return 头像URL，如果不存在返回null
     */
    private String extractAvatarUrl(String extendJson) {
        if (extendJson == null || extendJson.isEmpty()) {
            return null;
        }

        try {
            // 简单的JSON解析，提取avatar字段
            // 生产环境建议使用Jackson或Gson
            int avatarIndex = extendJson.indexOf("\"avatar\"");
            if (avatarIndex == -1) {
                avatarIndex = extendJson.indexOf("\"avatarUrl\"");
            }

            if (avatarIndex != -1) {
                int urlStartIndex = extendJson.indexOf("\"", avatarIndex + 10);
                if (urlStartIndex != -1) {
                    int urlEndIndex = extendJson.indexOf("\"", urlStartIndex + 1);
                    if (urlEndIndex != -1) {
                        return extendJson.substring(urlStartIndex + 1, urlEndIndex);
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败，返回null
        }

        return null;
    }

    /**
     * DTO转实体
     * <p>
     * 注意：一般不需要DTO转实体，因为创建时使用Request对象
     * 这里提供此方法以保持完整性
     * </p>
     *
     * @param dto DTO对象
     * @return 实体对象
     */
    public SocialConnection toEntity(SocialConnectionDTO dto) {
        if (dto == null) {
            return null;
        }

        return SocialConnection.builder()
                .id(dto.getId())
                .memberId(dto.getMemberId())
                .provider(dto.getProvider())
                .providerMemberId(dto.getProviderMemberId())
                .providerUsername(dto.getProviderUsername())
                .providerEmail(dto.getProviderEmail())
                .isActive(dto.getIsActive() ? SocialConnection.Status.ACTIVE.getCode() :
                        SocialConnection.Status.INACTIVE.getCode())
                .gmtCreatedAt(dto.getBoundAt())
                .gmtUpdatedAt(dto.getUpdatedAt())
                .build();
    }

    /**
     * 将LocalDateTime转换Unix时间戳
     *
     * @param dateTime LocalDateTime对象
     * @return Unix时间戳（秒）
     */
    private Long convertLocalDateTimeToTimestamp(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
    }
}

