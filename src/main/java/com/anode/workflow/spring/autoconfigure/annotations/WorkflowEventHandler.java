package com.anode.workflow.spring.autoconfigure.annotations;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a workflow event handler.
 *
 * <p>Event handlers listen to and react to workflow lifecycle events such as task completion,
 * workflow state transitions, and error conditions. Classes annotated with this are automatically
 * discovered and registered with the workflow engine.
 *
 * <p><b>Usage example:</b>
 * <pre>
 * @WorkflowEventHandler
 * public class WorkflowEventListener implements EventHandler {
 *     public void onTaskCompleted(WorkflowContext context) {
 *         // Handle task completion
 *     }
 * }
 * </pre>
 *
 * @see com.anode.workflow.spring.autoconfigure.registry.WorkflowEventHandlerRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowEventHandler {
}

