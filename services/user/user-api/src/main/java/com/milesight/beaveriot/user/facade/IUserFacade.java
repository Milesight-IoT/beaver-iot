package com.milesight.beaveriot.user.facade;

import com.milesight.beaveriot.user.dto.UserDTO;

/**
 * @author loong
 * @date 2024/10/14 11:47
 */
public interface IUserFacade {

    UserDTO getUserByEmail(String email);

}
