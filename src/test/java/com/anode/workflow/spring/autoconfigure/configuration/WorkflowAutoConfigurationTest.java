package com.anode.workflow.spring.autoconfigure.configuration;

import com.anode.tool.service.CommonService;
import com.anode.workflow.WorkflowService;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WorkflowAutoConfiguration.
 */
class WorkflowAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldCreateWorkflowServiceBean() {
        contextRunner
            .withUserConfiguration(WorkflowAutoConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(WorkflowService.class);
                WorkflowService service = context.getBean(WorkflowService.class);
                assertThat(service).isNotNull();
            });
    }

    @Test
    void shouldCreateRuntimeServicesForConfiguredEngines() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=engine1",
                "workflow.engines[0].storage.type=memory",
                "workflow.engines[1].name=engine2",
                "workflow.engines[1].storage.type=memory"
            )
            .withUserConfiguration(
                WorkflowAutoConfiguration.class,
                MemoryStorageConfiguration.class
            )
            .run(context -> {
                assertThat(context).hasBean("runtimeServices");

                @SuppressWarnings("unchecked")
                Map<String, RuntimeService> services = context.getBean("runtimeServices", Map.class);

                assertThat(services).hasSize(2);
                assertThat(services).containsKeys("engine1", "engine2");
            });
    }

    @Test
    void shouldCreateSingleRuntimeService() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=single-engine",
                "workflow.engines[0].storage.type=memory"
            )
            .withUserConfiguration(
                WorkflowAutoConfiguration.class,
                MemoryStorageConfiguration.class
            )
            .run(context -> {
                @SuppressWarnings("unchecked")
                Map<String, RuntimeService> services = context.getBean("runtimeServices", Map.class);

                assertThat(services).hasSize(1);
                assertThat(services).containsKey("single-engine");
            });
    }

    @Test
    void shouldUseMemoryStorageWhenConfigured() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=memory-engine",
                "workflow.engines[0].storage.type=memory"
            )
            .withUserConfiguration(
                WorkflowAutoConfiguration.class,
                MemoryStorageConfiguration.class
            )
            .run(context -> {
                assertThat(context).hasBean("memoryCommonService");
                CommonService storage = context.getBean("memoryCommonService", CommonService.class);
                assertThat(storage).isNotNull();
            });
    }

    @Test
    void shouldHandleEmptyEnginesConfiguration() {
        contextRunner
            .withUserConfiguration(WorkflowAutoConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(WorkflowService.class);

                @SuppressWarnings("unchecked")
                Map<String, RuntimeService> services = context.getBean("runtimeServices", Map.class);

                assertThat(services).isEmpty();
            });
    }

    @Test
    void shouldCreateRuntimeServiceWithDefaultComponents() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=test-engine",
                "workflow.engines[0].storage.type=memory",
                "workflow.engines[0].sla.enabled=false"
            )
            .withUserConfiguration(
                WorkflowAutoConfiguration.class,
                MemoryStorageConfiguration.class
            )
            .run(context -> {
                @SuppressWarnings("unchecked")
                Map<String, RuntimeService> services = context.getBean("runtimeServices", Map.class);

                RuntimeService runtimeService = services.get("test-engine");
                assertThat(runtimeService).isNotNull();
            });
    }

    @Test
    void shouldCreateRuntimeServiceWithSlaEnabled() {
        contextRunner
            .withPropertyValues(
                "workflow.engines[0].name=sla-engine",
                "workflow.engines[0].storage.type=memory",
                "workflow.engines[0].sla.enabled=true"
            )
            .withUserConfiguration(
                WorkflowAutoConfiguration.class,
                MemoryStorageConfiguration.class
            )
            .run(context -> {
                @SuppressWarnings("unchecked")
                Map<String, RuntimeService> services = context.getBean("runtimeServices", Map.class);

                RuntimeService runtimeService = services.get("sla-engine");
                assertThat(runtimeService).isNotNull();
            });
    }
}
