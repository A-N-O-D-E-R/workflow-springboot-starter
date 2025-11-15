package com.anode.workflow.spring.autoconfigure.annotations;


import java.lang.annotation.*;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Task {
    @AliasFor(annotation= Component.class, attribute = "value")
    String value() default "";
    int order() default 0;
    String userData() default "";
}
