package ru.job4j.grabber;

import java.util.ArrayList;
import java.util.List;

public class MemStore implements Store {

    private final List<Post> posts = new ArrayList<>();
    private int ids = 1;

    @Override
    public void save(Post post) {
        post.setId(ids++);
        posts.add(post);
    }

    @Override
    public List<Post> getAll() {
        return posts;
    }

    @Override
    public Post findById(int id) {
        return posts.stream()
                .filter((p) -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }
}
