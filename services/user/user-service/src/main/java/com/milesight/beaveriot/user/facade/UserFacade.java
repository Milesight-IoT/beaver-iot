package com.milesight.beaveriot.user.facade;

import com.milesight.beaveriot.user.convert.UserConverter;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author loong
 * @date 2024/10/14 11:47
 */
@Service
public class UserFacade implements IUserFacade {

    @Autowired
    UserService userService;

    @Override
    public UserDTO getUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        UserPO userPO = userService.getUserByEmail(email);
        return UserConverter.INSTANCE.convertDTO(userPO);
    }
}
