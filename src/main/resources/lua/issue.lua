-- issue.lua: 4.2 발급 차감 + 매진 플래그 set
-- 호출 위치: CouponIssuer.tryIssue() 가 발급 요청마다 실행
-- (2단원의 발급 Lua 에 "마지막 1장일 때 sold_out 플래그 set" 만 추가)
--
-- KEYS[1] = coupon:{id}:stock    (재고 카운터, 행사 생성 시 totalQuantity 로 초기화)
-- KEYS[2] = coupon:{id}:users    (발급된 사용자 set)
-- KEYS[3] = coupon:{id}:sold_out (매진 플래그, 마지막 1장 차감과 동시에 set)
-- ARGV[1] = user_id
-- ARGV[2] = sold_out TTL seconds (이벤트 종료 시각까지)
-- 반환:
--    1  발급 성공
--    0  매진
--   -1  이미 발급된 사용자

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -1
end
local remaining = tonumber(redis.call('GET', KEYS[1]) or '0')
if remaining <= 0 then
  return 0
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

-- 마지막 1장 차감과 동시에 매진 플래그 set. 매진은 정확히 한 번 일어나는 사건이라
-- 차감 atomic 안에 같이 넣는 게 자연스럽다.
if remaining == 1 then
  redis.call('SET', KEYS[3], '1', 'EX', ARGV[2])
end

return 1
