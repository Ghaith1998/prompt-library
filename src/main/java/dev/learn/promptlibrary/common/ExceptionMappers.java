package dev.learn.promptlibrary.common;

import jakarta.ws.rs.core.Response.Status;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Central translation from domain exceptions to the API's uniform {@link ApiError}
 * shape. HTTP status is chosen here, keeping services free of transport concerns.
 * Bean Validation errors are already turned into HTTP 400 by Quarkus.
 */
public class ExceptionMappers {

    @ServerExceptionMapper
    public RestResponse<ApiError> notFound(ResourceNotFoundException e) {
        return RestResponse.status(Status.NOT_FOUND, new ApiError("NOT_FOUND", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> conflict(ConflictException e) {
        return RestResponse.status(Status.CONFLICT, new ApiError("CONFLICT", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> invalidCredentials(InvalidCredentialsException e) {
        return RestResponse.status(Status.UNAUTHORIZED, new ApiError("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> upstream(UpstreamException e) {
        return RestResponse.status(Status.BAD_GATEWAY, new ApiError("KEYCLOAK_ERROR", e.getMessage()));
    }
}
