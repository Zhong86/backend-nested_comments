package com.example.NestedComments.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.NestedComments.models.Post;

public interface PostRepository extends JpaRepository<Post, UUID>{
}
