package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;

/**
 * A task that needs to know which case and step it runs for (case id, variables, step name...).
 * Implement this instead of {@code InvokableTask} on a {@code @Task} bean: the singleton bean is
 * then shared safely by concurrent cases, each call receiving its own context.
 */
@FunctionalInterface
public interface ContextualTask {

    TaskResponse execute(WorkflowContext context);
}
