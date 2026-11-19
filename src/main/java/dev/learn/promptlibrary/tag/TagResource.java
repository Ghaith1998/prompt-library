package dev.learn.promptlibrary.tag;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

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

@Path("/api/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Tags", description = "Your personal tags")
public class TagResource {

    private final TagService service;

    public TagResource(TagService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "List / search tags",
            description = "Normal users get their own tags, admins get all tags.")
    @APIResponse(responseCode = "200", description = "Matching tags, sorted by name")
    public List<TagResponse> list(
            @RestQuery @Parameter(description = "Free-text search in the tag name", example = "java") String q) {
        return service.list(q);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get one tag")
    @APIResponse(responseCode = "200", description = "The tag")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    public TagResponse get(@RestPath @Parameter(description = "Tag id", example = "3") Long id) {
        return service.get(id);
    }

    @POST
    @Operation(summary = "Create a tag", description = "The caller becomes the owner.")
    @APIResponse(responseCode = "201", description = "Created; the Location header points to the new tag")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "409", description = "Name already exists for this owner")
    public RestResponse<TagResponse> create(@Valid TagRequest request) {
        TagResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<TagResponse>created(URI.create("/api/tags/" + created.id()))
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a tag")
    @APIResponse(responseCode = "200", description = "Updated tag")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    @APIResponse(responseCode = "409", description = "Name already exists for this owner")
    public TagResponse update(@RestPath Long id, @Valid TagRequest request) {
        return service.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a tag")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    @APIResponse(responseCode = "409", description = "Tag is in use by prompts")
    public RestResponse<Void> delete(@RestPath Long id) {
        service.delete(id);
        return RestResponse.noContent();
    }
}
