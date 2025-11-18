package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Priority-based engine selection strategy.
 *
 * <p>Selects engines based on request priority:
 * <ul>
 *   <li>Higher priority requests (priority >= 7) → engine-0 (dedicated high-priority)</li>
 *   <li>Medium priority (4-6) → engine-1 or engine-2 (balanced)</li>
 *   <li>Lower priority (0-3) → engine-3 (background processing)</li>
 * </ul>
 */
@Slf4j
@Component("priorityBasedStrategy")
public class PriorityBasedStrategy implements EngineSelectionStrategy {

    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        if (availableEngines.length == 0) {
            throw new IllegalArgumentException("No available engines");
        }

        int priority = request.getPriority();
        String selectedEngine;

        if (priority >= 7) {
            // High priority → first engine
            selectedEngine = availableEngines[0];
            log.debug("[{}] High priority ({}) → {}",
                request.getRequestId(), priority, selectedEngine);
        } else if (priority >= 4) {
            // Medium priority → middle engines (if available)
            int index = availableEngines.length >= 3 ? 1 + (priority % 2) : 0;
            selectedEngine = availableEngines[Math.min(index, availableEngines.length - 1)];
            log.debug("[{}] Medium priority ({}) → {}",
                request.getRequestId(), priority, selectedEngine);
        } else {
            // Low priority → last engine
            selectedEngine = availableEngines[availableEngines.length - 1];
            log.debug("[{}] Low priority ({}) → {}",
                request.getRequestId(), priority, selectedEngine);
        }

        return selectedEngine;
    }

    @Override
    public String getStrategyName() {
        return "PRIORITY_BASED";
    }
}
