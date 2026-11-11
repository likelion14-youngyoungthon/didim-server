# dojang-server

용인시 등록장애인과 보호자에게 등록 후 남은 복지 신청을 알려주는 서비스 "도장"의 백엔드. 2026 용용톤 복지 트랙 15조.

## 실행

- Java 25, Spring Boot 4.1, Gradle(래퍼 포함), Lombok
- 서버: `./gradlew bootRun` (포트 8080, 프로필 기본값 local)
- 테스트: `./gradlew test`. 작업이 끝나면 통과시킨다
- API 문서: `/swagger-ui.html`
- 환경변수는 `.env`(형식은 `.env.example`)에 두면 자동으로 읽는다. 설정은 `application.yml`, `application-local.yml`, `application-prod.yml`

## 구조 (com.likelionknu.dojang)

기능마다 패키지를 하나 만들고 안을 같은 모양으로 나눈다.

```
기능이름/
  controller/
  data/dto/request, data/dto/response
  service/
  exception/      GlobalException 상속
common/
  response/       GlobalResponse, ErrorCode, GlobalExceptionHandler
  config/         CORS, 헬스체크
  swagger/
```

## API 공통 규칙 (노션 "API 명세서" 기준)

- 주소는 `/api/v1/...`
- 응답은 `GlobalResponse`로 감싼다
  - 성공: `{"code": "OK", "data": {...}, "message": null}` → `GlobalResponse.ok(data)`
  - 실패: `{"code": "E400", "data": null, "message": "사유"}` → 예외를 던지면 `GlobalExceptionHandler`가 만든다
- 에러 code는 E + HTTP 상태코드(E400, E404, E500, E502). AI 호출 실패는 `ErrorCode.AI_FAILED`(E502)
- JSON 필드는 snake_case
- 로그인은 선택이다. 로그인 회원의 프로필·신청 현황은 `/api/v1/me/progress`로 저장·조회한다(운영 MariaDB, 로컬 메모리 H2). 대화 원문은 저장하지 않는다.

## 지킬 것

- 자기 담당 패키지만 고친다. `common/`을 바꿀 때는 팀에 먼저 알린다
- 복지 자격 판정은 규칙표와 코드로 한다. LLM이 자격이나 상태를 직접 정하지 않는다
- 근거가 없는 값(서류, 기한, 전화번호 등)을 지어내지 않는다. 모르면 "확인 필요"
- `.env`와 API 키는 커밋하지 않고, 코드나 로그에 키를 출력하지 않는다
- 실명, 주민번호 같은 개인정보는 받지 않는다
