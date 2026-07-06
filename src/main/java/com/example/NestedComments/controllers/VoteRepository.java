package com.example.NestedComments.controllers;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.NestedComments.models.Vote;

public interface VoteRepository extends JpaRepository<Vote, UUID>{
  Optional<Vote> findByCommentIdAndUserId(UUID commentId, UUID userId);

  void deleteByCommentIdAndUserId(UUID commentId, UUID userId); 

  @Query("SELECT COALESCE(SUM(v.value), 0) FROM Vote v WHERE v.commentId = :commentId")
  int sumVotesForComment(@Param("commentId") UUID commentId);
}
