package com.yuqing.magic.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author yuqing
 * @date 2018-01-13
 */
@RunWith(JUnit4.class)
public class HttpClientUtilTest {

    @Test
    public void get() {
        String body = HttpClientUtil.get("http://www.baidu.com");

        Assert.assertNotNull(body);
    }

}
