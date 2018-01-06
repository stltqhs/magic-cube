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

    @Test
    public void second2Human() {
        Assert.assertEquals("0小时3分3秒",
                DateTimeUtil.second2Human(183, "%h小时%m分%s秒", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("00小时03分03秒",
                DateTimeUtil.second2Human(183, "%02h小时%02m分%02s秒", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("%%",
                DateTimeUtil.second2Human(183, "%%%", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("%%",
                DateTimeUtil.second2Human(183, "%%%%", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("1234",
                DateTimeUtil.second2Human(183, "1234", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("%tttt",
                DateTimeUtil.second2Human(183, "%tttt", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("3m",
                DateTimeUtil.second2Human(183, "%mm", DateTimeUtil.DEFAULT_STYLE));
        Assert.assertEquals("3分3秒",
                DateTimeUtil.second2Human(183, "%h小时%m分%s秒", DateTimeUtil.STRIP_LEADING_ZERO_FIELD_STYLE | DateTimeUtil.STRIP_TAILING_ZERO_FIELD_STYLE));
        Assert.assertEquals("1分",
                DateTimeUtil.second2Human(60, "%h小时%m分%s秒", DateTimeUtil.STRIP_LEADING_ZERO_FIELD_STYLE | DateTimeUtil.STRIP_TAILING_ZERO_FIELD_STYLE));
        Assert.assertEquals("1分0秒",
                DateTimeUtil.second2Human(60, "%h小时%m分%s秒", DateTimeUtil.STRIP_LEADING_ZERO_FIELD_STYLE));
        Assert.assertEquals("0分0秒",
                DateTimeUtil.second2Human(0, "%h小时%m分%s秒", DateTimeUtil.STRIP_LEADING_ZERO_FIELD_STYLE));
        Assert.assertEquals("0秒",
                DateTimeUtil.second2Human(0, "%h小时%m分%s秒", DateTimeUtil.RECURSIVE_STRIP_LEADING_ZERO_FIELD_STYLE));
        Assert.assertEquals("0秒",
                DateTimeUtil.second2Human(0, "%h小时%m分%s秒", DateTimeUtil.RECURSIVE_STRIP_LEADING_ZERO_FIELD_STYLE | DateTimeUtil.STRIP_TAILING_ZERO_FIELD_STYLE));
        Assert.assertEquals("3秒",
                DateTimeUtil.second2Human(3, "%h小时%m分%s秒", DateTimeUtil.RECURSIVE_STRIP_LEADING_ZERO_FIELD_STYLE | DateTimeUtil.STRIP_TAILING_ZERO_FIELD_STYLE));
        Assert.assertEquals("5小时",
                DateTimeUtil.second2Human(5 * 60 * 60, "%h小时%m分%s秒", DateTimeUtil.STRIP_LEADING_ZERO_FIELD_STYLE | DateTimeUtil.STRIP_TAILING_ZERO_FIELD_STYLE | DateTimeUtil.RECURSIVE_STRIP_FLAG));
    }
}
