package com.yuqing.magic.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * MD5Util帮助类
 * @author yuqing
 */
@RunWith(JUnit4.class)
public class MD5UtilTest {

    @Test
    public void encodeString() {
        Assert.assertEquals("e10adc3949ba59abbe56e057f20f883e", MD5Util.encode("123456"));
    }

}
