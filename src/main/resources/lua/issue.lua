-- KEYS: stock, users (set), sold_out
-- ARGV: userId, soldOutTtlSeconds
-- 반환: 1=성공, 0=매진, -1=중복 발급

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -1
end
local remaining = tonumber(redis.call('GET', KEYS[1]) or '0')
if remaining <= 0 then
  return 0
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

if remaining == 1 then
  redis.call('SET', KEYS[3], '1', 'EX', ARGV[2])
end

return 1
