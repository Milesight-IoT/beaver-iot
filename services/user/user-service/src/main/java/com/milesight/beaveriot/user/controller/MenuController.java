package com.milesight.beaveriot.user.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import com.milesight.beaveriot.user.util.MenuStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/21 16:13
 */
@RestController
@RequestMapping("/user/menus")
public class MenuController {

    @GetMapping
    public ResponseBody<List<MenuResponse>> getMenus() {
        List<MenuResponse> menuResponses = MenuStore.getMenuTrees();
        return ResponseBuilder.success(menuResponses);
    }
}
