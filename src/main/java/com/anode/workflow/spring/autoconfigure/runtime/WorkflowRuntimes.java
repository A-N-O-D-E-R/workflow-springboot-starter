package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.service.runtime.RuntimeService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedSet;
import java.util.function.Supplier;

/**
 * The configured engines. A {@link RuntimeService} carries the state of the case it is operating
 * on, so {@link #runtime} hands out a new, cheap instance for every operation: concurrent
 * operations on different cases never share one.
 */
public final class WorkflowRuntimes {

    private final Map<String, Supplier<RuntimeService>> engines;

    /** @param engines engine name to RuntimeService factory, the first entry being the default */
    public WorkflowRuntimes(Map<String, Supplier<RuntimeService>> engines) {
        this.engines = Collections.unmodifiableMap(new LinkedHashMap<>(engines));
    }

    /** Engine names, the first one being the default. */
    public SequencedSet<String> names() {
        return Collections.unmodifiableSequencedSet(new java.util.LinkedHashSet<>(engines.keySet()));
    }

    /** Name of the default (first configured) engine. */
    public String defaultEngine() {
        return engines.keySet().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No workflow engine configured (workflow.engines is empty)"));
    }

    /** A new RuntimeService for one operation on the named engine. */
    public RuntimeService runtime(String engineName) {
        Supplier<RuntimeService> engine = engines.get(engineName);
        if (engine == null) {
            throw new IllegalArgumentException(
                    "No workflow engine named '" + engineName + "'. Available engines: " + engines.keySet());
        }
        return engine.get();
    }
}
