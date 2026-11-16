package com.anode.workflow.spring.autoconfigure.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a workflow component factory.
 *
 * <p>Component factories are responsible for creating and managing workflow components
 * that can be registered in the Spring context. This annotation indicates that a class
 * should be scanned and registered as a factory for workflow components.
 *
 * <p><b>Usage example:</b>
 * <pre>
 * @WorkflowComponentFactory
 * public class PaymentComponentFactory implements IWorkflowComponentFactory {
 *     // Factory implementation
 * }
 * </pre>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface WorkflowComponentFactory {

    /** Bean name; derived from the class name when empty. */
    @AliasFor(annotation = Component.class, attribute = "value")
    String value() default "";
}