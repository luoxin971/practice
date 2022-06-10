package com.luoxin.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * content
 *
 * @author luoxin
 * @since 2022/4/21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrafficLimit {

  String limitKey() default "";

  double value() default Double.MAX_VALUE;
}
