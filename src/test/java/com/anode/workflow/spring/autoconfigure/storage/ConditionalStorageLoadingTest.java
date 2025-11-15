package com.anode.workflow.spring.autoconfigure.storage;

import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that storage beans are only loaded when their storage type is configured.
 */
class ConditionalStorageLoadingTest {

    @Configuration
    @EnableConfigurationProperties(WorkflowEnginesProperties.class)
    static class TestConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldOnlyLoadMemoryStorageWhenConfigured() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=memory"
            )
            .withUserConfiguration(
                MemoryStorageConfiguration.class,
                JpaStorageConfiguration.class,
                FileStorageConfiguration.class
            )
            .run(context -> {
                // Memory storage should be loaded
                assertThat(context).hasSingleBean(MemoryStorageConfiguration.class);
                assertThat(context).hasBean("memoryCommonService");

                // JPA and File storage should NOT be loaded
                assertThat(context).doesNotHaveBean(JpaStorageConfiguration.class);
                assertThat(context).doesNotHaveBean(FileStorageConfiguration.class);
                assertThat(context).doesNotHaveBean("jpaCommonService");
                assertThat(context).doesNotHaveBean("fileCommonService");
            });
    }

    @Test
    void shouldOnlyLoadFileStorageWhenConfigured() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=file"
            )
            .withUserConfiguration(
                MemoryStorageConfiguration.class,
                JpaStorageConfiguration.class,
                FileStorageConfiguration.class
            )
            .run(context -> {
                // File storage should be loaded
                assertThat(context).hasSingleBean(FileStorageConfiguration.class);
                assertThat(context).hasBean("fileCommonService");

                // Memory and JPA storage should NOT be loaded
                assertThat(context).doesNotHaveBean(MemoryStorageConfiguration.class);
                assertThat(context).doesNotHaveBean(JpaStorageConfiguration.class);
                assertThat(context).doesNotHaveBean("memoryCommonService");
                assertThat(context).doesNotHaveBean("jpaCommonService");
            });
    }

    @Test
    void shouldLoadMultipleStorageTypesWhenMultipleEnginesConfigured() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=memory-engine",
                "workflow.engines[0].storage.type=memory",
                "workflow.engines[1].name=file-engine",
                "workflow.engines[1].storage.type=file"
            )
            .withUserConfiguration(
                MemoryStorageConfiguration.class,
                JpaStorageConfiguration.class,
                FileStorageConfiguration.class
            )
            .run(context -> {
                // Both Memory and File storage should be loaded
                assertThat(context).hasSingleBean(MemoryStorageConfiguration.class);
                assertThat(context).hasSingleBean(FileStorageConfiguration.class);
                assertThat(context).hasBean("memoryCommonService");
                assertThat(context).hasBean("fileCommonService");

                // JPA storage should NOT be loaded
                assertThat(context).doesNotHaveBean(JpaStorageConfiguration.class);
                assertThat(context).doesNotHaveBean("jpaCommonService");
            });
    }

    @Test
    void shouldNotLoadAnyStorageWhenNoEnginesConfigured() {
        contextRunner
            .withUserConfiguration(
                MemoryStorageConfiguration.class,
                JpaStorageConfiguration.class,
                FileStorageConfiguration.class
            )
            .run(context -> {
                // No storage should be loaded when no engines are configured
                assertThat(context).doesNotHaveBean(MemoryStorageConfiguration.class);
                assertThat(context).doesNotHaveBean(JpaStorageConfiguration.class);
                assertThat(context).doesNotHaveBean(FileStorageConfiguration.class);
                assertThat(context).doesNotHaveBean("memoryCommonService");
                assertThat(context).doesNotHaveBean("jpaCommonService");
                assertThat(context).doesNotHaveBean("fileCommonService");
            });
    }
}
