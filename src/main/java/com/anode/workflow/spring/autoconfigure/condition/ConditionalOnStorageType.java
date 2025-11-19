package com.anode.workflow.spring.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;

/**
 * Conditional annotation that checks if any workflow engine is configured to use a specific storage type.
 *
 * <p>This ensures that storage beans are only created when they're actually needed
 * based on the workflow configuration.
 *
 * <p>Example usage:
 * <pre>
 * &#64;Bean
 * &#64;ConditionalOnStorageType(StorageType.JPA)
 * public CommonService jpaCommonService() {
 *     // Only created if at least one engine uses JPA storage
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnStorageTypeCondition.class)
public @interface ConditionalOnStorageType {

    /**
     * The storage type that must be configured in at least one engine.
     */
    StorageType value();
}
