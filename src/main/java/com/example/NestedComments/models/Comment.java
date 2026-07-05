package com.example.NestedComments.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.generator.values.internal.GeneratedValuesImpl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
 
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id; 

  @Column(name = "post_id", nullable = false)
  private UUID postId; 
  
  @Column(name = "parent_id")
  private UUID parentId; 
 
  @Column(name = "author_id", nullable = false)
  private UUID authorId; 

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body; 
  
  @Column(nullable = false)
  @Builder.Default
  private Integer score = 0; 

  @Column(nullable = false)
  @Builder.Default
  private Integer depth = 0; 

  @Column(nullable = false)
  @Builder.Default
  private boolean deleted = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }
}
