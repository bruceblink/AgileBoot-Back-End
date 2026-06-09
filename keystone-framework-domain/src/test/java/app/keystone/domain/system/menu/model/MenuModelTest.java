package app.keystone.domain.system.menu.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.common.enums.common.MenuTypeEnum;
import app.keystone.domain.system.menu.db.SysMenuService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class MenuModelTest {

    private static final long MENU_ID = 1L;

    private final SysMenuService menuService = mock(SysMenuService.class);

    private final MenuModelFactory menuModelFactory = new MenuModelFactory(menuService);

    @Test
    void testCheckMenuNameUnique() {
        MenuModel menuModel = menuModelFactory.create();
        menuModel.setMenuName("menu 1");
        when(
            menuService.isMenuNameDuplicated(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, menuModel::checkMenuNameUnique);
        Assertions.assertEquals(Business.MENU_NAME_IS_NOT_UNIQUE, exception.getErrorCode());
    }

    @Test
    void testCheckExternalLinkWhenSuccessful() {
        MenuModel notExternalButWithoutHttpPrefix = new MenuModel();
        notExternalButWithoutHttpPrefix.setMenuType(MenuTypeEnum.MENU.getValue());
        notExternalButWithoutHttpPrefix.setPath("www.baidu.com");

        MenuModel isExternalWithHttpPrefix = new MenuModel();
        isExternalWithHttpPrefix.setMenuType(MenuTypeEnum.OUTSIDE_LINK_REDIRECT.getValue());
        isExternalWithHttpPrefix.setPath("http://www.baidu.com");

        Assertions.assertDoesNotThrow(()->{
            notExternalButWithoutHttpPrefix.checkExternalLink();
            isExternalWithHttpPrefix.checkExternalLink();
        });
    }

    @Test
    void testCheckExternalLinkWhenFailed() {
        MenuModel externalWithoutHttpPrefix = new MenuModel();
        externalWithoutHttpPrefix.setMenuType(MenuTypeEnum.OUTSIDE_LINK_REDIRECT.getValue());
        externalWithoutHttpPrefix.setPath("www.baidu.com");

        ApiException exception = assertThrows(ApiException.class, externalWithoutHttpPrefix::checkExternalLink);

        Assertions.assertEquals(Business.MENU_EXTERNAL_LINK_MUST_BE_HTTP, exception.getErrorCode());
    }



    @Test
    void testCheckParentIdConflict() {
        MenuModel menuModel = menuModelFactory.create();
        menuModel.setMenuId(MENU_ID);
        menuModel.setParentId(MENU_ID);

        ApiException exception = assertThrows(ApiException.class, menuModel::checkParentIdConflict);

        Assertions.assertEquals(Business.MENU_PARENT_ID_NOT_ALLOW_SELF, exception.getErrorCode());
    }

    @Test
    void testCheckHasChildMenus() {
        MenuModel menuModel = menuModelFactory.create();
        menuModel.setMenuId(MENU_ID);
        when(menuService.hasChildrenMenu(MENU_ID)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, menuModel::checkHasChildMenus);

        Assertions.assertEquals(Business.MENU_EXIST_CHILD_MENU_NOT_ALLOW_DELETE, exception.getErrorCode());

    }

    @Test
    void testCheckMenuAlreadyAssignToRole() {
        MenuModel menuModel = menuModelFactory.create();
        menuModel.setMenuId(MENU_ID);
        when(menuService.isMenuAssignToRoles(MENU_ID)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, menuModel::checkMenuAlreadyAssignToRole);

        Assertions.assertEquals(Business.MENU_ALREADY_ASSIGN_TO_ROLE_NOT_ALLOW_DELETE, exception.getErrorCode());
    }
}
