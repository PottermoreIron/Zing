package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.device.DeviceAggregate;
import com.pot.member.service.domain.repository.DeviceRepository;
import com.pot.member.service.infrastructure.persistence.entity.Device;
import com.pot.member.service.infrastructure.persistence.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceRepositoryImpl implements DeviceRepository {

    private final DeviceMapper deviceMapper;

    @Override
    public DeviceAggregate save(DeviceAggregate domain) {
        Device entity = toEntity(domain);
        if (domain.getId() == null) {
            deviceMapper.insert(entity);
            log.debug("[Device] Inserting device record — memberId={}, deviceToken={}", domain.getMemberId(), domain.getDeviceToken());
        } else {
            deviceMapper.updateById(entity);
            log.debug("[Device] Updating device record — id={}", domain.getId());
        }
        return toDomain(entity);
    }

    @Override
    public Optional<DeviceAggregate> findByMemberIdAndDeviceToken(Long memberId, String deviceToken) {
        LambdaQueryWrapper<Device> q = new LambdaQueryWrapper<>();
        q.eq(Device::getMemberId, memberId)
                .eq(Device::getDeviceId, deviceToken)
                .isNull(Device::getGmtDeletedAt)
                .last("LIMIT 1");
        Device entity = deviceMapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<DeviceAggregate> findByMemberId(Long memberId) {
        LambdaQueryWrapper<Device> q = new LambdaQueryWrapper<>();
        q.eq(Device::getMemberId, memberId)
                .isNull(Device::getGmtDeletedAt)
                .eq(Device::getIsActive, 1)
                .orderByDesc(Device::getGmtLastUsedAt);
        return deviceMapper.selectList(q).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device != null) {
            device.setGmtDeletedAt(java.time.LocalDateTime.now());
            device.setIsActive(0);
            deviceMapper.updateById(device);
            log.debug("[Device] Soft-deleting device — id={}", deviceId);
        }
    }

    private DeviceAggregate toDomain(Device e) {
        return DeviceAggregate.reconstitute(
                e.getId(),
                e.getMemberId(),
                e.getDeviceId(), // deviceToken → deviceId column
                e.getDeviceType(),
                null, // deviceName - not stored in current entity
                e.getPlatform(), // osType → platform column
                null, // osVersion - not stored in current entity
                e.getAppVersion(),
                null, // lastLoginIp - not stored in current entity
                e.getGmtLastUsedAt(), // lastLoginAt → gmtLastUsedAt column
                e.getPushToken(), // refreshToken → pushToken column (best available mapping)
                e.getGmtCreatedAt(),
                e.getGmtUpdatedAt());
    }

    private Device toEntity(DeviceAggregate d) {
        Device e = new Device();
        if (d.getId() != null) {
            e.setId(d.getId());
        }
        e.setMemberId(d.getMemberId());
        e.setDeviceId(d.getDeviceToken());
        if (d.getDeviceType() != null) {
            e.setDeviceType(Device.DeviceType.fromCode(d.getDeviceType()));
        }
        if (d.getOsType() != null) {
            e.setPlatform(Device.Platform.fromCode(d.getOsType()));
        }
        e.setAppVersion(d.getAppVersion());
        e.setPushToken(d.getRefreshToken());
        e.setIsActive(1);
        e.setGmtLastUsedAt(d.getLastLoginAt() != null ? d.getLastLoginAt() : java.time.LocalDateTime.now());
        e.setGmtCreatedAt(d.getCreatedAt() != null ? d.getCreatedAt() : java.time.LocalDateTime.now());
        e.setGmtUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : java.time.LocalDateTime.now());
        return e;
    }
}
