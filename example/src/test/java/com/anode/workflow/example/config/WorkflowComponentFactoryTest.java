package com.anode.workflow.example.config;

import com.anode.workflow.example.routes.ShippingMethodRoute;
import com.anode.workflow.example.tasks.ValidateOrderTask;
import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowComponentFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ValidateOrderTask validateOrderTask;

    @Mock
    private ShippingMethodRoute shippingMethodRoute;

    private WorkflowComponentFactory componentFactory;

    @BeforeEach
    void setUp() {
        componentFactory = new WorkflowComponentFactory(applicationContext);
    }

    @Test
    void shouldGetTaskByName() {
        // Given: Application context has a task bean
        when(applicationContext.getBean("validateOrderTask", InvokableTask.class))
                .thenReturn(validateOrderTask);

        // When: Getting task by name
        InvokableTask task = componentFactory.getTask("validateOrderTask");

        // Then: Should return the task
        assertThat(task).isNotNull();
        assertThat(task).isEqualTo(validateOrderTask);
        verify(applicationContext).getBean("validateOrderTask", InvokableTask.class);
    }

    @Test
    void shouldGetRouteByName() {
        // Given: Application context has a route bean
        when(applicationContext.getBean("shippingMethodRoute", InvokableRoute.class))
                .thenReturn(shippingMethodRoute);

        // When: Getting route by name
        InvokableRoute route = componentFactory.getRoute("shippingMethodRoute");

        // Then: Should return the route
        assertThat(route).isNotNull();
        assertThat(route).isEqualTo(shippingMethodRoute);
        verify(applicationContext).getBean("shippingMethodRoute", InvokableRoute.class);
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFound() {
        // Given: Application context throws exception for unknown task
        when(applicationContext.getBean("unknownTask", InvokableTask.class))
                .thenThrow(new RuntimeException("No bean named 'unknownTask'"));

        // When/Then: Should throw exception with meaningful message
        assertThatThrownBy(() -> componentFactory.getTask("unknownTask"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown task component: unknownTask");
    }

    @Test
    void shouldThrowExceptionWhenRouteNotFound() {
        // Given: Application context throws exception for unknown route
        when(applicationContext.getBean("unknownRoute", InvokableRoute.class))
                .thenThrow(new RuntimeException("No bean named 'unknownRoute'"));

        // When/Then: Should throw exception with meaningful message
        assertThatThrownBy(() -> componentFactory.getRoute("unknownRoute"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown route component: unknownRoute");
    }

    @Test
    void shouldHandleNullComponentName() {
        // Given: Null component name
        when(applicationContext.getBean(null, InvokableTask.class))
                .thenThrow(new IllegalArgumentException("Bean name cannot be null"));

        // When/Then: Should throw exception
        assertThatThrownBy(() -> componentFactory.getTask(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleEmptyComponentName() {
        // Given: Empty component name
        when(applicationContext.getBean("", InvokableTask.class))
                .thenThrow(new IllegalArgumentException("Bean name cannot be empty"));

        // When/Then: Should throw exception
        assertThatThrownBy(() -> componentFactory.getTask(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
