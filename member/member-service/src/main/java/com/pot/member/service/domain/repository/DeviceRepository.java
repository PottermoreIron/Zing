package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.device.DeviceAggregate;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository {

    DeviceAggregate save(DeviceAggregate device);

    Optional<DeviceAggregate> findByMemberIdAndDeviceToken(Long memberId, String deviceToken);

    List<DeviceAggregate> findByMemberId(Long memberId);

    void deleteById(Long deviceId);
}
