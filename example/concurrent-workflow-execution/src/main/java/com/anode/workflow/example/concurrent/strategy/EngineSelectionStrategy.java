package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;

/**
 * Strategy interface for selecting workflow engines.
 *
 * <p>Different strategies can be implemented to distribute workflow
 * executions across multiple engines based on various criteria.
 */
public interface EngineSelectionStrategy {

    /**
     * Select an engine name for the given request.
     *
     * @param request The data processing request
     * @param availableEngines Array of available engine names
     * @return The selected engine name
     */
    String selectEngine(DataProcessingRequest request, String[] availableEngines);

    /**
     * Get the strategy name for logging and configuration.
     *
     * @return The strategy name
     */
    String getStrategyName();
}
