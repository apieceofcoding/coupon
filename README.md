# coupon-service

## 실행

```bash
   # 이미지 빌드 (Docker daemon 필요)
docker compose up -d       # MySQL + 앱 기동
```

종료:

```bash
docker compose down -v     # 컨테이너 + 볼륨 제거
```

## API 호출 예시

```bash
# 행사 생성
curl -X POST http://localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{"name":"5월 행사","totalQuantity":5000,"validityDays":7}'

# 발급
curl -X POST http://localhost:8080/api/coupons/1/issue \
  -H 'X-User-Id: 42'

# 사용
curl -X POST http://localhost:8080/api/issuances/1/use \
  -H 'X-User-Id: 42'

# 내 쿠폰
curl http://localhost:8080/api/users/me/issuances -H 'X-User-Id: 42'
```
