package com.anode.workflow.spring.autoconfigure.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BeanNameValidator}.
 */
class BeanNameValidatorTest {

    // ========== Valid Bean Names ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "myTask",
        "my_task",
        "my-task",
        "my.task",
        "myTask123",
        "task1",
        "t",
        "T",
        "camelCaseTask",
        "kebab-case-task",
        "snake_case_task",
        "dot.notation.task",
        "mixed_Case-Task.v1"
    })
    void shouldAcceptValidBeanNames(String beanName) {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult(beanName, null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldAcceptSimpleCamelCase() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("myTaskName", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldAcceptKebabCase() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("my-task-name", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    // ========== Null and Empty Validation ==========

    @Test
    void shouldRejectNullBeanName() {
        // When/Then
        assertThatThrownBy(() -> BeanNameValidator.validate(null, TestClass.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bean name cannot be null");
    }

    @Test
    void shouldRejectEmptyBeanName() {
        // When/Then
        assertThatThrownBy(() -> BeanNameValidator.validate("", TestClass.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bean name cannot be empty");
    }

    @Test
    void shouldRejectWhitespaceOnlyBeanName() {
        // When/Then
        assertThatThrownBy(() -> BeanNameValidator.validate("   ", TestClass.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bean name cannot be empty");
    }

    // ========== First Character Validation ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "1task",
        "9myTask",
        "_task",
        "-task",
        ".task",
        "$task",
        "#task",
        "@task"
    })
    void shouldRejectBeanNameNotStartingWithLetter(String beanName) {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult(beanName, null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("must start with a letter");
    }

    // ========== Invalid Characters Validation ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "my$task",
        "my@task",
        "my#task",
        "my task",  // space
        "my\ttask",  // tab
        "my/task",
        "my\\task",
        "my*task",
        "my+task",
        "my=task",
        "my[task]",
        "my{task}",
        "my(task)",
        "my&task",
        "my%task",
        "my!task",
        "my?task"
    })
    void shouldRejectBeanNameWithInvalidCharacters(String beanName) {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult(beanName, null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
            .anyMatch(error -> error.contains("contains invalid characters"));
    }

    // ========== Ending Character Validation ==========

    @Test
    void shouldRejectBeanNameEndingWithDot() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("myTask.", null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
            .anyMatch(error -> error.contains("cannot end with special characters"));
    }

    @Test
    void shouldRejectBeanNameEndingWithHyphen() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("myTask-", null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
            .anyMatch(error -> error.contains("cannot end with special characters"));
    }

    @Test
    void shouldAcceptBeanNameEndingWithUnderscore() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("myTask_", null);

        // Then - underscore at the end is allowed
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldAcceptBeanNameEndingWithNumber() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("myTask123", null);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    // ========== Reserved Names Validation ==========

    @ParameterizedTest
    @ValueSource(strings = {
        "applicationContext",
        "beanFactory",
        "environment",
        "systemProperties",
        "systemEnvironment",
        "messageSource",
        "applicationEventMulticaster",
        "lifecycleProcessor",
        "springApplicationArguments",
        "conversionService",
        "transactionManager"
    })
    void shouldRejectReservedBeanNames(String reservedName) {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult(reservedName, null);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
            .anyMatch(error -> error.contains("reserved Spring bean name"));
    }

    // ========== Warnings ==========

    @Test
    void shouldWarnForVeryShortBeanName() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("t", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings())
            .anyMatch(warning -> warning.contains("very short"));
    }

    @Test
    void shouldWarnForVeryLongBeanName() {
        // Given - name longer than 255 characters
        String longName = "a" + "b".repeat(300);

        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult(longName, null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings())
            .anyMatch(warning -> warning.contains("very long"));
    }

    @Test
    void shouldWarnForConsecutiveSpecialCharacters() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("my--task..name", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings())
            .anyMatch(warning -> warning.contains("consecutive special characters"));
    }

    @Test
    void shouldWarnForLeadingOrTrailingWhitespace() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("  myTask  ", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings())
            .anyMatch(warning -> warning.contains("leading or trailing whitespace"));
    }

    @Test
    void shouldWarnForCaseInsensitiveReservedNames() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("ApplicationContext", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings())
            .anyMatch(warning -> warning.contains("similar to a reserved Spring bean name"));
    }

    @Test
    void shouldWarnForNumericSuffix() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("myTask123", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings())
            .anyMatch(warning -> warning.contains("ends with multiple digits"));
    }

    // ========== Context Class in Error Messages ==========

    @Test
    void shouldIncludeContextClassInErrorMessage() {
        // When/Then
        assertThatThrownBy(() -> BeanNameValidator.validate("123invalid", TestClass.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("TestClass");
    }

    @Test
    void shouldWorkWithoutContextClass() {
        // When/Then
        assertThatThrownBy(() -> BeanNameValidator.validate("123invalid", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must start with a letter");
    }

    // ========== Edge Cases ==========

    @Test
    void shouldAcceptSingleLetterBeanName() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("a", null);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldAcceptUppercaseStartLetter() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("MyTask", null);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldAcceptMixedCaseWithNumbers() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("MyTask123WithNumbers", null);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldAcceptAllLowercase() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("mytask", null);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void shouldRejectUnicodeLetters() {
        // When
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("tâche", null);

        // Then - should reject unicode letters (only ASCII is accepted for consistency)
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors())
            .anyMatch(error -> error.contains("contains invalid characters"));
    }

    // ========== Validation Result toString() ==========

    @Test
    void shouldFormatValidationResultToString() {
        // Given
        BeanNameValidator.ValidationResult result =
            BeanNameValidator.validateWithResult("123invalid", TestClass.class);

        // When
        String str = result.toString();

        // Then
        assertThat(str).contains("valid=false");
        assertThat(str).contains("errors=");
    }

    // Test class for context
    private static class TestClass {}
}
