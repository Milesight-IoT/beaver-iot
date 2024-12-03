package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.user.enums.UserErrorCode;
import com.milesight.beaveriot.user.model.request.UserRegisterRequest;
import com.milesight.beaveriot.user.model.response.UserInfoResponse;
import com.milesight.beaveriot.user.model.response.UserStatusResponse;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.repository.UserRepository;
import com.milesight.beaveriot.user.util.SignUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 8:42
 */
@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public void register(UserRegisterRequest userRegisterRequest) {
        String email = userRegisterRequest.getEmail();
        String nickname = userRegisterRequest.getNickname();
        String password = userRegisterRequest.getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(nickname) || !StringUtils.hasText(password)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("email and nickname and password must be not null").build();
        }
        UserPO userPO = getUserByEmail(email);
        if (userPO != null) {
            throw ServiceException.with(UserErrorCode.USER_REGISTER_EMAIL_EXIST).build();
        }
        userPO = new UserPO();
        userPO.setId(SnowflakeUtil.nextId());
        userPO.setEmail(email);
        userPO.setEmailHash(SignUtils.sha256Hex(email));
        userPO.setNickname(nickname);
        userPO.setPassword(new BCryptPasswordEncoder().encode(password));
        userPO.setPreference(null);
        userRepository.save(userPO);
    }

    public UserStatusResponse status() {
        List<UserPO> users = userRepository.findAll();
        boolean isInit = users != null && !users.isEmpty();
        UserStatusResponse userStatusResponse = new UserStatusResponse();
        userStatusResponse.setInit(isInit);
        return userStatusResponse;
    }

    public UserInfoResponse getUserInfo() {
        SecurityUserContext.SecurityUser securityUser = SecurityUserContext.getSecurityUser();
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        if (securityUser != null) {
            userInfoResponse.setUserId(SecurityUserContext.getUserId());
            userInfoResponse.setNickname(securityUser.getPayload().get("nickname").toString());
            userInfoResponse.setEmail(securityUser.getPayload().get("email").toString());
        }
        return userInfoResponse;
    }

    public UserPO getUserByEmail(String email) {
        return userRepository.findOne(filter -> filter.eq(UserPO.Fields.email, email)).orElse(null);
    }

}
