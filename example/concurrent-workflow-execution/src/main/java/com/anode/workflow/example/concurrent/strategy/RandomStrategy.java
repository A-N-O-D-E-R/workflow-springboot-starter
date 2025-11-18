package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random engine selection strategy.
 *
 * <p>Randomly distributes requests across all available engines.
 * Uses ThreadLocalRandom for better performance in concurrent scenarios.
 *
 * <p>Useful for simple load balancing without any affinity requirements.
 */
@Slf4j
@Component("randomStrategy")
public class RandomStrategy implements EngineSelectionStrategy {

    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        int index = ThreadLocalRandom.current().nextInt(availableEngines.length);
        String selectedEngine = availableEngines[index];

        log.debug("[{}] Random selected: {} (index: {})",
            request.getRequestId(), selectedEngine, index);

        return selectedEngine;
    }

    @Override
    public String getStrategyName() {
        return "RANDOM";
    }
}
