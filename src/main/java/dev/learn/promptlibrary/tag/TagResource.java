package dev.learn.promptlibrary.tag;

import java.util.List;

import io.quarkus.security.Authenticated;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/api/tags")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Tags", description = "Tags are created implicitly when a prompt uses them")
public class TagResource {

    private final TagService service;

    public TagResource(TagService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "List all tags")
    public List<TagResponse> list() {
        return service.list();
    }
}
