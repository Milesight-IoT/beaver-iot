package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.user.convert.MenuConverter;
import com.milesight.beaveriot.user.enums.MenuType;
import com.milesight.beaveriot.user.model.request.CreateMenuRequest;
import com.milesight.beaveriot.user.model.request.UpdateMenuRequest;
import com.milesight.beaveriot.user.model.response.CreateMenuResponse;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import com.milesight.beaveriot.user.po.MenuPO;
import com.milesight.beaveriot.user.repository.MenuRepository;
import com.milesight.beaveriot.user.repository.RoleMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    @Autowired
    RoleMenuRepository roleMenuRepository;

    public CreateMenuResponse createMenu(CreateMenuRequest createMenuRequest) {
        String code = createMenuRequest.getCode();
        String name = createMenuRequest.getName();
        MenuType type = createMenuRequest.getType();
        String parentId = createMenuRequest.getParentId();
        if (!StringUtils.hasText(code)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("code is empty").build();
        }
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }
        if (type == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("type is empty").build();
        }
        if (!StringUtils.hasText(parentId) && type == MenuType.FUNCTION) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("function can not be root").build();
        }
        MenuPO menuPO = menuRepository.findOne(filterable -> filterable.eq(MenuPO.Fields.code, code)).orElse(null);
        if (menuPO != null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("code already exist").build();
        }
        menuPO = new MenuPO();
        menuPO.setId(SnowflakeUtil.nextId());
        menuPO.setCode(code);
        menuPO.setName(name);
        menuPO.setType(type);
        menuPO.setParentId(parentId == null ? null : Long.valueOf(parentId));
        menuRepository.save(menuPO);

        CreateMenuResponse createMenuResponse = new CreateMenuResponse();
        createMenuResponse.setMenuId(menuPO.getId().toString());
        return createMenuResponse;
    }

    public void updateMenu(Long menuId, UpdateMenuRequest updateMenuRequest) {
        String name = updateMenuRequest.getName();
        MenuType type = updateMenuRequest.getType();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }
        if (type == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("type is empty").build();
        }
        MenuPO menuPO = menuRepository.findUniqueOne(filterable -> filterable.eq(MenuPO.Fields.id, menuId));
        menuPO.setName(name);
        menuPO.setType(type);
        menuRepository.save(menuPO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        MenuPO menuPO = menuRepository.findUniqueOne(filterable -> filterable.eq(MenuPO.Fields.id, menuId));
        if (menuPO == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("menu not found").build();
        }
        menuRepository.deleteById(menuId);
        roleMenuRepository.deleteByMenuId(menuId);
    }

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
