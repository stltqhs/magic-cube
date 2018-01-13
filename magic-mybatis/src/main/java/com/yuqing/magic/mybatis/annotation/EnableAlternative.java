package com.yuqing.magic.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
@Target({ElementType.TYPE})
@Retention(RUNTIME)
public @interface EnableAlternative {
}
