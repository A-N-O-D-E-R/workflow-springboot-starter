package com.anode.workflow.example.config;

import com.anode.workflow.entities.events.EventType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom event handler for auditing workflow events
 */
@Component
public class AuditEventHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventHandler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void invoke(EventType event, WorkflowContext context) {
        String timestamp = LocalDateTime.now().format(formatter);
        String caseId = context.getCaseId();

        // Log the event
        logger.info("[AUDIT] {} - Case: {} - Event: {} - Variables: {}",
                timestamp, caseId, event, context.getProcessVariables().getListOfWorkflowVariable().size());

        // In a real application, you might:
        // - Store to database
        // - Send to message queue
        // - Publish to event stream
        // - Send notifications
        // - Update metrics
    }
}
