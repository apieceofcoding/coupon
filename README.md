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

## API 호출

[scripts/api.sh](scripts/api.sh) (`jq` 필요)
