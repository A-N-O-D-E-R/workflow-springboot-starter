package com.anode.workflow.spring.autoconfigure.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an SLA (Service Level Agreement) queue manager component.
 *
 * <p>SLA queue managers handle priority-based task queuing and ensure that workflow tasks
 * are processed within specified time constraints. Classes annotated with this are automatically
 * discovered and registered as SLA managers in the workflow engine.
 *
 * <p><b>Usage example:</b>
 * <pre>
 * @SlaQueueManagerComponent
 * public class CustomSlaQueueManager implements SlaQueueManager {
 *     // SLA queue management implementation
 * }
 * </pre>
 *
 * @see com.anode.workflow.spring.autoconfigure.registry.SlaQueueManagerRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlaQueueManagerComponent {
}