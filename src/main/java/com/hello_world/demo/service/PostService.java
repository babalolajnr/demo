package com.hello_world.demo.service;

import java.util.List;

import com.hello_world.demo.model.entity.Post;

interface PostService {

    List<Post> findAll();

    Post findById(Long id);

    Post save(Post post);

    void deleteById(Long id);

    List<Post> findByTitleContaining(String title);
}