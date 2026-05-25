-- KEYS: cache, lock, fallback / ARGV: lockToken, lockTtlMs
-- 반환: HIT | LOAD | WAIT_FALLBACK (stale) | WAIT_MISS (대기 필요)

local cached = redis.call('GET', KEYS[1])
if cached then return {'HIT', cached} end

local acquired = redis.call('SET', KEYS[2], ARGV[1], 'NX', 'PX', ARGV[2])
if acquired then return {'LOAD', ''} end

local fallback = redis.call('GET', KEYS[3])
if fallback then return {'WAIT_FALLBACK', fallback} end
return {'WAIT_MISS', ''}
