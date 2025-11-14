package com.anode.workflow.spring.autoconfigure.properties;

import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WorkflowProperties} configuration binding.
 */
class WorkflowPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @EnableConfigurationProperties(WorkflowProperties.class)
    static class TestConfiguration {
    }

    @Test
    void shouldLoadDefaultProperties() {
        contextRunner.run(context -> {
            WorkflowProperties properties = context.getBean(WorkflowProperties.class);

            assertThat(properties).isNotNull();
            assertThat(properties.getStorage()).isNotNull();
            assertThat(properties.getStorage().getType()).isEqualTo(StorageType.JPA);
            assertThat(properties.getStorage().getFilePath()).isEqualTo("./workflow-data");
            assertThat(properties.getJpa()).isNotNull();
            assertThat(properties.getJpa().isEnabled()).isTrue();
            assertThat(properties.getSla()).isNotNull();
            assertThat(properties.getSla().isEnabled()).isFalse();
            assertThat(properties.getEvent()).isNotNull();
            assertThat(properties.getEvent().isEnabled()).isTrue();
            assertThat(properties.getFactory()).isNotNull();
            assertThat(properties.getFactory().isEnabled()).isTrue();
        });
    }

    @Test
    void shouldBindStorageProperties() {
        contextRunner
                .withPropertyValues(
                        "workflow.storage.type=MEMORY",
                        "workflow.storage.file-path=/custom/path"
                )
                .run(context -> {
                    WorkflowProperties properties = context.getBean(WorkflowProperties.class);

                    assertThat(properties.getStorage().getType()).isEqualTo(StorageType.MEMORY);
                    assertThat(properties.getStorage().getFilePath()).isEqualTo("/custom/path");
                });
    }

    @Test
    void shouldBindJpaProperties() {
        contextRunner
                .withPropertyValues(
                        "workflow.jpa.enabled=false",
                        "workflow.jpa.entity-manager-factory-ref=customEntityManagerFactory",
                        "workflow.jpa.auto-create-schema=true"
                )
                .run(context -> {
                    WorkflowProperties properties = context.getBean(WorkflowProperties.class);

                    assertThat(properties.getJpa().isEnabled()).isFalse();
                    assertThat(properties.getJpa().getEntityManagerFactoryRef())
                            .isEqualTo("customEntityManagerFactory");
                    assertThat(properties.getJpa().isAutoCreateSchema()).isTrue();
                });
    }

    @Test
    void shouldBindSlaProperties() {
        contextRunner
                .withPropertyValues(
                        "workflow.sla.enabled=true",
                        "workflow.sla.queue-manager-bean=customSlaManager"
                )
                .run(context -> {
                    WorkflowProperties properties = context.getBean(WorkflowProperties.class);

                    assertThat(properties.getSla().isEnabled()).isTrue();
                    assertThat(properties.getSla().getQueueManagerBean())
                            .isEqualTo("customSlaManager");
                });
    }

    @Test
    void shouldBindEventHandlerProperties() {
        contextRunner
                .withPropertyValues(
                        "workflow.event.enabled=false",
                        "workflow.event.bean-name=customEventHandler"
                )
                .run(context -> {
                    WorkflowProperties properties = context.getBean(WorkflowProperties.class);

                    assertThat(properties.getEvent().isEnabled()).isFalse();
                    assertThat(properties.getEvent().getBeanName())
                            .isEqualTo("customEventHandler");
                });
    }

    @Test
    void shouldBindFactoryProperties() {
        contextRunner
                .withPropertyValues("workflow.factory.enabled=false")
                .run(context -> {
                    WorkflowProperties properties = context.getBean(WorkflowProperties.class);

                    assertThat(properties.getFactory().isEnabled()).isFalse();
                });
    }

    @Test
    void shouldBindCustomStorageBean() {
        contextRunner
                .withPropertyValues(
                        "workflow.storage.type=CUSTOM",
                        "workflow.storage.custom-bean-name=myCustomStorage"
                )
                .run(context -> {
                    WorkflowProperties properties = context.getBean(WorkflowProperties.class);

                    assertThat(properties.getStorage().getType()).isEqualTo(StorageType.CUSTOM);
                    assertThat(properties.getStorage().getCustomBeanName())
                            .isEqualTo("myCustomStorage");
                });
    }

    @Test
    void shouldBindAllStorageTypes() {
        for (StorageType type : StorageType.values()) {
            contextRunner
                    .withPropertyValues("workflow.storage.type=" + type.name())
                    .run(context -> {
                        WorkflowProperties properties = context.getBean(WorkflowProperties.class);
                        assertThat(properties.getStorage().getType()).isEqualTo(type);
                    });
        }
    }
}
