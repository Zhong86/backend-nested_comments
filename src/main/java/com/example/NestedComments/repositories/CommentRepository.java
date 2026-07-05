package com.example.NestedComments.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.NestedComments.models.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>{
  Page<Comment> findByPostIdAndParentIdIsNull(UUID postId, Pageable pageable);
  Page<Comment> findByParentId(UUID parentId, Pageable pageable);

  @Query("SELECT c.parentId, COUNT(c) FROM Comment c WHERE c.parentId IN :parentIds GROUP BY c.parentId")
  List<Object[]> countRepliesByParentIds(@Param("parentIds") List<UUID> parentIds);

  @Query(value = """
    SELECT * FROM (
      SELECT *, ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY score DESC) as rn
      FROM comments
      WHERE parent_id IN :parentIds
    )
    WHERE rn <= :num
  """, nativeQuery = true)
  List<Comment> findTopRepliesForParents(
    @Param("parentIds") List<UUID> parentIds, 
    @Param("num") Integer num
  );
}
