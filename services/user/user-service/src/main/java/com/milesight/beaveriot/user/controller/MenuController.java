package com.milesight.beaveriot.user.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.user.model.request.CreateMenuRequest;
import com.milesight.beaveriot.user.model.request.UpdateMenuRequest;
import com.milesight.beaveriot.user.model.response.CreateMenuResponse;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import com.milesight.beaveriot.user.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @date 2024/11/21 16:13
 */
@RestController
@RequestMapping("/user/menus")
public class MenuController {

    @Autowired
    MenuService menuService;

    @PostMapping("")
    public ResponseBody<CreateMenuResponse> createMenu(@RequestBody CreateMenuRequest createMenuRequest) {
        CreateMenuResponse createMenuResponse = menuService.createMenu(createMenuRequest);
        return ResponseBuilder.success(createMenuResponse);
    }

    @PutMapping("/{menuId}")
    public ResponseBody<Void> updateMenu(@PathVariable("menuId") Long menuId, @RequestBody UpdateMenuRequest updateMenuRequest) {
        menuService.updateMenu(menuId, updateMenuRequest);
        return ResponseBuilder.success();
    }

    @DeleteMapping("/{menuId}")
    public ResponseBody<Void> deleteMenu(@PathVariable("menuId") Long menuId) {
        menuService.deleteMenu(menuId);
        return ResponseBuilder.success();
    }

    @GetMapping("")
    public ResponseBody<List<MenuResponse>> getMenus() {
        List<MenuResponse> menuResponses = menuService.getMenus();
        return ResponseBuilder.success(menuResponses);
    }

}
