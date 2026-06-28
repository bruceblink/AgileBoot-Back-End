package app.keystone.common.enums;


import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.common.enums.common.UserStatusEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BasicEnumUtilTest {

    @Test
    public void testFromValue() {

        YesOrNoEnum yes = BasicEnumUtil.fromValue(YesOrNoEnum.class, 1);
        YesOrNoEnum no = BasicEnumUtil.fromValue(YesOrNoEnum.class, 0);

        Assertions.assertEquals(yes.description(), "是");
        Assertions.assertEquals(no.description(), "否");

    }

    @Test
    public void dictionaryEnumShouldReturnConfiguredCssTag() {
        Assertions.assertEquals("warning", UserStatusEnum.FROZEN.cssTag());
    }
}
