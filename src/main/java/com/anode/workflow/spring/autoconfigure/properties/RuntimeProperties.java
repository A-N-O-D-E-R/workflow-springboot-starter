package com.anode.workflow.spring.autoconfigure.properties;

import java.time.Duration;
import lombok.Data;

/** Process-wide settings of the workflow engine (shared by all engines). */
@Data
public class RuntimeProperties {

    /** Threads running the branches of parallel routes; 0 runs them on the calling thread. */
    private int threads = 10;

    /** Idle time before a branch thread above the core size is released. */
    private Duration idleTimeout = Duration.ofSeconds(30);

    /** Separator in persistence keys (journey_&lt;case&gt;, worflow_info_&lt;case&gt;...). */
    private String keySeparator = "_";

    /** Work basket a case pends in when a step throws. */
    private String errorWorkbasket = "workflow_error";
}
