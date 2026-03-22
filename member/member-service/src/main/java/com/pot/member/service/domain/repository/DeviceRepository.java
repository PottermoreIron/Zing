package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.device.DeviceAggregate;

import java.util.List;
import java.util.Optional;

/**
 * 设备 Repository Port
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface DeviceRepository {

    DeviceAggregate save(DeviceAggregate device);

    Optional<DeviceAggregate> findByMemberIdAndDeviceToken(Long memberId, String deviceToken);

    List<DeviceAggregate> findByMemberId(Long memberId);

    void deleteById(Long deviceId);
}
