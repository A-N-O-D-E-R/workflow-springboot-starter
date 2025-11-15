package com.anode.workflow.spring.autoconfigure.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link BeanNameUtils}.
 */
class BeanNameUtilsTest {

    @Test
    void shouldDeriveBeanNameFromSimpleClass() {
        // Given
        class MyTask {}

        // When
        String beanName = BeanNameUtils.deriveBeanName(MyTask.class);

        // Then
        assertThat(beanName).isEqualTo("myTask");
    }

    @Test
    void shouldHandleSingleCharacterClassName() {
        // Given
        class A {}

        // When
        String beanName = BeanNameUtils.deriveBeanName(A.class);

        // Then
        assertThat(beanName).isEqualTo("a");
    }

    @Test
    void shouldHandleCamelCaseClassName() {
        // Given
        class WorkflowEngine {}

        // When
        String beanName = BeanNameUtils.deriveBeanName(WorkflowEngine.class);

        // Then
        assertThat(beanName).isEqualTo("workflowEngine");
    }

    @Test
    void shouldHandleAcronymClassName() {
        // Given
        class SLAQueueManager {}

        // When
        String beanName = BeanNameUtils.deriveBeanName(SLAQueueManager.class);

        // Then
        assertThat(beanName).isEqualTo("sLAQueueManager");
    }

    @Test
    void shouldHandleAlreadyLowercaseFirstChar() {
        // Given
        class myService {}

        // When
        String beanName = BeanNameUtils.deriveBeanName(myService.class);

        // Then
        assertThat(beanName).isEqualTo("myService");
    }

    @Test
    void shouldThrowExceptionForNullClass() {
        // When/Then
        assertThatThrownBy(() -> BeanNameUtils.deriveBeanName(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class cannot be null");
    }

    @Test
    void shouldHandleClassWithNumbers() {
        // Given
        class Task123 {}

        // When
        String beanName = BeanNameUtils.deriveBeanName(Task123.class);

        // Then
        assertThat(beanName).isEqualTo("task123");
    }
}
