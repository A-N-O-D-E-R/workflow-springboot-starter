package com.anode.workflow.spring.autoconfigure.annotations;


import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowComponentFactory {
}