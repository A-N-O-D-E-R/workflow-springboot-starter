package com.anode.workflow.spring.autoconfigure.configuration;

import com.anode.tool.service.CommonService;
import com.anode.workflow.WorkflowService;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.runtime.WorkflowRuntimes;
import com.anode.workflow.spring.autoconfigure.properties.StorageProperties.StorageType;
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
                WorkflowRuntimes runtimes = context.getBean(WorkflowRuntimes.class);

                assertThat(runtimes.names()).containsExactly("engine1", "engine2");
                assertThat(runtimes.defaultEngine()).isEqualTo("engine1");
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
                assertThat(context.getBean(WorkflowRuntimes.class).names()).containsExactly("single-engine");
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

                assertThat(context.getBean(WorkflowRuntimes.class).names()).isEmpty();
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
                WorkflowRuntimes runtimes = context.getBean(WorkflowRuntimes.class);

                RuntimeService first = runtimes.runtime("test-engine");
                assertThat(first).isNotNull();
                // a RuntimeService holds per-case state: one per operation
                assertThat(runtimes.runtime("test-engine")).isNotSameAs(first);
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
                WorkflowRuntimes runtimes = context.getBean(WorkflowRuntimes.class);

                RuntimeService first = runtimes.runtime("sla-engine");
                assertThat(first).isNotNull();
                // a RuntimeService holds per-case state: one per operation
                assertThat(runtimes.runtime("sla-engine")).isNotSameAs(first);
            });
    }
}
