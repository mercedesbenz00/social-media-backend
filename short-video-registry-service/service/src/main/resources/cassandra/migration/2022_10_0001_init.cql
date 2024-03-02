CREATE TABLE IF NOT EXISTS category
(
    id            uuid,
    category_name text,
    created_at    timestamp,
    updated_at    timestamp,
    PRIMARY KEY (id, category_name)
)
    WITH CACHING = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     AND COMPACTION = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     AND COMPRESSION = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     AND SPECULATIVE_RETRY = '99p';


CREATE TABLE IF NOT EXISTS short_video
(
    id               uuid primary key,
    author_id        bigint,
    bucket           text,
    categories       set<uuid>,
    comments_allowed boolean,
    created_at       timestamp,
    metadata         map<text, text>,
    privacy_level    text,
    selected_groups  set<bigint>,
    selected_users   set<bigint>,
    title            text,
    updated_at       timestamp,
    year             int
)
    WITH CACHING = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     AND COMPACTION = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     AND COMPRESSION = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     AND SPECULATIVE_RETRY = '99p';

CREATE TABLE IF NOT EXISTS short_video_author_friends
(
    author_id       bigint,
    friend_user_id  text,
    author_username text,
    primary key (author_id, friend_user_id)
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_video_config
(
    id               uuid primary key,
    comments_allowed boolean,
    created_at       timestamp,
    person_id        bigint,
    privacy_level    text,
    selected_groups  set<bigint>,
    selected_users   set<bigint>
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_video_stats
(
    id       uuid primary key,
    comments counter,
    dislikes counter,
    halves   counter,
    likes    counter,
    skips    counter,
    views    counter
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_video_tags
(
    id         uuid primary key,
    createdat  timestamp,
    externalid text,
    name       text,
    updatedat  timestamp
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_video_vote
(
    person_id      bigint,
    short_video_id uuid,
    created_at     timestamp,
    vote_type      int,
    primary key ((person_id, short_video_id))
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_videos_by_author
(
    author_id        bigint,
    year             int,
    created_at       timestamp,
    short_video_id   uuid,
    bucket           text,
    categories       set<uuid>,
    comments_allowed boolean,
    metadata         map<text, text>,
    privacy_level    text,
    selected_groups  set<bigint>,
    selected_users   set<bigint>,
    title            text,
    updated_at       timestamp,
    primary key ((author_id, year), created_at, short_video_id)
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_videos_of_category
(
    year             int,
    category_id      uuid,
    created_at       timestamp,
    short_video_id   uuid,
    author_id        bigint,
    bucket           text,
    categories       set<uuid>,
    comments_allowed boolean,
    metadata         map<text, text>,
    privacy_level    text,
    selected_groups  set<bigint>,
    selected_users   set<bigint>,
    title            text,
    updated_at       timestamp,
    primary key ((year, category_id), created_at, short_video_id)
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';

CREATE TABLE IF NOT EXISTS short_videos_of_friends
(
    user_id          bigint,
    year             int,
    author_user_name text,
    created_at       timestamp,
    author_id        bigint,
    bucket           text,
    categories       set<uuid>,
    comments_allowed boolean,
    metadata         map<text, text>,
    privacy_level    text,
    selected_groups  set<bigint>,
    selected_users   set<bigint>,
    short_video_id   uuid,
    title            text,
    updated_at       timestamp,
    primary key ((user_id, year, author_user_name), created_at)
)
    with caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
     and compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
     and compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
     and speculative_retry = '99p';










