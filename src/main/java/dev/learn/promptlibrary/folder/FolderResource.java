package dev.learn.promptlibrary.folder;

import java.net.URI;
import java.util.List;

import dev.learn.promptlibrary.folder.dto.FolderMoveRequest;
import dev.learn.promptlibrary.folder.dto.FolderRequest;
import dev.learn.promptlibrary.folder.dto.FolderResponse;
import dev.learn.promptlibrary.folder.dto.FolderUpdateRequest;

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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * HTTP boundary for personal folder management. All methods require authentication;
 * owner scoping, default-folder protection, cross-owner rules and cycle detection
 * live in {@link FolderService}.
 */
@Path("/api/folders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Folders", description = "Your personal prompt folders")
public class FolderResource {

    private final FolderService service;

    public FolderResource(FolderService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "List folders",
            description = "Normal users get their own folders, admins get all folders.")
    @APIResponse(responseCode = "200", description = "Folders as a flat list; parentId describes the tree")
    public List<FolderResponse> list() {
        return service.list();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get one folder")
    @APIResponse(responseCode = "200", description = "The folder")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    public FolderResponse get(@RestPath Long id) {
        return service.get(id);
    }

    @POST
    @Operation(summary = "Create a folder",
            description = "The caller becomes the owner; parentId is optional.")
    @APIResponse(responseCode = "201", description = "Created; the Location header points to the new folder")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Parent folder not found")
    @APIResponse(responseCode = "409", description = "Name already exists for this owner")
    public RestResponse<FolderResponse> create(@Valid FolderRequest request) {
        FolderResponse created = service.create(request);
        return RestResponse.ResponseBuilder
                .<FolderResponse>created(URI.create("/api/folders/" + created.id()))
                .entity(created)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Rename a folder")
    @APIResponse(responseCode = "200", description = "Renamed folder")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    @APIResponse(responseCode = "409", description = "Name already exists or the folder is the default folder")
    public FolderResponse rename(@RestPath Long id, @Valid FolderUpdateRequest request) {
        return service.rename(id, request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an empty folder")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", description = "Not found (or not yours)")
    @APIResponse(responseCode = "409", description = "Default folder, non-empty folder, or folder in use by prompts")
    public RestResponse<Void> delete(@RestPath Long id) {
        service.delete(id);
        return RestResponse.noContent();
    }

    @PUT
    @Path("/{id}/move")
    @Operation(summary = "Move a folder",
            description = "parentId null moves the folder to the root.")
    @APIResponse(responseCode = "200", description = "Moved folder")
    @APIResponse(responseCode = "400", description = "Validation failed")
    @APIResponse(responseCode = "404", description = "Folder or parent not found (or not yours)")
    @APIResponse(responseCode = "409", description = "Default folder, cross-owner parent, or cyclic move")
    public FolderResponse move(@RestPath Long id, @Valid FolderMoveRequest request) {
        return service.move(id, request);
    }
}
