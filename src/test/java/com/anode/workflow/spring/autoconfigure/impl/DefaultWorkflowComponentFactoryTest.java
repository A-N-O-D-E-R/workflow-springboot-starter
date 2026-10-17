package com.anode.workflow.spring.autoconfigure.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.steps.responses.RouteResponse;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.spring.autoconfigure.runtime.ContextualRoute;
import com.anode.workflow.spring.autoconfigure.runtime.ContextualTask;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** {@link DefaultWorkflowComponentFactory} resolves a step's component by bean name. */
class DefaultWorkflowComponentFactoryTest {

    private static final TaskResponse PROCEED = new TaskResponse(StepResponseType.OK_PROCEED, "", "");

    private final StaticListableBeanFactory beans = new StaticListableBeanFactory();
    private WorkflowComponantFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultWorkflowComponentFactory(beans);
    }

    private static WorkflowContext context(StepType type, String component, String caseId) {
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(type);
        when(context.getCompName()).thenReturn(component);
        when(context.getCaseId()).thenReturn(caseId);
        return context;
    }

    @Test
    void returnsTheInvokableTaskBeanNamedByTheStep() {
        InvokableTask task = () -> PROCEED;
        beans.addBean("prepare", task);

        assertThat(factory.getObject(context(StepType.TASK, "prepare", "C-1"))).isSameAs(task);
    }

    @Test
    void aContextualTaskReceivesTheContextOfEachCall() {
        List<String> seen = new java.util.ArrayList<>();
        beans.addBean("review", (ContextualTask) ctx -> {
            seen.add(ctx.getCaseId());
            return PROCEED;
        });

        ((InvokableTask) factory.getObject(context(StepType.TASK, "review", "C-1"))).executeStep();
        ((InvokableTask) factory.getObject(context(StepType.TASK, "review", "C-2"))).executeStep();

        assertThat(seen).containsExactly("C-1", "C-2");
    }

    @ParameterizedTest
    @EnumSource(names = {"S_ROUTE", "P_ROUTE", "P_ROUTE_DYNAMIC"})
    void returnsRoutesForAllRouteTypes(StepType type) {
        InvokableRoute route = () -> new RouteResponse(StepResponseType.OK_PROCEED, List.of("yes"), "");
        beans.addBean("decide", route);

        assertThat(factory.getObject(context(type, "decide", "C-1"))).isSameAs(route);
    }

    @Test
    void aContextualRouteReceivesTheContext() {
        beans.addBean("decide", (ContextualRoute) ctx ->
                new RouteResponse(StepResponseType.OK_PROCEED, List.of(ctx.getCaseId()), ""));

        var route = (InvokableRoute) factory.getObject(context(StepType.S_ROUTE, "decide", "C-9"));

        assertThat(route.executeRoute().getBranches()).containsExactly("C-9");
    }

    @Test
    void unknownBeanIsAnError() {
        assertThatThrownBy(() -> factory.getObject(context(StepType.TASK, "missing", "C-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No task bean found with name: 'missing'");
    }

    @Test
    void aBeanOfTheWrongKindIsAnError() {
        beans.addBean("notATask", "just a string");

        assertThatThrownBy(() -> factory.getObject(context(StepType.TASK, "notATask", "C-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("neither an InvokableTask nor a ContextualTask");
    }

    @ParameterizedTest
    @EnumSource(names = {"PAUSE", "P_JOIN", "PERSIST"})
    void engineHandledStepTypesAreNotComponents(StepType type) {
        assertThatThrownBy(() -> factory.getObject(context(type, "x", "C-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported component type");
    }

    @Test
    void rejectsIncompleteContexts() {
        assertThatThrownBy(() -> factory.getObject(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.getObject(context(null, "x", "C-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.getObject(context(StepType.TASK, " ", "C-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
