/**
 * Custom Spring conditional annotations and conditions for workflow configuration.
 *
 * <p>This package provides Spring Boot conditional logic that enables beans to be
 * created only when specific storage types are configured.
 *
 * <h2>Available Conditions</h2>
 * <ul>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.condition.ConditionalOnStorageType} -
 *       Conditional annotation for storage type-based bean creation</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.condition.OnStorageTypeCondition} -
 *       Condition implementation that evaluates storage type configuration</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <p>The conditional logic examines the {@code workflow.engines[*].storage.type} configuration
 * and creates beans only when at least one engine is configured to use the specified storage type.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Configuration
 * @ConditionalOnStorageType(StorageType.JPA)
 * public class JpaStorageConfiguration {
 *     // This configuration is only loaded when at least one
 *     // engine uses JPA storage
 *
 *     @Bean
 *     public CommonService jpaCommonService() {
 *         return new JpaCommonServiceAdapter();
 *     }
 * }
 * }</pre>
 *
 * <h2>Supported Storage Types</h2>
 * <ul>
 *   <li>{@code MEMORY} - In-memory storage (development/testing only)</li>
 *   <li>{@code JPA} - JPA/Hibernate-based persistent storage</li>
 *   <li>{@code FILE} - File-based JSON storage (development/testing only)</li>
 * </ul>
 *
 * @see com.anode.workflow.spring.autoconfigure.storage
 * @see com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.condition;
