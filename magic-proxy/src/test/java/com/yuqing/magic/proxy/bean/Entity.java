package com.yuqing.magic.proxy.bean;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
public class Entity {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
