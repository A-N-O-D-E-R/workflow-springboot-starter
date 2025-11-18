package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * User affinity-based engine selection strategy.
 *
 * <p>Routes all requests from the same user to the same engine
 * to maintain user-specific state, cache locality, and session affinity.
 *
 * <p>Uses consistent hashing on userId to ensure deterministic routing.
 * Thread-safe and ensures same user always uses same engine.
 */
@Slf4j
@Component("userAffinityStrategy")
public class UserAffinityStrategy implements EngineSelectionStrategy {

    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        // Use consistent hashing based on user ID
        String userId = request.getUserId();
        int hash = Math.abs(userId.hashCode());
        int index = hash % availableEngines.length;
        String selectedEngine = availableEngines[index];

        log.debug("[{}] User affinity selected: {} for user: {} (hash: {})",
            request.getRequestId(), selectedEngine, userId, hash);

        return selectedEngine;
    }

    @Override
    public String getStrategyName() {
        return "USER_AFFINITY";
    }
}
