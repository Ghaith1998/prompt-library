package dev.learn.promptlibrary.tag;

import java.util.List;

import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TagService {

    private final TagRepository tags;

    public TagService(TagRepository tags) {
        this.tags = tags;
    }

    @Transactional
    public List<TagResponse> list() {
        return tags.listAll(Sort.by("name")).stream().map(TagResponse::from).toList();
    }
}
