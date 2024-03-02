create table notification_event
(
  event_date DateTime,
  event_id UUID,
  receiver_id String,
  notification_type String,
  body String
)
Engine = MergeTree
partition by toYYYYMMDD(event_date)
order by (event_date);

create table notification_view (
  view_date DateTime,
  event_date DateTime,
  event_id UUID,
  receiver_id String,
  state String
)
Engine = MergeTree
partition by toYYYYMMDD(event_date)
order by (event_date);
