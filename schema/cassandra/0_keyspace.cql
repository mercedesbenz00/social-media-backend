CREATE KEYSPACE IF NOT EXISTS short_video_service
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 1
  };