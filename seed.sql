-- =========================================================
-- Seed Data: users, posts, comments (with nested replies)
-- =========================================================

-- =========================================================
-- USERS
-- =========================================================
INSERT INTO users (id, username, email) VALUES
    ('11111111-1111-1111-1111-111111111111', 'alice', 'alice@example.com'),
    ('22222222-2222-2222-2222-222222222222', 'bob',   'bob@example.com'),
    ('33333333-3333-3333-3333-333333333333', 'carol', 'carol@example.com'),
    ('44444444-4444-4444-4444-444444444444', 'dave',  'dave@example.com');

-- =========================================================
-- POSTS
-- =========================================================
INSERT INTO posts (id, author_id, title, body) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '11111111-1111-1111-1111-111111111111',
     'What''s the best way to learn backend development?',
     'Looking for advice on how to get started with backend engineering. Any resources or project ideas?'),

    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     '22222222-2222-2222-2222-222222222222',
     'Postgres vs MongoDB for a new project',
     'Starting a new side project and trying to decide between Postgres and MongoDB. Thoughts?');

-- =========================================================
-- COMMENTS + REPLIES for Post 1 (aaaaaaaa...)
-- =========================================================

-- Root comment 1 (top score)
INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000001-0000-0000-0000-000000000001',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     NULL,
     '22222222-2222-2222-2222-222222222222',
     'Build a real project instead of just doing tutorials. Something like a nested comments system, an API, or a small clone of an app you use.',
     42, 0);

-- Replies to root comment 1 (depth 1)
INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000001-0000-0000-0000-000000000002',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000001',
     '33333333-3333-3333-3333-333333333333',
     'Agreed. I learned more from one real project than 10 tutorials combined.',
     15, 1),

    ('c0000001-0000-0000-0000-000000000003',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000001',
     '44444444-4444-4444-4444-444444444444',
     'What stack would you recommend for a first backend project?',
     8, 1),

    ('c0000001-0000-0000-0000-000000000004',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'Also don''t skip learning how databases actually work under the hood.',
     5, 1);

-- Nested reply to reply (depth 2) — reply to comment 3
INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000001-0000-0000-0000-000000000005',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000003',
     '22222222-2222-2222-2222-222222222222',
     'Spring Boot + Postgres is a solid, widely-used combo to start with.',
     12, 2),

    ('c0000001-0000-0000-0000-000000000006',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000003',
     '33333333-3333-3333-3333-333333333333',
     'Node/Express is also great if you want something lighter to start.',
     6, 2);

-- Deeper nested reply (depth 3) — reply to comment 5
INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000001-0000-0000-0000-000000000007',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000005',
     '44444444-4444-4444-4444-444444444444',
     'Seconding this, Spring Boot + Postgres is what I used for my first real project too.',
     4, 3);

-- Root comment 2 (lower score, no replies)
INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000001-0000-0000-0000-000000000008',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     NULL,
     '44444444-4444-4444-4444-444444444444',
     'Read the docs for whatever framework you pick, seriously. It helps more than people think.',
     3, 0);

-- Root comment 3 (deleted, but has replies underneath — tests soft delete)
INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth, deleted) VALUES
    ('c0000001-0000-0000-0000-000000000009',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     NULL,
     '11111111-1111-1111-1111-111111111111',
     'this comment was removed',
     1, 0, TRUE);

INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000001-0000-0000-0000-000000000010',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'c0000001-0000-0000-0000-000000000009',
     '22222222-2222-2222-2222-222222222222',
     'Replying even though the parent got deleted, to test orphan handling.',
     2, 1);

-- =========================================================
-- COMMENTS + REPLIES for Post 2 (bbbbbbbb...)
-- =========================================================

INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000002-0000-0000-0000-000000000001',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     NULL,
     '11111111-1111-1111-1111-111111111111',
     'Postgres, unless your data is truly document-shaped with no relations. Most projects underestimate how relational their data actually is.',
     30, 0),

    ('c0000002-0000-0000-0000-000000000002',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     NULL,
     '33333333-3333-3333-3333-333333333333',
     'MongoDB is fine if you need flexible schemas early on, but migrating away from it later can be painful.',
     10, 0);

INSERT INTO comments (id, post_id, parent_id, author_id, body, score, depth) VALUES
    ('c0000002-0000-0000-0000-000000000003',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     'c0000002-0000-0000-0000-000000000001',
     '44444444-4444-4444-4444-444444444444',
     'This matches my experience exactly. Started with Mongo, ended up rebuilding on Postgres a year later.',
     9, 1);

-- =========================================================
-- VOTES (a few, to make scores meaningful / testable)
-- =========================================================
INSERT INTO votes (comment_id, user_id, value) VALUES
    ('c0000001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 1),
    ('c0000001-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 1),
    ('c0000001-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 1),
    ('c0000002-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 1),
    ('c0000002-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 1);
