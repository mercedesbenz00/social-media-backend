CREATE KEYSPACE IF NOT EXISTS short_video_service WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': 1
};