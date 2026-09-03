# 부하 테스트

## 사전 준비

- macOS: `brew install k6 jq`
- Windows: Git Bash 에서 실행한다. `winget install jqlang.jq`, `winget install k6 --source winget`
- `docker compose up -d` 후 서비스가 8080 응답

### 브랜치 전환 시

브랜치마다 서비스 코드가 다르므로, 전환 후에는 이미지를 새로 생성하여 도커 컨테이너를 재실행한다.

```bash
git checkout <branch>
./gradlew --stop && ./gradlew jibDockerBuild && docker compose up -d
```

## part-2

```bash
./scripts/load/part-2/run.sh
```

`run.sh` 는 `reset → create_coupon → k6 → verify` 를 한 번에 실행한다.
