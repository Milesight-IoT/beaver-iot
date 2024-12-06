package com.milesight.beaveriot.authentication.facade;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/23 13:10
 */
public interface IAuthenticationFacade {

    Map<String, Object> getUserByToken(String token);

}
