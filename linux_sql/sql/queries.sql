
-- group hosts by hardware info
SELECT
  FIRST_VALUE(cpu_number) OVER(PARTITION BY cpu_number ORDER BY total_mem DESC) AS cpu_number,
  id AS host_id,
  total_mem
FROM host_info;

-- function for rounding current ts every 5 mins
CREATE FUNCTION round5(ts timestamp) RETURNS timestamp AS
$$
BEGIN
    RETURN date_trunc('hour', ts) + date_part('minute', ts):: int / 5 * interval '5 min';
END;
$$
    LANGUAGE PLPGSQL;

-- average memory usage over 5 mins interval for each host
SELECT
  h_usage.host_id,
  h_info.hostname,
  round5(h_usage.timestamp) AS "timestamp",
  AVG((h_info.total_mem::float - h_usage.memory_free::float)/ h_info.total_mem::float * 100)::int AS avg_used_mem_percentage
FROM
  host_usage AS h_usage,
  host_info AS h_info
WHERE
  h_usage.host_id = h_info.id
GROUP BY
  round5(h_usage.timestamp),
  h_info.id,
  h_usage.host_id;

-- detect host failure
SELECT
  h_usage.host_id,
  round5(h_usage.timestamp) AS "timestamp",
  COUNT(h_usage.timestamp) AS num_data_points
FROM
  host_usage AS h_usage
GROUP BY
  round5(h_usage.timestamp),
  h_usage.host_id;