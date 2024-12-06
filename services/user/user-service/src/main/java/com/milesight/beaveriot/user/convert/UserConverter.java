package com.milesight.beaveriot.user.convert;

import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/17 16:59
 */
@Mapper
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    @Mappings({
            @Mapping(source = "id", target = "userId"),
            @Mapping(source = "password", target = "encodePassword"),
    })
    UserDTO convertDTO(UserPO userPO);

    List<UserDTO> convertDTOList(List<UserPO> userPOList);

}
