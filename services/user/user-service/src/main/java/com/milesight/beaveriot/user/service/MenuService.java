package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.user.convert.MenuConverter;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import com.milesight.beaveriot.user.po.MenuPO;
import com.milesight.beaveriot.user.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/21 16:13
 */
@Service
public class MenuService {

    @Autowired
    MenuRepository menuRepository;

    public List<MenuResponse> getMenus() {
        List<MenuPO> menuPOS = menuRepository.findAll();
        if (menuPOS == null || menuPOS.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuPO> rootMenuPOS = menuPOS.stream().filter(menuPO -> menuPO.getParentId() == null).toList();
        if (rootMenuPOS.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuResponse> menuResponses = new ArrayList<>();
        rootMenuPOS.forEach(menuPO -> {
            MenuResponse menuResponse = MenuConverter.INSTANCE.convertResponse(menuPO);
            List<MenuResponse> menuChild = recurrenceChildMenu(menuPOS, menuPO.getId());
            menuResponse.setChildren(menuChild);
            menuResponses.add(menuResponse);
        });
        return menuResponses;
    }

    private List<MenuResponse> recurrenceChildMenu(List<MenuPO> menuPOS, Long parentId) {
        List<MenuPO> menuChs = menuPOS.stream().filter(t -> t.getParentId() != null && t.getParentId().equals(parentId)).toList();
        if (menuChs.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuResponse> menuChResponses = new ArrayList<>();
        menuChs.forEach(t -> {
            MenuResponse menuChResponse = MenuConverter.INSTANCE.convertResponse(t);
            List<MenuResponse> menuChild = recurrenceChildMenu(menuPOS, t.getId());
            menuChResponse.setChildren(menuChild);
            menuChResponses.add(menuChResponse);
        });
        return menuChResponses;
    }

}
