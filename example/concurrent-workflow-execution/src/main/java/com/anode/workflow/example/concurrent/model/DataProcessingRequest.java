package com.anode.workflow.example.concurrent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a data processing request that will be processed concurrently.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataProcessingRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String userId;
    private ProcessingType type;
    private List<String> dataItems;
    private int priority;
    private long submittedAt;

    public enum ProcessingType {
        IMAGE_PROCESSING,
        VIDEO_PROCESSING,
        DOCUMENT_PROCESSING,
        DATA_ANALYSIS,
        REPORT_GENERATION
    }
}
