package com.anode.workflow.spring.autoconfigure.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AliasFor;

/**
 * Marks a class as a workflow task that can be invoked as a step in a workflow definition.
 *
 * <p>This annotation automatically registers the class as a Spring component and makes it
 * discoverable by the workflow task scanner.
 *
 * <p><b>Usage example:</b>
 * <pre>
 * @Task("processPayment")
 * public class PaymentTask implements WorkflowTask {
 *     public void execute(WorkflowContext context) {
 *         // Task logic here
 *     }
 * }
 * </pre>
 *
 * @see com.anode.workflow.spring.autoconfigure.scanner.TaskScanner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Task {
    /**
     * The bean name for this task component.
     * If not provided, Spring will derive one from the class name.
     *
     * @return the bean name
     */
    @AliasFor(annotation= Component.class, attribute = "value")
    String value() default "";

    /**
     * Optional execution order within a workflow step.
     * Default is 0.
     *
     * @return the execution order
     */
    int order() default 0;

    /**
     * Optional user data to attach to the task metadata.
     * This can be used to store custom metadata about the task.
     *
     * @return the user data
     */
    String userData() default "";
}
