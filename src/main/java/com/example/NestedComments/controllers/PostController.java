package com.example.NestedComments.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.example.NestedComments.util.RateLimiter;

@RestController
@RequestMapping("/api/post")
public class PostController {

  private static final int MAX_DEPTH = 10; 
  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final RateLimiter rateLimiter;
  
  public PostController(CommentRepository commentRepository, PostRepository postRepository, RateLimiter rateLimiter) {
    this.commentRepository = commentRepository;
    this.postRepository = postRepository;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/{postId}/comments")
  public List<CommentResponse> getTopComments(
    @PathVariable UUID postId, 
    @RequestParam(defaultValue = "top") String sort, 
    @RequestParam(required = false) UUID cursorId,
    @RequestParam(defaultValue = "20") int limit, 
    @RequestParam(required = false) Integer cursorScore, 
    @RequestParam(required = false) String cursorDate
  ) {
    boolean isNew = switch (sort) {
      case "new" -> true;
      case "top" -> false;
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort type invalid!");
    };

    List<Comment> roots;
if (cursorId == null) {
      roots = isNew
      ? commentRepository.findFirstPageRootCommentsByDate(postId, limit)
      : commentRepository.findFirstPageRootCommentsByScore(postId, limit);
    } else if (isNew) {
      OffsetDateTime cursorTime = OffsetDateTime.parse(cursorDate);
      roots = commentRepository.findRootCommentsAfterCursorByDate(postId, cursorTime, cursorId, limit);
    } else {
      roots = commentRepository.findRootCommentsAfterCursorByScore(postId, cursorScore, cursorId, limit);
    }


    List<UUID> rootIds = roots.stream().map(Comment::getId).toList();

    Map<UUID, Long> replyCounts = commentRepository
      .countRepliesByParentIds(rootIds).stream()
      .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

    // Gets 3 previews
    Map<UUID, List<Comment>> previewsByParent = commentRepository.findTopRepliesForParents(rootIds, 3).stream()
      .collect(Collectors.groupingBy(Comment::getParentId));


    return roots.stream()
      .map(comment -> {
      long replyCount = replyCounts.getOrDefault(comment.getId(), 0L);
      List<Comment> preview = previewsByParent.getOrDefault(comment.getId(), List.of());
      return CommentResponse.from(comment, replyCount, preview);
    }).toList();
  }

  @PostMapping("/{postId}/comments")
  public Comment createComment(
    @PathVariable UUID postId, 
    @RequestBody CreateCommentRequest request
  ) {
    if (!rateLimiter.allow(request.getAuthorId().toString())) {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Slow down — too many comments, try again shortly");
    }

    validateBody(request.getBody());

    postRepository.findById(postId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found!"));
    System.out.println("findById succeeded");

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
    if(parent.isDeleted()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment deleted, cannot reply!");
    }

    if (!parent.getPostId().equals(postId)) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Parent comment does not match post!"
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
