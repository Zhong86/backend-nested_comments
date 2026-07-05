package com.example.NestedComments.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.NestedComments.dto.request.CreateCommentRequest;
import com.example.NestedComments.dto.response.CommentResponse;
import com.example.NestedComments.models.Comment;
import com.example.NestedComments.repositories.CommentRepository;
import com.example.NestedComments.repositories.PostRepository;

@RestController
@RequestMapping("/api/post")
public class PostController {

  private static final int MAX_DEPTH = 10; 
  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  
  public PostController(CommentRepository commentRepository) {
    this.commentRepository = commentRepository;
    this.postRepository = null;
  }

  @GetMapping("/{postId}/comments")
  public Page<CommentResponse> getTopComments(
    @PathVariable UUID postId, 
    @RequestParam(defaultValue = "top") String sort, 
    @RequestParam(defaultValue = "20") int limit, 
    @RequestParam(defaultValue = "0") int offset
  ) {
    Sort sortOrder = switch (sort) {
      case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
      case "top" -> Sort.by(Sort.Direction.DESC, "score");
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort type invalid!");
    };

    Pageable pageable = PageRequest.of(offset / limit, limit, sortOrder);

    Page<Comment> roots =  commentRepository.findByPostIdAndParentIdIsNull(postId, pageable);
    List<UUID> rootIds = roots.getContent().stream().map(Comment::getId).toList();

    Map<UUID, Long> replyCounts = commentRepository
      .countRepliesByParentIds(rootIds).stream()
      .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

    // Gets 3 previews
    Map<UUID, List<Comment>> previewsByParent = commentRepository.findTopRepliesForParents(rootIds, 3).stream()
      .collect(Collectors.groupingBy(Comment::getParentId));


    return roots.map(comment -> {
      long replyCount = replyCounts.getOrDefault(comment.getId(), 0L);
      List<Comment> preview = previewsByParent.getOrDefault(comment.getId(), List.of());

      return CommentResponse.from(comment, replyCount, preview);
    });
  }

  @PostMapping("/{postId}/comments")
  public Comment createComment(
    @PathVariable UUID postId, 
    @RequestBody CreateCommentRequest request
  ) {
    validateBody(request.getBody());

    postRepository.findById(postId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found!"));

    Comment newComment = Comment.builder()
      .postId(postId)
      .authorId(request.getAuthorId())
      .body(request.getBody())
      .depth(0)
      .build();

    return commentRepository.save(newComment);
  }

  @PostMapping("/{postId}/comments/{commentId}/replies")
  public Comment createReply(
    @PathVariable UUID postId,
    @PathVariable UUID commentId, 
    @RequestBody CreateCommentRequest request
  ) {
    validateBody(request.getBody());

    Comment parent = commentRepository.findById(commentId) 
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));

    if (!parent.getPostId().equals(postId)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Parent comment does not match post!"
      );
    }

    if (parent.getDepth() >= MAX_DEPTH) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Max nesting depth reached!"
      );
    }

    Comment newReply = Comment.builder()
      .postId(parent.getPostId())
      .parentId(commentId)
      .authorId(request.getAuthorId())
      .body(request.getBody())
      .depth(parent.getDepth() + 1)
      .build();

    return commentRepository.save(newReply);
  }

  private void validateBody(String body) {
    if (body == null | body.isBlank()) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Comment body cannot be empty!"
      );
    }
  }
}
