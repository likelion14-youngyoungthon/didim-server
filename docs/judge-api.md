# 남은 신청 판정

## Summary

| 항목 | 내용 |
| --- | --- |
| Endpoint / Method | `POST /api/v1/judge` |
| 로그인 | 필요 없음 |
| 용도 | 결과 보기·체험 인물 선택·상황 변경·재방문 때 판정. 서버에 결과 저장 안 함 |

## Request

```json
{"age":4,"disability_type":"autism","severity":"severe","dong":"백암면","household":3,"status":"neither","income_band":"le180","reg_items":["fee_reductions_7","parking_sticker"],"reg_items_unknown":false,"tags":["daycare"]}
```

`age`(0 이상 정수)와 `dong`은 필수. `household`는 선택이며 1 이상이다.
장애 유형·장애 정도·수급 여부·소득 구간의 미확인은 `unknown` 또는 null.
읍면동·등록 항목·소득 구간은 각각 `/dongs`, `/reg-items`, `/income-bands` 값을 사용한다.
`reg_items: []`는 고른 항목 없음, 필드 누락은 등록 내역 미확인이다.
`tags` 허용값은 `daycare`, `has_car`, `wheelchair`, `newborn`이다.

## Response (200)

`{"code":"OK","data":{"서비스 id":{...}},"message":null}`. data는 규칙표 순서를 유지한다.
기존 판정·접수처·서식·서류·근거 필드에 아래 값을 추가한다.

| 필드 | 값 / 화면 처리 |
| --- | --- |
| category | 대응표 아이콘 종류. null이면 숨김 |
| short_desc / description | 목록 둘째 줄 / 상세 설명. null이면 숨김. 발달재활 목록 금액은 benefit과 동일 |
| check_reason | `status: check`일 때만 규칙표 문구, 그 외 null |
| benefit | `{amount, note}` 또는 null. 규칙표 when을 요청의 status → income_band → any 순으로 대조하고 처음 일치한 값 반환 |
| conditions | `[{key,label,rule,value,met}]` 또는 null. rule은 표시 문장, value는 요청의 한국어 표시. met은 구조화된 규칙 기준 true/false/미확인 null |

조건 키는 `age`, `disability_type`, `severity`, `income_band`, `status`를 지원한다.
미지원 키는 value/met을 null로 두며 표시 문장을 자격 조건으로 해석하지 않는다.
수급과 소득 구간을 구분하지 못하는 금액은 추정하지 않는다(예: 발달재활 le120의 세부 소득).
규칙표의 `short_desc_from_benefit: true`는 선택된 benefit의 amount·note로 목록 설명을 만든다.
매칭된 benefit이 없으면 short_desc의 금액 없는 기본 문구를 쓴다(11/13 사용자 확정).
11개 체험 대표 서비스의 확인 가능한 표시값을 채웠고, 제공되지 않은 값은 null로 유지한다.

## 검증 기준

11/13 `./gradlew test`: 68개 통과, 실패·오류 0. 코드 검토와 공백 검사 통과.

- 4세 예시: 신청 전 5 + 확인 필요 2 + 공고 대기 1, 등록 때 신청 2, 나중에 가능 4.
- 남은 신청 중 행정복지센터 5건, 중복 제거한 센터 신청서 5종.
- 6세 미만+daycare 보육료는 apply. 6세 이상은 취학 확인 유지, daycare 없으면 숨김.
- notes에는 age_note·severity_note·office_note·amount_note를 합치지 않는다.
- 발달재활 의뢰서는 documents에만 포함하고 form_names/form_files에서는 제외한다.

## 오류

`E400`: 누락·범위·허용값·본문 형식 오류. `E405`: POST 이외 방식. `E500`: 서버 오류.
공통 `GlobalResponse` 형식이며 프론트는 message 문구 대신 code로 분기한다.

## 별도 작업 / 확인 필요

- `online`과 `easy`는 이번 변경에 포함하지 않는다.
- 원본: 노션 「남은 신청 판정」(11/12), 「대응표 (11월 12일 기준)」, FE `origin/dev` 702c3a4의 personas/results와 기존 규칙표. 혜택·조건은 규칙표에서 확인되는 내용만 사용.
