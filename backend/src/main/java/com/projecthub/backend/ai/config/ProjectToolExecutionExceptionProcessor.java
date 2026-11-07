
package com.projecthub.backend.ai.config;

import com.projecthub.backend.exception.ProjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Component
public class ProjectToolExecutionExceptionProcessor
        implements ToolExecutionExceptionProcessor {

    private final ObjectMapper objectMapper;

    public ProjectToolExecutionExceptionProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String process(ToolExecutionException exception) {

        String toolName = exception.getToolDefinition() != null
                ? exception.getToolDefinition().name()
                : "unknown";

        Map<String, Object> errorResponse = new LinkedHashMap<>();

        errorResponse.put("success", false);

        // 1. Check whether the requested project exists
        ProjectNotFoundException projectException =
                findCause(exception, ProjectNotFoundException.class);

        if (projectException != null) {

            errorResponse.put("errorCode", "PROJECT_UNAVAILABLE");

            errorResponse.put(
                    "message",
                    "The requested project does not exist or is not accessible " +
                            "to the currently authenticated user."
            );

            log.warn(
                    "Tool '{}' failed because the requested project was unavailable: {}",
                    toolName,
                    projectException.getMessage()
            );
        }

        // 2. Check whether the user lacks permission
        else if (isAccessDenied(exception)) {

            errorResponse.put("errorCode", "ACCESS_DENIED");

            errorResponse.put(
                    "message",
                    "You do not have permission to perform this operation. " +
                            "Do not assume that the operation succeeded."
            );

            log.warn(
                    "Access denied while executing tool '{}'",
                    toolName
            );
        }

        // 3. Handle all other unexpected tool failures
        else {

            errorResponse.put("errorCode", "TOOL_EXECUTION_FAILED");

            errorResponse.put(
                    "message",
                    "The requested operation could not be completed due to " +
                            "an internal tool error. Do not assume that the operation " +
                            "succeeded. Please inform the user that the information " +
                            "is currently unavailable."
            );

            log.error(
                    "Unexpected failure while executing tool '{}'",
                    toolName,
                    exception
            );
        }

        // 4. Serialize the error response into valid JSON
        try {
            return objectMapper.writeValueAsString(errorResponse);
        }
        catch (Exception serializationException) {

            log.error(
                    "Failed to serialize error response for tool '{}'",
                    toolName,
                    serializationException
            );

            // Static valid JSON fallback
            return """
                    {
                        "success": false,
                        "errorCode": "TOOL_EXECUTION_FAILED",
                        "message": "The requested operation could not be completed."
                    }
                    """;
        }
    }

    /**
     * Searches the complete exception cause chain for a particular exception type.
     */
    private <T extends Throwable> T findCause(
            Throwable exception,
            Class<T> expectedType
    ) {

        Set<Throwable> visited =
                Collections.newSetFromMap(new IdentityHashMap<>());

        Throwable current = exception;

        while (current != null && visited.add(current)) {

            if (expectedType.isInstance(current)) {
                return expectedType.cast(current);
            }

            current = current.getCause();
        }

        return null;
    }

    /**
     * Checks both Spring Security exceptions and HTTP 403 exceptions.
     */
    private boolean isAccessDenied(Throwable exception) {

        if (findCause(exception, AccessDeniedException.class) != null) {
            return true;
        }

        ResponseStatusException responseStatusException =
                findCause(exception, ResponseStatusException.class);

        return responseStatusException != null
                && responseStatusException.getStatusCode().value()
                == HttpStatus.FORBIDDEN.value();
    }
}
