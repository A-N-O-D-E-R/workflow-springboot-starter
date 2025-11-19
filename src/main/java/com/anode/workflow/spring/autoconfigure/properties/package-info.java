/**
 * Configuration properties classes for workflow engine setup.
 *
 * <p>This package contains Spring Boot configuration properties classes that
 * bind to {@code workflow.*} configuration in {@code application.yml} or
 * {@code application.properties}.
 *
 * <h2>Main Properties Classes</h2>
 * <ul>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties} -
 *       Top-level configuration for all workflow engines</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.properties.EngineProperties} -
 *       Configuration for a single workflow engine</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.properties.JpaProperties} -
 *       Storage configuration properties</li>
 * </ul>
 *
 * <h2>Configuration Structure</h2>
 * <pre>{@code
 * workflow:
 *   engines:                          # List of workflow engines
 *     - name: main-engine             # Engine name (required, unique)
 *       storage:
 *         type: jpa                   # Storage type: memory, jpa, file
 *         file-path: ./workflow-data  # Only for file storage
 *       scan-packages:                # Packages to scan for @Task
 *         - com.example.tasks
 *         - com.example.workflows
 *     - name: secondary-engine
 *       storage:
 *         type: memory
 * }</pre>
 *
 * <h2>Storage Types</h2>
 * <p>Available storage types defined in {@code StorageType} enum:
 * <ul>
 *   <li><b>MEMORY:</b> In-memory storage (volatile, for testing/development)</li>
 *   <li><b>JPA:</b> JPA/Hibernate-based persistent storage (production-ready)</li>
 *   <li><b>FILE:</b> File-based JSON storage (for testing/development)</li>
 * </ul>
 *
 * <h2>Property Binding</h2>
 * <p>Properties are automatically bound by Spring Boot's
 * {@code @ConfigurationProperties} mechanism. Validation is performed
 * during binding to ensure required properties are present.
 *
 * <h2>Example Configurations</h2>
 *
 * <h3>Single Engine with Memory Storage</h3>
 * <pre>{@code
 * workflow:
 *   engines:
 *     - name: test-engine
 *       storage:
 *         type: memory
 * }</pre>
 *
 * <h3>Multiple Engines with Different Storage</h3>
 * <pre>{@code
 * workflow:
 *   engines:
 *     - name: production-engine
 *       storage:
 *         type: jpa
 *       scan-packages:
 *         - com.example.production.tasks
 *     - name: test-engine
 *       storage:
 *         type: memory
 *       scan-packages:
 *         - com.example.test.tasks
 * }</pre>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.properties;
