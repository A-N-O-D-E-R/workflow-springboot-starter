package com.anode.workflow.spring.autoconfigure.annotations;


import java.lang.annotation.*;

import org.springframework.stereotype.Component;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Task {
    String value() default "";
    int order() default 0;
    String userData() default "";
}
