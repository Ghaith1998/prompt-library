package dev.learn.promptlibrary.category;

import java.net.URI;
import java.util.List;

import dev.learn.promptlibrary.category.dto.CategoryRequest;
import dev.learn.promptlibrary.category.dto.CategoryResponse;

import io.quarkus.security.Authenticated;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestResponse;

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
                .created(URI.create("/api/categories/" + created.id()))
                .entity(created)
                .build();
    }
}
