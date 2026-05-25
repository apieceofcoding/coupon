-- KEYS[1] = coupon:{id} (Hash: value, fetchedAtMs), KEYS[2] = lock key
-- ARGV: nowMs, freshMs, lockToken, lockTtlMs
-- 반환: HIT (값 반환) | STALE_REFRESH (값 반환 + 백그라운드 refresh)
--        | LOAD (이 요청이 채움) | WAIT (다른 요청이 채우는 중)

local data = redis.call('HMGET', KEYS[1], 'value', 'fetchedAtMs')
local value, fetchedAtStr = data[1], data[2]

if not value then
    local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
    if acquired then return {'LOAD', ''} end
    return {'WAIT', ''}
end

local now = tonumber(ARGV[1])
local fetchedAt = tonumber(fetchedAtStr)
if now - fetchedAt < tonumber(ARGV[2]) then
    return {'HIT', value}
end

-- stale: refresh lock 시도. 못 잡으면 다른 요청이 갱신 중이니 그냥 HIT.
local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
if acquired then return {'STALE_REFRESH', value} end
return {'HIT', value}
