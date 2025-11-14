package com.anode.workflow.spring.autoconfigure.impl;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultWorkflowComponentFactory}.
 */
class DefaultWorkflowComponentFactoryTest {

    private DefaultWorkflowComponentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultWorkflowComponentFactory();
    }

    @Test
    void shouldImplementWorkflowComponentFactory() {
        assertThat(factory).isInstanceOf(WorkflowComponantFactory.class);
    }

    @Test
    void shouldReturnNullForValidContext() {
        WorkflowContext context = mock(WorkflowContext.class);

        Object result = factory.getObject(context);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForNullContext() {
        Object result = factory.getObject(null);

        assertThat(result).isNull();
    }

    @Test
    void shouldBeInstantiable() {
        assertThat(factory).isNotNull();
    }
}
