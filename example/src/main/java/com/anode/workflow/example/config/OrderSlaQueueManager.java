package com.anode.workflow.example.config;

import com.anode.workflow.entities.sla.Milestone;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.SlaQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SLA Queue Manager for tracking order processing deadlines
 */
@Component
public class OrderSlaQueueManager implements SlaQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(OrderSlaQueueManager.class);

    // In-memory storage for demo purposes
    // In production, use database or distributed cache
    private final Map<String, List<Milestone>> milestonesByCase = new ConcurrentHashMap<>();

    @Override
    public void enqueue(WorkflowContext context, List<Milestone> milestones) {
        String caseId = context.getCaseId();
        logger.info("Enqueuing {} SLA milestones for case: {}", milestones.size(), caseId);

        milestonesByCase.computeIfAbsent(caseId, k -> new CopyOnWriteArrayList<>())
                .addAll(milestones);

        // Log each milestone
        for (Milestone milestone : milestones) {
            logger.info("SLA Milestone - Case: {}, Name: {}, AppliedAtAge: {}, AppliedAtTimestamp: {}",
                    caseId, milestone.getName(), milestone.getAppliedAtAge(), milestone.getAppliedAtTimestamp());
        }

        // In production, you might:
        // - Schedule actual jobs/timers
        // - Store in database
        // - Publish to scheduling service
        // - Set up alerts
    }

    @Override
    public void dequeue(WorkflowContext context, String milestoneName) {
        String caseId = context.getCaseId();
        logger.info("Dequeuing SLA milestone '{}' for case: {}", milestoneName, caseId);

        List<Milestone> milestones = milestonesByCase.get(caseId);
        if (milestones != null) {
            milestones.removeIf(m -> m.getName().equals(milestoneName));
            logger.debug("Removed milestone '{}' for case: {}", milestoneName, caseId);
        }
    }

    @Override
    public void dequeueAll(WorkflowContext context) {
        String caseId = context.getCaseId();
        logger.info("Dequeuing all SLA milestones for case: {}", caseId);

        List<Milestone> removed = milestonesByCase.remove(caseId);
        if (removed != null) {
            logger.debug("Removed {} milestones for case: {}", removed.size(), caseId);
        }
    }

    /**
     * Get all milestones for a case (for monitoring/debugging)
     */
    public List<Milestone> getMilestones(String caseId) {
        return milestonesByCase.getOrDefault(caseId, List.of());
    }
}
