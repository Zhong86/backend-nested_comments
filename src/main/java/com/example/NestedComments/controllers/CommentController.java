package com.example.NestedComments.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.NestedComments.dto.response.CommentResponse;
import com.example.NestedComments.models.Comment;
import com.example.NestedComments.repositories.CommentRepository;

import lombok.Getter;

@RestController
@RequestMapping("/comments")
public class CommentController {

  private final CommentRepository commentRepository;

  public CommentController(CommentRepository commentRepository) {
    this.commentRepository = commentRepository;
  } 

  @GetMapping("/{commentId}/replies")
  public Page<CommentResponse> getReplies(
    @PathVariable UUID commentId,
    @RequestParam(defaultValue = "top") String sort,
    @RequestParam(defaultValue = "20") int limit, 
    @RequestParam(defaultValue = "0") int offset
  ) {
    Sort sortOrder = switch (sort) {
      case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
      case "top" -> Sort.by(Sort.Direction.DESC, "score");
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort option invalid!");
    };

    Pageable pageable = PageRequest.of(offset / limit, limit, sortOrder);

    Page<Comment> roots = commentRepository.findByParentId(commentId, pageable);
    List<UUID> rootIds = roots.getContent().stream().map(Comment::getId).toList();

    Map<UUID, Long> replyCounts = commentRepository
      .countRepliesByParentIds(rootIds).stream()
      .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

    // Get 1 preview reply
    Map<UUID, List<Comment>> previewsByParent = commentRepository
      .findTopRepliesForParents(rootIds, 1).stream()
      .collect(Collectors.groupingBy(Comment::getParentId));

    return roots.map(comment -> {
      long replyCount = replyCounts.getOrDefault(comment.getId(), 0L);
      List<Comment> preview = previewsByParent.getOrDefault(comment.getId(), List.of());
      return CommentResponse.from(comment, replyCount, preview);
    });
  }
}
