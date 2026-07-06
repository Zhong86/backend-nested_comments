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
- [ ] Add recursive CTE endpoint for fetching a full subtree in one query
- [ ] Implement materialized path (`path = "1/5/23"`) for efficient subtree lookups
- [ ] Switch offset pagination to cursor-based pagination
- [ ] (Optional/advanced) Build a closure table for complex ancestor/descendant queries
- [ ] Load-test with a large synthetic comment tree (10k+ comments) and check query performance

## Notes

