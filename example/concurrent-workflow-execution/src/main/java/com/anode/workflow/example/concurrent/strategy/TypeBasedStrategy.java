package com.anode.workflow.example.concurrent.strategy;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processing type-based engine selection strategy.
 *
 * <p>Routes requests to specific engines based on their processing type:
 * <ul>
 *   <li>VIDEO_PROCESSING → Dedicated engine for resource-intensive video</li>
 *   <li>IMAGE_PROCESSING → Shared engine for image processing</li>
 *   <li>DOCUMENT_PROCESSING → Shared engine for document processing</li>
 *   <li>DATA_ANALYSIS → Dedicated engine for analytical workloads</li>
 *   <li>REPORT_GENERATION → Dedicated engine for report generation</li>
 * </ul>
 *
 * <p>This allows specialization and optimal resource allocation per workload type.
 */
@Slf4j
@Component("typeBasedStrategy")
public class TypeBasedStrategy implements EngineSelectionStrategy {

    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        if (availableEngines.length == 0) {
            throw new IllegalArgumentException("No available engines");
        }

        // Map processing types to engine indices
        int engineIndex = switch (request.getType()) {
            case VIDEO_PROCESSING -> 0;  // Most resource-intensive
            case IMAGE_PROCESSING -> 1;  // Moderate resources
            case DOCUMENT_PROCESSING -> 2;  // I/O intensive
            case DATA_ANALYSIS -> 3;  // CPU intensive
            case REPORT_GENERATION -> availableEngines.length - 1;  // Background
        };

        // Ensure index is within bounds
        engineIndex = Math.min(engineIndex, availableEngines.length - 1);
        String selectedEngine = availableEngines[engineIndex];

        log.debug("[{}] Type-based selected: {} for type: {}",
            request.getRequestId(), selectedEngine, request.getType());

        return selectedEngine;
    }

    @Override
    public String getStrategyName() {
        return "TYPE_BASED";
    }
}
