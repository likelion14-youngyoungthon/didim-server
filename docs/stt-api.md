# 음성 변환

- `POST /api/v1/stt`, `multipart/form-data`, 파일 필드 `audio`.
- WebM/WAV, 최대 10MiB. 브라우저 `filename="blob"` 및 `audio/webm;codecs=opus` 허용.
- 성공: `{"code":"OK","data":{"text":"변환 결과"},"message":null}`.
- 누락·빈 파일·용량 초과·지원 형식 불일치: E400. 외부 변환 실패·시간 초과·키 미설정: E502.
- 컨테이너 헤더만 사전 확인하며 실제 오디오 디코딩은 공급자가 수행. 손상 파일이 공급자에서 거부되면 E502.
- STT는 텍스트만 반환. 확인·수정 후 `/analyze`에 보내는 것은 프론트 흐름이며 자동 판정·상태 저장은 하지 않음.

## 실행·연동

- 키: `STT_API_KEY`, 비어 있으면 기존 `LLM_API_KEY` 사용. 실제 키는 DM으로 전달받아 `.env`에만 입력.
- 모델: `STT_MODEL` (미설정·빈 값이면 `gpt-transcribe`).
- 음성과 변환 결과는 DB·로그에 저장하지 않음. 제한 내 업로드는 메모리에서 처리.
- Spring 파일 10MiB / 전체 요청 11MiB, Nginx 전체 요청 11MiB 제한. 외부 API 읽기 제한 60초.
- Nginx의 업로드 오류도 Spring과 같은 `CORS_ALLOWED_ORIGINS` 목록에만 공개. 시작 시 전용 스크립트로 목록을 생성.
- API 키가 없어도 모의 테스트는 실행 가능. 실제 WAV 전사는 확인했고, WebM은 브라우저 형식·업로드 계약 테스트까지 확인(실제 녹음 전사는 프론트 연동 시 확인).

```bash
curl http://localhost:8080/api/v1/stt -F 'audio=@sample.webm;type=audio/webm'
```

[공식 호출 규격](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create)
