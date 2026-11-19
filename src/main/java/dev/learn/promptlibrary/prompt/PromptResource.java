package dev.learn.promptlibrary.prompt;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import dev.learn.promptlibrary.prompt.dto.PromptMoveRequest;
import dev.learn.promptlibrary.prompt.dto.PromptRequest;
import dev.learn.promptlibrary.prompt.dto.PromptResponse;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/prompts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Prompts", description = "Your personal prompt library")
public class PromptResource {

    private final PromptService service;

    public PromptResource(PromptService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "List / search prompts",
            description = "Normal users get their own prompts, admins get all prompts.")
    @APIResponse(responseCode = "200", description = "Matching prompts, newest first")
    public List<PromptResponse> list(
            @RestQuery @Parameter(description = "Free-text search in title and content", example = "review") String q) {
        return service.list(q);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get one prompt")
    @APIResponse(responseCode = "200", description = "The prompt")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    public PromptResponse get(@RestPath @Parameter(description = "Prompt id", example = "42") Long id) {
        return service.get(id);
    }

    @POST
    @Operation(summary = "Create a prompt", description = "The caller becomes the owner.")
    @APIResponse(responseCode = "201", description = "Created; the Location header points to the new prompt")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Category not found")
    public RestResponse<PromptResponse> create(@Valid PromptRequest request) {
        PromptResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<PromptResponse>created(URI.create("/api/prompts/" + created.id()))
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Replace a prompt")
    @APIResponse(responseCode = "200", description = "Updated prompt")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Prompt or category not found")
    public PromptResponse update(@RestPath Long id, @Valid PromptRequest request) {
        return service.update(id, request);
    }

    @PUT
    @Path("/{id}/move")
    @Operation(summary = "Move a prompt to another folder")
    @APIResponse(responseCode = "200", description = "Prompt moved")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Prompt or folder not found (or not yours)")
    public PromptResponse move(@RestPath Long id, @Valid PromptMoveRequest request) {
        return service.moveToFolder(id, request.folderId());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a prompt")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    public RestResponse<Void> delete(@RestPath Long id) {
        service.delete(id);
        return RestResponse.noContent();
    }
}
