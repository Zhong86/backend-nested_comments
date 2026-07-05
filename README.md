# Nested Comments Backend (Reddit-style) — Project Plan
Build a backend that supports threaded/nested comments on posts, similar to Reddit — including replies, sorting, and pagination — without fetching entire comment trees at once.

---

## 2. Data Model

```
User {
  id: UUID
  username: TEXT
  created_at: TIMESTAMP
}

Post {
  id: UUID
  author_id: UUID
  title: TEXT
  body: TEXT
  created_at: TIMESTAMP
}

Comment {
  id: UUID
  post_id: UUID
  parent_id: UUID | NULL     -- NULL = top-level comment
  author_id: UUID
  body: TEXT
  score: INTEGER DEFAULT 0   -- upvotes - downvotes
  created_at: TIMESTAMP
  deleted: BOOLEAN DEFAULT FALSE
}

Vote {
  id: UUID
  comment_id: UUID
  user_id: UUID
  value: INTEGER  -- +1 or -1
  UNIQUE(comment_id, user_id)
}
```

Key idea: a reply is just a `Comment` whose `parent_id` points to another comment instead of being `NULL`. Self-referencing foreign key = the whole trick.

---

## 3. Core Flow / Decision Logic

### 3.1 Initial page load (root comments only)
1. Client requests: `GET /posts/:postId/comments?sort=top&limit=20&offset=0`
2. Backend fetches **only top-level comments** (`parent_id IS NULL`) for that post
3. Sort applied **within that group only**
4. For each root comment, attach:
   - A small preview of top replies (e.g. 2–3)
   - A `reply_count` (total children, not the children themselves)
5. Return the 20 threads + previews + counts to the client

```sql
SELECT * FROM comments
WHERE post_id = ? AND parent_id IS NULL AND deleted = FALSE
ORDER BY score DESC
LIMIT 20 OFFSET 0;
```

### 3.2 Expanding a thread ("load more replies")
1. Client requests: `GET /comments/:commentId/replies?limit=10&offset=0`
2. Backend fetches: `WHERE parent_id = :commentId ORDER BY score DESC LIMIT 10 OFFSET 0`
3. Each returned reply also gets its own `reply_count` (lazy — don't recurse further automatically)

### 3.3 Golden rule
**Never fetch the whole tree in one query.** Every level (root comments, and each comment's children) is its own paginated, sorted query — sorting and pagination scoped **per parent_id**, not globally.

---

## 4. Sorting Logic

Sorting is applied **per sibling group** (per `parent_id`), not across the whole tree.

| Sort type | Logic |
|---|---|
| `top` (v1) | `ORDER BY score DESC` |
| `new` | `ORDER BY created_at DESC` |
| `hot` (later) | score + time-decay formula (log scale on votes, age in seconds) |
| `controversial` (later) | high vote count, near 50/50 up/down split |

Start with `top` = `upvotes - downvotes DESC`. Add time-decay once basics work.

---

## 5. Pagination Rules

- Root comments: paginated with `LIMIT` / `OFFSET` (or cursor-based later)
- Replies: paginated the same way, scoped to `parent_id`
- Depth cap: stop auto-expanding after N levels (e.g. 3) — deeper levels require explicit "load more" clicks
- Soft delete: deleted comments stay as placeholders (`[deleted]`) so replies underneath aren't orphaned

---
## 7. API Endpoints (planned)

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/posts/:postId/comments` | Create a top-level comment |
| POST | `/comments/:commentId/replies` | Create a reply to a comment |
| GET | `/posts/:postId/comments?sort=&limit=&offset=` | Get root comments (paginated, sorted) |
| GET | `/comments/:commentId/replies?limit=&offset=` | Get replies to a comment (paginated, sorted) |
| POST | `/comments/:commentId/vote` | Upvote/downvote, recompute score |
| DELETE | `/comments/:commentId` | Soft-delete comment |

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
- [ ] Implement `sort=new` (`ORDER BY created_at DESC`)
- [ ] Implement `sort=top` (`ORDER BY score DESC`)
- [ ] Apply sorting **per parent_id group**, not globally
- [ ] Add `sort` query param to both root and replies endpoints

## Phase 6: Voting
- [ ] `POST /comments/:commentId/vote` — upvote/downvote endpoint
- [ ] Enforce one vote per user per comment (unique constraint)
- [ ] Allow changing/removing a vote
- [ ] Recompute and store `score` on the comment (upvotes - downvotes)

## Phase 7: Soft Delete
- [ ] `DELETE /comments/:commentId` — mark `deleted = TRUE` instead of removing row
- [ ] When fetching, replace deleted comment body with `[deleted]` but still show its replies
- [ ] Prevent voting/replying rules update if needed (optional)

## Phase 8: Depth & Safety Rules
- [ ] Add max nesting depth (e.g. 10 levels) — reject replies beyond that
- [ ] Add basic rate limiting on comment creation (optional but realistic)

## Phase 9: Testing the Full Flow
- [ ] Seed a post with a large nested comment tree (script or manual)
- [ ] Test root fetch + pagination
- [ ] Test reply expansion at multiple depths
- [ ] Test sorting changes results correctly
- [ ] Test voting updates score and re-sorts correctly

---

## Stretch Goals (final stage)
- [ ] Implement `sort=hot` with time-decay scoring formula
- [ ] Implement `sort=controversial` (high votes, near 50/50 split)
- [ ] Add recursive CTE endpoint for fetching a full subtree in one query
- [ ] Implement materialized path (`path = "1/5/23"`) for efficient subtree lookups
- [ ] Switch offset pagination to cursor-based pagination
- [ ] (Optional/advanced) Build a closure table for complex ancestor/descendant queries
- [ ] Load-test with a large synthetic comment tree (10k+ comments) and check query performance

## Notes

