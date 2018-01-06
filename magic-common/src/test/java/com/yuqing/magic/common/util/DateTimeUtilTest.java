package com.yuqing.magic.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.Time;

/**
 * DateTimeUtil的测试类
 * @author yuqing
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class DateTimeUtilTest {

    @Test
    public void formatTime() {
        Assert.assertEquals("02:03:04", DateTimeUtil.format(new Time(2, 3, 4)));
        Assert.assertEquals("24:00:00", DateTimeUtil.format(new Time(24, 0, 0)));
    }

}
