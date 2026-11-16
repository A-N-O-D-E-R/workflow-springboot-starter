package com.anode.workflow.spring.autoconfigure.storage.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anode.tool.service.CommonService;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowInfo;
import com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.spring.autoconfigure.runtime.ContextualTask;
import com.anode.workflow.spring.autoconfigure.runtime.WorkflowEngine;
import com.anode.workflow.spring.autoconfigure.storage.JdbcWorkflowStore;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** JDBC storage against a real PostgreSQL: a pended case is resumed from what is in the tables. */
@Testcontainers
@SpringBootTest(
        classes = JdbcStorageIntegrationTest.App.class,
        properties = {
            "workflow.engines[0].name=durable",
            "workflow.engines[0].storage.type=jdbc",
            "workflow.engines[0].storage.jdbc.initialize-schema=true"
        })
class JdbcStorageIntegrationTest {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    /** Test-nested classes are not component-scanned: declare the tasks explicitly. */
    @SpringBootApplication
    static class App {
        @Bean("prepare")
        Prepare prepare() {
            return new Prepare();
        }

        @Bean("approval")
        Approval approval() {
            return new Approval();
        }

        @Bean("perCaseApproval")
        PerCaseApproval perCaseApproval() {
            return new PerCaseApproval();
        }
    }

    /** Pends the first time it runs (waiting for someone), proceeds afterwards. */
    @Task("approval")
    static class Approval implements InvokableTask {
        static final AtomicInteger RUNS = new AtomicInteger();

        @Override
        public TaskResponse executeStep() {
            return RUNS.getAndIncrement() == 0
                    ? new TaskResponse(StepResponseType.OK_PEND, "", "approvers")
                    : new TaskResponse(StepResponseType.OK_PROCEED, "", "");
        }
    }

    /** Pends the first time it runs for a given case, proceeds afterwards: one bean, many cases. */
    @Task("perCaseApproval")
    static class PerCaseApproval implements ContextualTask {
        private final Set<String> seen = ConcurrentHashMap.newKeySet();

        @Override
        public TaskResponse execute(WorkflowContext context) {
            return seen.add(context.getCaseId())
                    ? new TaskResponse(StepResponseType.OK_PEND, "", "approvers")
                    : new TaskResponse(StepResponseType.OK_PROCEED, "", "");
        }
    }

    @Task("prepare")
    static class Prepare implements InvokableTask {
        @Override
        public TaskResponse executeStep() {
            return new TaskResponse(StepResponseType.OK_PROCEED, "", "");
        }
    }

    @Autowired WorkflowEngine engine;
    @Autowired JdbcClient jdbc;
    @Autowired @Qualifier("jdbcCommonService") CommonService storage;

    @Test
    void aPendedCaseIsResumedFromTheDatabase() {
        var pended = engine.startWorkflow("C-1", "durable", List.of("prepare", "approval"),
                Map.<String, Object>of("ticket", "ALE-42", "attempt", 1));

        assertThat(pended.getPendWorkBasket()).isEqualTo("approvers");
        assertThat(jdbc.sql("SELECT doc_key FROM workflow_document ORDER BY doc_key").query(String.class).list())
                .contains("journey_C-1", "worflow_info_C-1");

        engine.resumeWorkflow("C-1", "durable", null);

        WorkflowInfo info = storage.get(WorkflowInfo.class, RuntimeService.WORKFLOW_INFO + RuntimeService.SEP + "C-1");
        assertThat(info.isCaseCompleted()).isTrue();
        assertThat(info.getWorkflowVariables().getString("ticket")).isEqualTo("ALE-42");
        assertThat(info.getWorkflowVariables().getType("attempt")).isEqualTo(WorkflowVariableType.INTEGER);
        assertThat(info.getWorkflowVariables().getInteger("attempt")).isEqualTo(1);
    }

    @Test
    void concurrentCasesDoNotInterfere() throws Exception {
        List<String> ids = IntStream.range(0, 20).mapToObj(i -> "P-" + i).toList();
        try (var pool = Executors.newFixedThreadPool(8)) {
            var starts = ids.stream().map(id -> pool.submit(() ->
                    engine.startWorkflow(id, "durable", List.of("prepare", "perCaseApproval"), Map.<String, Object>of("id", id))))
                    .toList();
            for (var start : starts) {
                assertThat(start.get().getPendWorkBasket()).isEqualTo("approvers");
            }
            var resumes = ids.stream().map(id -> pool.submit(() -> engine.resumeWorkflow(id, "durable", null))).toList();
            for (var resume : resumes) {
                resume.get();
            }
        }
        for (String id : ids) {
            WorkflowInfo info = storage.get(WorkflowInfo.class, RuntimeService.WORKFLOW_INFO + RuntimeService.SEP + id);
            assertThat(info.isCaseCompleted()).as(id).isTrue();
            assertThat(info.getWorkflowVariables().getString("id")).isEqualTo(id);
        }
    }

    @Test
    void countersAreAtomicAndPersistent() {
        long first = storage.incrCounter("c-test");
        assertThat(storage.incrCounter("c-test")).isEqualTo(first + 1);
    }

    @Test
    void tableNamesMustBePlainIdentifiers() {
        assertThatThrownBy(() -> new JdbcWorkflowStore(jdbc, "workflow_document; DROP TABLE x", "workflow_counter"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
