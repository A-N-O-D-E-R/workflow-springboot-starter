package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Hash-based engine selection strategy.
 *
 * <p>Uses consistent hashing based on request ID to select engines.
 * Ensures that requests with the same ID always route to the same engine,
 * which can be useful for maintaining affinity and cache locality.
 *
 * <p>Thread-safe and deterministic - same request ID always maps to same engine.
 */
@Slf4j
@Component("hashBasedStrategy")
public class HashBasedStrategy implements EngineSelectionStrategy {

    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        // Use consistent hashing based on request ID
        int hash = Math.abs(request.getRequestId().hashCode());
        int index = hash % availableEngines.length;
        String selectedEngine = availableEngines[index];

        log.debug("[{}] Hash-based selected: {} (hash: {}, index: {})",
            request.getRequestId(), selectedEngine, hash, index);

        return selectedEngine;
    }

    @Override
    public String getStrategyName() {
        return "HASH_BASED";
    }
}
