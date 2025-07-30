package com.milesight.beaveriot.user.util;

import com.milesight.beaveriot.permission.enums.MenuCode;
import com.milesight.beaveriot.permission.enums.MenuItem;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.user.convert.MenuConverter;
import com.milesight.beaveriot.user.model.Menu;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import lombok.*;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author loong
 * @date 2024/11/21 16:13
 */
public class MenuStore {

    private static final List<? extends MenuItem> MENU_ITEMS = Stream.concat(
                    Arrays.stream(MenuCode.values()),
                    Arrays.stream(OperationPermissionCode.values()))
            .toList();

    private static final List<Menu> MENUS;

    private static final List<MenuResponse> MENU_TREES;

    static {
        if (MENU_ITEMS.size() != MENU_ITEMS.stream().map(MenuItem::getId).distinct().count()) {
            throw new IllegalStateException("Duplicate menu id");
        }
        MENUS = Collections.unmodifiableList(initMenus());
        MENU_TREES = Collections.unmodifiableList(initMenuTrees());
    }

    @NonNull
    public static List<Menu> getAllMenus() {
        return MENUS;
    }

    public static List<MenuResponse> getMenuTrees() {
        return MENU_TREES;
    }

    private static List<Menu> initMenus() {
        return MENU_ITEMS.stream()
                .map(menuItem -> {
                    val menu = new Menu();
                    menu.setId(menuItem.getId());
                    menu.setName(menuItem.getCode());
                    menu.setCode(menuItem.getCode());
                    menu.setType(menuItem.getType());

                    val parentMenu = menuItem.getParent();
                    if (parentMenu != null) {
                        menu.setParentId(parentMenu.getId());
                    }
                    return menu;
                })
                .sorted(Comparator.comparing(Menu::getId))
                .collect(Collectors.toList());
    }

    private static List<MenuResponse> initMenuTrees() {
        val menus = getAllMenus();
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }
        return getMenuTrees(menus, null);
    }

    private static List<MenuResponse> getMenuTrees(List<Menu> allMenus, Long parentId) {
        val menus = allMenus.stream()
                .filter(m -> Objects.equals(parentId, m.getParentId()))
                .toList();
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }

        return menus.stream()
                .map(menu -> {
                    val menuTree = MenuConverter.INSTANCE.convertResponse(menu);
                    val childMenuTrees = getMenuTrees(allMenus, menu.getId());
                    menuTree.setChildren(childMenuTrees);
                    return menuTree;
                })
                .collect(Collectors.toList());
    }

    private MenuStore() {
        throw new IllegalStateException("Utility class");
    }

}
