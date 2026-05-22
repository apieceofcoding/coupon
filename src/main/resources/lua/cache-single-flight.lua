-- KEYS[1] = coupon:{id}, KEYS[2] = lock key
-- ARGV[1] = lockToken (UUID), ARGV[2] = lockTtlMs
-- 반환 { state, value }: HIT | LOAD (이 요청이 채움) | WAIT (다른 요청이 채우는 중)

local cached = redis.call('GET', KEYS[1])
if cached then return {'HIT', cached} end

local acquired = redis.call('SET', KEYS[2], ARGV[1], 'NX', 'PX', ARGV[2])
if acquired then return {'LOAD', ''} end
return {'WAIT', ''}
