package com.example.NestedComments.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.NestedComments.dto.request.VoteRequest;
import com.example.NestedComments.dto.response.CommentResponse;
import com.example.NestedComments.models.Comment;
import com.example.NestedComments.models.Vote;
import com.example.NestedComments.repositories.CommentRepository;
import com.example.NestedComments.util.RateLimiter;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

  private final CommentRepository commentRepository;
  private final VoteRepository voteRepository;
  private final RateLimiter rateLimiter;

  public CommentController(CommentRepository commentRepository, VoteRepository voteRepository, RateLimiter rateLimiter) {
    this.commentRepository = commentRepository;
    this.voteRepository = voteRepository;
    this.rateLimiter = rateLimiter;
  } 

  @GetMapping("/{commentId}/replies")
  public List<CommentResponse> getReplies(
    @PathVariable UUID commentId,
    @RequestParam(defaultValue = "top") String sort,
    @RequestParam(required = false) UUID cursorId,
    @RequestParam(required = false) Integer cursorScore,
    @RequestParam(required = false) String cursorDate, 
    @RequestParam(defaultValue = "20") int limit
  ) {
    commentRepository.findById(commentId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found!"));

    boolean isNew = switch (sort) {
      case "new" -> true;
      case "top" -> false;
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort option invalid!");
    };

    List<Comment> replies;
    if (cursorId == null) {
      replies = isNew
      ? commentRepository.findFirstPageRepliesByDate(commentId, limit)
      : commentRepository.findFirstPageRepliesByScore(commentId, limit);
    } else if (isNew) {
      OffsetDateTime cursorTime = OffsetDateTime.parse(cursorDate);
      replies = commentRepository.findRepliesAfterCursorByDate(commentId, cursorTime, cursorId, limit);
    } else {
      replies = commentRepository.findRepliesAfterCursorByScore(commentId, cursorScore, cursorId, limit);
    }

    List<UUID> replyIds = replies.stream().map(Comment::getId).toList();

    Map<UUID, Long> replyCounts = commentRepository
    .countRepliesByParentIds(replyIds).stream()
    .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

    Map<UUID, List<Comment>> previewsByParent = commentRepository
    .findTopRepliesForParents(replyIds, 1).stream()
    .collect(Collectors.groupingBy(Comment::getParentId));

    return replies.stream()
    .map(comment -> {
      long replyCount = replyCounts.getOrDefault(comment.getId(), 0L);
      List<Comment> preview = previewsByParent.getOrDefault(comment.getId(), List.of());
      return CommentResponse.from(comment, replyCount, preview);
    })
    .toList();
  }

  @PostMapping("/{commentId}/vote")
  @Transactional
  public Comment voteComment(
    @PathVariable UUID commentId,
    @RequestBody VoteRequest request
  ) {
    if (!rateLimiter.allow(request.getUserId().toString())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Slow down — too many comments, try again shortly");
    }
    if(request.getValue() != 1 && request.getValue() != -1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote value must be 1 or -1");
    }

    Comment comment = commentRepository.findById(commentId)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found!"));
    if(comment.isDeleted()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment deleted, cannot vote!");
    }

    Optional<Vote> existingVote = voteRepository.findByCommentIdAndUserId(commentId, request.getUserId());

    if (existingVote.isPresent()) {
      Vote vote = existingVote.get(); 
      if (vote.getValue().equals(request.getValue())) {
        voteRepository.delete(vote);
      } else {
        vote.setValue(request.getValue());
        voteRepository.save(vote);
      }
    } else {
      Vote newVote = Vote.builder()
      .commentId(commentId)
      .userId(request.getUserId())
      .value(request.getValue())
      .build();
      voteRepository.save(newVote);
    }
    int newScore = voteRepository.sumVotesForComment(commentId);
    comment.setScore(newScore);
    return commentRepository.save(comment);
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found!"));
    comment.setDeleted(true);
    commentRepository.save(comment);
    return ResponseEntity.noContent().build();
  }
}
