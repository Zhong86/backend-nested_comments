package com.example.NestedComments.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.NestedComments.models.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>{

  // Get comments based on Post
  @Query(value = """
    SELECT * FROM comments
    WHERE post_id = :postId AND parent_id IS NULL
    ORDER BY score DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findFirstPageRootCommentsByScore(
    @Param("postId") UUID postId,  
    @Param("limit") int limit
  );

  @Query(value = """
    SELECT * FROM comments
    WHERE post_id = :postId AND parent_id IS NULL
    AND (score, id) < (:cursorScore, :cursorId)
    ORDER BY score DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findRootCommentsAfterCursorByScore(
    @Param("postId") UUID postId, 
    @Param("cursorScore") int cursorScore, 
    @Param("cursorId") UUID cursorId, 
    @Param("limit") int limit
  );

  @Query(value = """
    SELECT * FROM comments
    WHERE post_id = :postId AND parent_id IS NULL
    ORDER BY created_at DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findFirstPageRootCommentsByDate(
    @Param("postId") UUID postId,  
    @Param("limit") int limit
  );

  @Query(value = """
    SELECT * FROM comments
    WHERE post_id = :postId AND parent_id IS NULL
    AND (created_at, id) < (:cursorDate, :cursorId)
    ORDER BY created_at DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findRootCommentsAfterCursorByDate(
    @Param("postId") UUID postId, 
    @Param("cursorDate") OffsetDateTime cursorDate, 
    @Param("cursorId") UUID cursorId, 
    @Param("limit") int limit
  );


  // Get replies of a comment
  @Query(value = """
    SELECT * FROM comments
    WHERE parent_id = :parentId
    ORDER BY score DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findFirstPageRepliesByScore(
    @Param("parentId") UUID parentId,
    @Param("limit") int limit
  );

  @Query(value = """
    SELECT * FROM comments
    WHERE parent_id = :parentId
    AND (score, id) < (:cursorScore, :cursorId)
    ORDER BY score DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findRepliesAfterCursorByScore(
    @Param("parentId") UUID parentId,
    @Param("cursorScore") int cursorScore,
    @Param("cursorId") UUID cursorId,
    @Param("limit") int limit
  );

  @Query(value = """
    SELECT * FROM comments
    WHERE parent_id = :parentId
    ORDER BY created_at DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findFirstPageRepliesByDate(
    @Param("parentId") UUID parentId,
    @Param("limit") int limit
  );

  @Query(value = """
    SELECT * FROM comments
    WHERE parent_id = :parentId
    AND (created_at, id) < (:cursorCreatedAt, :cursorId)
    ORDER BY created_at DESC, id DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<Comment> findRepliesAfterCursorByDate(
    @Param("parentId") UUID parentId,
    @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
    @Param("cursorId") UUID cursorId,
    @Param("limit") int limit
  );

  //Additional
  @Query("SELECT c.parentId, COUNT(c) FROM Comment c WHERE c.parentId IN (:parentIds) GROUP BY c.parentId")
  List<Object[]> countRepliesByParentIds(@Param("parentIds") List<UUID> parentIds);

  @Query(value = """
    SELECT * FROM (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY score DESC) as rn
    FROM comments
    WHERE parent_id IN (:parentIds)
    )
    WHERE rn <= :num
    """, nativeQuery = true)
  List<Comment> findTopRepliesForParents(
    @Param("parentIds") List<UUID> parentIds, 
    @Param("num") Integer num
  );
}
