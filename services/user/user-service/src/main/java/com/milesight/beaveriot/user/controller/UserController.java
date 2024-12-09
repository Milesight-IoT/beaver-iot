package com.milesight.beaveriot.user.controller;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.user.model.request.ChangePasswordRequest;
import com.milesight.beaveriot.user.model.request.CreateUserRequest;
import com.milesight.beaveriot.user.model.request.UpdatePasswordRequest;
import com.milesight.beaveriot.user.model.request.UpdateUserRequest;
import com.milesight.beaveriot.user.model.request.UserListRequest;
import com.milesight.beaveriot.user.model.request.UserRegisterRequest;
import com.milesight.beaveriot.user.model.response.UserInfoResponse;
import com.milesight.beaveriot.user.model.response.UserMenuResponse;
import com.milesight.beaveriot.user.model.response.UserStatusResponse;
import com.milesight.beaveriot.user.po.TenantPO;
import com.milesight.beaveriot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 8:40
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/register")
    public ResponseBody<Void> register(HttpServletRequest request, @RequestBody UserRegisterRequest userRegisterRequest) {
        Long tenantId = request.getHeader(SecurityUserContext.TENANT_ID) != null ? Long.valueOf(request.getHeader(SecurityUserContext.TENANT_ID)) : null;
        TenantPO tenantPO = userService.analyzeTenantId(tenantId);
        if (tenantPO == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }
        tenantId = tenantPO.getId();
        userService.register(tenantId, userRegisterRequest);
        return ResponseBuilder.success();
    }

    @GetMapping("/status")
    public ResponseBody<UserStatusResponse> status(HttpServletRequest request) {
        Long tenantId = request.getHeader(SecurityUserContext.TENANT_ID) != null ? Long.valueOf(request.getHeader(SecurityUserContext.TENANT_ID)) : null;
        TenantPO tenantPO = userService.analyzeTenantId(tenantId);
        if (tenantPO == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }
        tenantId = tenantPO.getId();
        UserStatusResponse userStatusResponse = userService.status(tenantId);
        return ResponseBuilder.success(userStatusResponse);
    }

    @GetMapping("")
    public ResponseBody<UserInfoResponse> getUserInfo() {
        UserInfoResponse userInfoResponse = userService.getUserInfo();
        return ResponseBuilder.success(userInfoResponse);
    }

    @GetMapping("/members")
    public ResponseBody<Page<UserInfoResponse>> getUsers(@RequestBody UserListRequest userListRequest) {
        Page<UserInfoResponse> userInfoResponses = userService.getUsers(userListRequest);
        return ResponseBuilder.success(userInfoResponses);
    }

    @PostMapping("/members")
    public ResponseBody<Void> createUser(@RequestBody CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        return ResponseBuilder.success();
    }

    @PutMapping("/password")
    public ResponseBody<Void> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest) {
        userService.updatePassword(updatePasswordRequest);
        return ResponseBuilder.success();
    }

    @PutMapping("/members/{userId}")
    public ResponseBody<Void> updateUser(@PathVariable("userId") Long userId, @RequestBody UpdateUserRequest updateUserRequest) {
        userService.updateUser(userId, updateUserRequest);
        return ResponseBuilder.success();
    }

    @PutMapping("/members/{userId}/change-password")
    public ResponseBody<Void> changePassword(@PathVariable("userId") Long userId, @RequestBody ChangePasswordRequest changePasswordRequest) {
        userService.changePassword(userId, changePasswordRequest);
        return ResponseBuilder.success();
    }


    @DeleteMapping("/members/{userId}")
    public ResponseBody<Void> deleteUser(@PathVariable("userId") Long userId) {
        userService.deleteUser(userId);
        return ResponseBuilder.success();
    }

    @GetMapping("/members/{userId}/menus")
    public ResponseBody<List<UserMenuResponse>> getMenusByUserId(@PathVariable("userId") Long userId) {
        List<UserMenuResponse> userMenuResponses = userService.getMenusByUserId(userId);
        return ResponseBuilder.success(userMenuResponses);
    }

}
