package com.milesight.beaveriot.device.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.device.po.DevicePO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceRepository extends BaseJpaRepository<DevicePO, Long> {
    public List<DevicePO> findByIdIn(List<Long> ids);

    @Query("SELECT r.integration, COUNT(r) FROM DevicePO r WHERE r.integration IN :integrations GROUP BY r.integration")
    List<Object[]> countByIntegrations(@Param("integrations") List<String> integrations);
}
