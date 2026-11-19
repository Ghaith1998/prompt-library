package dev.learn.promptlibrary.category;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import dev.learn.promptlibrary.category.dto.CategoryRequest;
import dev.learn.promptlibrary.category.dto.CategoryResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Controller layer (called "Resource" in Jakarta REST): HTTP in / HTTP out, no business logic. */
@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Categories", description = "Prompt categories")
public class CategoryResource {

    private final CategoryService service;

    public CategoryResource(CategoryService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "List all categories")
    @APIResponse(responseCode = "200", description = "All categories, sorted by name")
    public List<CategoryResponse> list() {
        return service.list();
    }

    @POST
    @RolesAllowed("admin")
    @Operation(summary = "Create a category", description = "Requires the realm role `admin`.")
    @APIResponse(responseCode = "201", description = "Created")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "403", description = "Caller is not an admin")
    @APIResponse(responseCode = "409", description = "Name already exists")
    public RestResponse<CategoryResponse> create(@Valid CategoryRequest request) {
        CategoryResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<CategoryResponse>created(URI.create("/api/categories/" + created.id()))
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    @Operation(summary = "Update a category", description = "Requires the realm role `admin`.")
    @APIResponse(responseCode = "200", description = "Updated")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "403", description = "Caller is not an admin")
    @APIResponse(responseCode = "404", description = "Category not found")
    @APIResponse(responseCode = "409", description = "Name already exists")
    public CategoryResponse update(@RestPath Long id, @Valid CategoryRequest request) {
        return service.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Operation(summary = "Delete a category", description = "Requires the realm role `admin`.")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "403", description = "Caller is not an admin")
    @APIResponse(responseCode = "404", description = "Category not found")
    @APIResponse(responseCode = "409", description = "Category is in use by prompts")
    public RestResponse<Void> delete(@RestPath Long id) {
        service.delete(id);
        return RestResponse.noContent();
    }
}
