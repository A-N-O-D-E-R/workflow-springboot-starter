package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin engine selection strategy.
 *
 * <p>Distributes requests evenly across all available engines
 * in a circular fashion, ensuring balanced load distribution.
 *
 * <p>Thread-safe implementation using AtomicInteger.
 */
@Slf4j
@Component("roundRobinStrategy")
public class RoundRobinStrategy implements EngineSelectionStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        int index = Math.abs(counter.getAndIncrement() % availableEngines.length);
        String selectedEngine = availableEngines[index];

        log.debug("[{}] Round-robin selected: {} (index: {})",
            request.getRequestId(), selectedEngine, index);

        return selectedEngine;
    }

    @Override
    public String getStrategyName() {
        return "ROUND_ROBIN";
    }
}
