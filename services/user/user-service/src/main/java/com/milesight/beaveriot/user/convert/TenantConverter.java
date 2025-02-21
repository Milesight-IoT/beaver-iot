package com.milesight.beaveriot.user.convert;

import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.po.TenantPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author loong
 * @date 2024/12/5 10:53
 */
@Mapper
public interface TenantConverter {

    TenantConverter INSTANCE = Mappers.getMapper(TenantConverter.class);

    @Mapping(source = "id", target = "tenantId")
    @Mapping(source = "name", target = "tenantName")
    @Mapping(source = "domain", target = "tenantDomain")
    TenantDTO convertDTO(TenantPO tenantPO);

    List<TenantDTO> convertDTOList(List<TenantPO> tenantPOList);
}
