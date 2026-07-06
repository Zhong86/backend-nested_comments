package com.example.NestedComments.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.NestedComments.models.Comment;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentResponse {
  private UUID id; 
  private UUID postId; 
  private UUID parentId; 
  private UUID authorId; 
  private String body; 
  private Integer score; 
  private Integer depth; 
  private OffsetDateTime createdAt;
  private long replyCount; 
  private List<Comment> previewReplies;
  public CommentResponse() {
  } 

  public static CommentResponse from(Comment comment, long replyCount, List<Comment> previewReplies) {
    String displayBody = comment.isDeleted() ? "[deleted]" : comment.getBody();
    return new CommentResponse(
      comment.getId(),
      comment.getPostId(),
      comment.getParentId(),
      comment.getAuthorId(),
      displayBody,
      comment.getScore(),
      comment.getDepth(),
      comment.getCreatedAt(),
      replyCount,
      previewReplies
    );
  }
}
