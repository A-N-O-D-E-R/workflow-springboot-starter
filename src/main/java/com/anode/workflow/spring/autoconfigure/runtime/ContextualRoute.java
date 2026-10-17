package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.entities.steps.responses.RouteResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;

/** A route bean that receives the context of the case it routes; see {@link ContextualTask}. */
@FunctionalInterface
public interface ContextualRoute {

    RouteResponse execute(WorkflowContext context);
}
