# 신청 단계 (1)

- `GET /api/v1/services/{id}/steps`, 로그인 불필요. 서비스의 공통 4단계를 반환한다.
- 순서: prepare(서류 준비) → submit(접수처별 신청 문구) → review(자격 심사) → result(결과 안내).
- 모든 단계의 `days`는 null, `parallel`은 prepare만 true이다.
- 현재 단계 번호는 1부터 시작하며 화면이 저장된 서비스의 status로 계산한다: submitted → 3, approved·in_use → 4. 이 API 요청·응답에는 사용자 상태나 current_step을 추가하지 않는다. 그 밖의 상태 매핑은 이 명세에서 정하지 않는다.
- E404: `service: 없는 서비스입니다.`, E405: `지원하지 않는 요청 방식입니다.`, E500: `서버 오류가 발생했습니다.`
