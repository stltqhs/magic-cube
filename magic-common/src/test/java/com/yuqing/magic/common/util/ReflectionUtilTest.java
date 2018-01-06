package com.yuqing.magic.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * ReflectionUtil测试类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class ReflectionUtilTest {

    @Test
    public void getUnsafe() {
        Assert.assertNotNull(ReflectionUtil.getUnsafe());
    }

    @Test
    public void getField() {
        Assert.assertNotNull(ReflectionUtil.getField(Proxy.class, "h", true));
        Assert.assertNotNull(ReflectionUtil.getField(Field.class, "ACCESS_PERMISSION", true));
        Assert.assertNull(ReflectionUtil.getField(Field.class, "ACCESS_PERMISSION", false));
    }

    @Test
    public void getFieldValue() {
        Assert.assertNotNull(ReflectionUtil.getFieldValue(Proxy.class, "proxyClassNamePrefix"));
    }
}
