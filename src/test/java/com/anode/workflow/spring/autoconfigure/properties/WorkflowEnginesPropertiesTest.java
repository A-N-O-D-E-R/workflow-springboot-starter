package com.anode.workflow.spring.autoconfigure.properties;

import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WorkflowEnginesProperties} configuration binding.
 */
class WorkflowEnginesPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @EnableConfigurationProperties(WorkflowEnginesProperties.class)
    static class TestConfiguration {
    }

    @Test
    void shouldLoadEmptyEnginesList() {
        contextRunner.run(context -> {
            WorkflowEnginesProperties properties = context.getBean(WorkflowEnginesProperties.class);

            assertThat(properties).isNotNull();
            assertThat(properties.getEngines()).isEmpty();
        });
    }

    @Test
    void shouldBindSingleEngine() {
        contextRunner
                .withPropertyValues(
                        "workflow.engines[0].name=engine1",
                        "workflow.engines[0].storage.type=MEMORY"
                )
                .run(context -> {
                    WorkflowEnginesProperties properties = context.getBean(WorkflowEnginesProperties.class);

                    assertThat(properties.getEngines()).hasSize(1);
                    WorkflowEnginesProperties.EngineConfig engine = properties.getEngines().get(0);
                    assertThat(engine.getName()).isEqualTo("engine1");
                    assertThat(engine.getStorage().getType()).isEqualTo(StorageType.MEMORY);
                });
    }

    @Test
    void shouldBindMultipleEngines() {
        contextRunner
                .withPropertyValues(
                        "workflow.engines[0].name=engine1",
                        "workflow.engines[0].storage.type=JPA",
                        "workflow.engines[0].sla.enabled=true",
                        "workflow.engines[1].name=engine2",
                        "workflow.engines[1].storage.type=MEMORY",
                        "workflow.engines[1].sla.enabled=false"
                )
                .run(context -> {
                    WorkflowEnginesProperties properties = context.getBean(WorkflowEnginesProperties.class);

                    assertThat(properties.getEngines()).hasSize(2);

                    WorkflowEnginesProperties.EngineConfig engine1 = properties.getEngines().get(0);
                    assertThat(engine1.getName()).isEqualTo("engine1");
                    assertThat(engine1.getStorage().getType()).isEqualTo(StorageType.JPA);
                    assertThat(engine1.getSla().isEnabled()).isTrue();

                    WorkflowEnginesProperties.EngineConfig engine2 = properties.getEngines().get(1);
                    assertThat(engine2.getName()).isEqualTo("engine2");
                    assertThat(engine2.getStorage().getType()).isEqualTo(StorageType.MEMORY);
                    assertThat(engine2.getSla().isEnabled()).isFalse();
                });
    }

    @Test
    void shouldBindEngineWithAllProperties() {
        contextRunner
                .withPropertyValues(
                        "workflow.engines[0].name=fullEngine",
                        "workflow.engines[0].storage.type=FILE",
                        "workflow.engines[0].storage.file-path=/custom/workflow",
                        "workflow.engines[0].jpa.enabled=false",
                        "workflow.engines[0].jpa.entity-manager-factory-ref=customEMF",
                        "workflow.engines[0].jpa.auto-create-schema=true",
                        "workflow.engines[0].sla.enabled=true",
                        "workflow.engines[0].sla.queue-manager-bean=customSlaManager"
                )
                .run(context -> {
                    WorkflowEnginesProperties properties = context.getBean(WorkflowEnginesProperties.class);

                    assertThat(properties.getEngines()).hasSize(1);
                    WorkflowEnginesProperties.EngineConfig engine = properties.getEngines().get(0);

                    assertThat(engine.getName()).isEqualTo("fullEngine");
                    assertThat(engine.getStorage().getType()).isEqualTo(StorageType.FILE);
                    assertThat(engine.getStorage().getFilePath()).isEqualTo("/custom/workflow");
                    assertThat(engine.getJpa().isEnabled()).isFalse();
                    assertThat(engine.getJpa().getEntityManagerFactoryRef()).isEqualTo("customEMF");
                    assertThat(engine.getJpa().isAutoCreateSchema()).isTrue();
                    assertThat(engine.getSla().isEnabled()).isTrue();
                    assertThat(engine.getSla().getQueueManagerBean()).isEqualTo("customSlaManager");
                });
    }

    @Test
    void shouldUseDefaultValuesForEngine() {
        contextRunner
                .withPropertyValues("workflow.engines[0].name=defaultEngine")
                .run(context -> {
                    WorkflowEnginesProperties properties = context.getBean(WorkflowEnginesProperties.class);

                    assertThat(properties.getEngines()).hasSize(1);
                    WorkflowEnginesProperties.EngineConfig engine = properties.getEngines().get(0);

                    assertThat(engine.getName()).isEqualTo("defaultEngine");
                    assertThat(engine.getStorage()).isNotNull();
                    assertThat(engine.getStorage().getType()).isEqualTo(StorageType.JPA);
                    assertThat(engine.getJpa()).isNotNull();
                    assertThat(engine.getJpa().isEnabled()).isTrue();
                    assertThat(engine.getSla()).isNotNull();
                    assertThat(engine.getSla().isEnabled()).isFalse();
                });
    }
}
