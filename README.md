# Nested Comments Backend (Reddit-style) — Project Plan
Build a backend that supports threaded/nested comments on posts, similar to Reddit — including replies, sorting, and pagination — without fetching entire comment trees at once.

---
# Nested Comments Backend — TODO Checklist

## Phase 1: Schema
- [ x ] Create `users` table
- [ x ] Create `posts` table
- [ x ] Create `comments` table (with `parent_id` self-reference)
- [ x ] Create `votes` table
- [ x ] Add indexes on `comments.post_id` and `comments.parent_id`

## Phase 2: Basic Comment Creation
- [ x ] `POST /posts/:postId/comments` — create top-level comment
- [ x ] `POST /posts/:postId/comments/:commentId/replies` — create reply
- [ x ] Validate `parent_id` belongs to the same `post_id`
- [ x ] Handle basic errors (missing post, missing parent, empty body)

## Phase 3: Fetching Root Comments
- [ x ] `GET /posts/:postId/comments` — fetch top-level comments only (`parent_id IS NULL`)
- [ x ] Add `limit` / `offset` query params
- [ x ] Add `reply_count` per comment (count query)
- [ x ] Add small preview of top 2-3 replies per comment

## Phase 4: Fetching Replies (Expand Thread)
- [ x ] `GET /comments/:commentId/replies` — fetch children of a comment
- [ x ] Add `limit` / `offset` pagination
- [ x ] Add `reply_count` for each nested reply too (1 preview reply)

## Phase 5: Sorting
- [ x ] Implement `sort=new` (`ORDER BY created_at DESC`)
- [ x ] Implement `sort=top` (`ORDER BY score DESC`)
- [ x ] Apply sorting **per parent_id group**, not globally
- [ x ] Add `sort` query param to both root and replies endpoints

## Phase 6: Voting
- [ x ] `POST /comments/:commentId/vote` — upvote/downvote endpoint
- [ x ] Enforce one vote per user per comment (unique constraint)
- [ x ] Allow changing/removing a vote
- [ x ] Recompute and store `score` on the comment (upvotes - downvotes)

## Phase 7: Soft Delete
- [ x ] `DELETE /comments/:commentId` — mark `deleted = TRUE` instead of removing row
- [ x ] When fetching, replace deleted comment body with `[deleted]` but still show its replies
- [ x ] Prevent voting/replying rules update if needed (optional)

## Phase 8: Depth & Safety Rules
- [ x ] Add max nesting depth (e.g. 10 levels) — reject replies beyond that
- [ x ] Add basic rate limiting on comment creation (optional but realistic)

## Stretch Goals (final stage)
- [ x ] Switch offset pagination to cursor-based pagination

## Notes
Nested Comments have models: Post, Comment
Comments can have a parentId for replies (if it's the root comment then parentId is NULL).
Max depth is set to 10, can be less - if it exceeds, then it throws an error. 

### Fetch Methods
1. Nested Query: The SQL Query will fetch root comment then its preview comments (not all); showing the replies will then call every comment (pagination). To prevent N + 1 fetches, the query relies on window and nested queries. 
```
SELECT * FROM (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY score DESC) as rn
    FROM comments
    WHERE parent_id IN (:parentIds)
)
WHERE rn <= :num
```
This will get every comment for every parent (root comment). The window query is the `ROW_NUMBER() OVER (PARTITION BY ...)` which acts similar to GROUPBY but the index for every grouped comment will reset to start from 0 again. 

2. Recursive CTE (Common Table Expression).Does not meet this usecase because we want pagination. This can be used if every item wants to get taken. 

3. Materialized Path / Closure Table. This method stores the ID of the ancestor and the the descendant, can be in a new column (path) or a completely new table (columns: ancestor_id, descendant_id, depth). This method significantly reduces READ speeds, but greatly increases WRITE complexity & storage.  

### Pagination 
1. Standard Pagination -> Limits, Offset. This method takes every all data then limits it. Overall, really safe and okay if dataset is not large. 
2. Cursor-Based Pagination -> Limit, lastId, lastX. A lot faster than Standard because this only fetches data past lastId instead of taking every row. Helps efficiency with bigger data sets.
```
SELECT * FROM comments
WHERE post_id = :postId AND parent_id IS NULL
AND (score, id) < (:cursorScore, :cursorId)
ORDER BY score DESC, id DESC
LIMIT :limit
```

### Additional
- Sorting options: top & new. Top is measured by the amount of likes.
- Comments are SOFT deleted (deleted column = True) to allow its replies to still be accessible. 
