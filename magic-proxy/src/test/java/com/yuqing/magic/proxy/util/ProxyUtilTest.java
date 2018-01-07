package com.yuqing.magic.proxy.util;

import com.yuqing.magic.proxy.bean.Entity;
import com.yuqing.magic.proxy.callback.EntityCallback;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * ProxyUtil测试类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class ProxyUtilTest {

    @Test
    public void cglibProxy() {
        Entity entity = new Entity();
        EntityCallback entityCallback = new EntityCallback(entity);
        entity = (Entity) ProxyUtil.cglibProxy(Entity.class, entityCallback);

        entity.setName("hello");

        Assert.assertEquals("hello", entity.getName());

        Assert.assertEquals(entity.getName(), entity.toString());
    }

}
