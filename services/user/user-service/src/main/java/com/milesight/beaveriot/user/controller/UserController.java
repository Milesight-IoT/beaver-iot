package com.milesight.beaveriot.user.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.user.model.request.UserRegisterRequest;
import com.milesight.beaveriot.user.model.response.UserInfoResponse;
import com.milesight.beaveriot.user.model.response.UserStatusResponse;
import com.milesight.beaveriot.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseBody<Void> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        userService.register(userRegisterRequest);
        return ResponseBuilder.success();
    }

    @GetMapping("/status")
    public ResponseBody<UserStatusResponse> status() {
        UserStatusResponse userStatusResponse = userService.status();
        return ResponseBuilder.success(userStatusResponse);
    }

    @GetMapping("")
    public ResponseBody<UserInfoResponse> getUserInfo() {
        UserInfoResponse userInfoResponse = userService.getUserInfo();
        return ResponseBuilder.success(userInfoResponse);
    }

}
