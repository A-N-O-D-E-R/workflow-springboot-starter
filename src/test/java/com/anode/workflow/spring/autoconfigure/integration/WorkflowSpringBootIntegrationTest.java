package com.anode.workflow.spring.autoconfigure.integration;

import com.anode.workflow.WorkflowService;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import com.anode.workflow.spring.autoconfigure.scanner.TaskScanner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Workflow Spring Boot Starter.
 *
 * Tests the full Spring Boot application context with workflow autoconfiguration.
 */
@SpringBootTest(
    classes = TestWorkflowApplication.class,
    properties = {
        "workflow.engines[0].name=test-engine",
        "workflow.engines[0].storage.type=memory",
        "workflow.engines[0].sla.enabled=false",
        "workflow.engines[0].event.enabled=true",
        "workflow.engines[0].factory.enabled=true",
        "workflow.scan-base-package=com.anode.workflow.spring.autoconfigure.integration.testapp"
    }
)
class WorkflowSpringBootIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private FluentWorkflowBuilderFactory workflowBuilderFactory;

    @Autowired
    private Map<String, RuntimeService> runtimeServices;

    @Autowired(required = false)
    private TaskScanner taskScanner;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void workflowServiceBeanIsCreated() {
        assertThat(workflowService).isNotNull();
        assertThat(applicationContext.getBean(WorkflowService.class)).isSameAs(workflowService);
    }

    @Test
    void workflowServiceIsWorkflowSingleton() {
        // WorkflowService should return the same instance from instance()
        assertThat(workflowService).isSameAs(WorkflowService.instance());
    }

    @Test
    void fluentWorkflowBuilderFactoryIsCreated() {
        assertThat(workflowBuilderFactory).isNotNull();
        assertThat(applicationContext.getBean(FluentWorkflowBuilderFactory.class))
            .isSameAs(workflowBuilderFactory);
    }

    @Test
    void runtimeServicesAreCreated() {
        assertThat(runtimeServices).isNotNull();
        assertThat(runtimeServices).isNotEmpty();
        assertThat(runtimeServices).containsKey("test-engine");
    }

    @Test
    void testEngineRuntimeServiceIsConfigured() {
        RuntimeService testEngineRuntime = runtimeServices.get("test-engine");
        assertThat(testEngineRuntime).isNotNull();
    }

    @Test
    void canCreateWorkflowBuilderForTestEngine() {
        var builder = workflowBuilderFactory.builder("test-workflow-001");
        assertThat(builder).isNotNull();

        var engineBuilder = builder.engine("test-engine");
        assertThat(engineBuilder).isNotNull();
    }

    @Test
    void allCoreBeansAreAvailable() {
        // Verify core beans
        assertThat(applicationContext.containsBean("workflowService")).isTrue();
        assertThat(applicationContext.containsBean("runtimeServices")).isTrue();
        assertThat(applicationContext.containsBean("fluentWorkflowBuilderFactory")).isTrue();

        // Verify storage bean
        assertThat(applicationContext.containsBean("memoryCommonService")).isTrue();
    }

    @Test
    void workflowPropertiesAreLoaded() {
        // Verify that the engines were configured from properties
        assertThat(runtimeServices).hasSize(1);
        assertThat(runtimeServices.get("test-engine")).isNotNull();
    }

    @Test
    void taskScannerIsAvailable() {
        // TaskScanner should be available for discovering tasks
        assertThat(taskScanner).isNotNull();
        assertThat(taskScanner.getRegistry()).isNotNull();
    }

    @Test
    void workflowEngineIsAvailableAsBean() {
        // The WorkflowEngine bean should be available
        assertThat(applicationContext.containsBean("workflowEngine")).isTrue();
    }
}
