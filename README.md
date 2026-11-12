# didim-server

"디딤" 백엔드. 2026 용용톤 복지 트랙 15조.

## 시작하기

```bash
cp .env.example .env
./gradlew bootRun
```

- 서버: http://localhost:8080
- API 문서: http://localhost:8080/swagger-ui.html
- 헬스체크: `GET /api/v1/health`
- 테스트: `./gradlew test`
- 구조와 API 공통 규칙: `AGENTS.md`
