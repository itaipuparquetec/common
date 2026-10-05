package br.org.itaipuparquetec.common.infrastructure.audit.trail.context;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Propagates the MDC (audit context identifiers) to the worker threads spawned by a use case. Register it in
 * the {@code TaskExecutor} used for asynchronous work so the {@code traceId} and actor follow the execution.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(final Runnable runnable) {
        final Map<String, String> parentContext = MDC.getCopyOfContextMap();
        return () -> {
            final Map<String, String> previousContext = MDC.getCopyOfContextMap();
            replaceContext(parentContext);
            try {
                runnable.run();
            } finally {
                replaceContext(previousContext);
            }
        };
    }

    private static void replaceContext(final Map<String, String> context) {
        if (context == null) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(context);
    }
}
