-- KEYS[1] = coupon:{id} (Hash: value, fetchedAtMs), KEYS[2] = lock key
-- ARGV: nowMs, freshMs, lockToken, lockTtlMs
-- 반환: FRESH | STALE_REFRESH (백그라운드 갱신 트리거) | STALE_AS_IS (다른 인스턴스가 갱신 중)
--        | MISS_LOAD (이 요청이 채움) | MISS_WAIT (다른 요청이 채우는 중)

local data = redis.call('HMGET', KEYS[1], 'value', 'fetchedAtMs')
local value, fetchedAtStr = data[1], data[2]

if not value then
    local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
    if acquired then return {'MISS_LOAD', ''} end
    return {'MISS_WAIT', ''}
end

local now = tonumber(ARGV[1])
local fetchedAt = tonumber(fetchedAtStr)
if now - fetchedAt < tonumber(ARGV[2]) then
    return {'FRESH', value}
end

local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
if acquired then return {'STALE_REFRESH', value} end
return {'STALE_AS_IS', value}
