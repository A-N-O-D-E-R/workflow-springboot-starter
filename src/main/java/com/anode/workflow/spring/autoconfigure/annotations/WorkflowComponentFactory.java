package com.anode.workflow.spring.autoconfigure.annotations;


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
 * @see com.anode.workflow.spring.autoconfigure.registry.WorkflowComponentFactoryRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowComponentFactory {
}