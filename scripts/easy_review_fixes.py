"""쉬운 말 검수(Sonnet 2개, 2026-11-13) 결과를 easy_texts.json에 반영한다.

검수자가 재료(규칙표, 공식 원문)로 확인한 수정만 넣는다. 반영 뒤 make_easy_texts.py의 check()로 다시 검사하고
검수가 끝난 서비스는 reviewed를 true로 바꾼다.
실행: python scripts/easy_review_fixes.py
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from make_easy_texts import OUT, check, load_material  # noqa: E402

W, N, A = "무엇을 해요", "누가 받아요", "알아 두세요"


def item(icon, text):
    return {"icon": icon, "text": text}


# 서비스 id -> 바꿀 필드. groups는 통째로 바꾼다
FIXES = {
    "dd_day_activity": {
        "groups": [
            {"title": W, "items": [item("sun", "낮에 여러 곳에서 동료와 함께 활동해요."), item("ticket", "한 달에 132시간이나 176시간까지 써요.")]},
            {"title": N, "items": [item("user_check", "지적장애나 자폐성 장애로 등록한 18살 이상이에요."), item("money", "소득과 관계없이 신청할 수 있어요.")]},
            {"title": A, "items": [item("hourglass", "기관 자리가 없으면 기다릴 수 있어요.")]},
        ],
    },
    "dev_rehab_voucher": {
        "groups": [
            {"title": W, "items": [item("sun", "언어, 미술, 음악, 놀이 등 발달을 도와요."), item("money", "한 달 지원액은 18만 원부터 26만 원까지예요.")]},
            {"title": N, "items": [item("user_check", "집 소득이 많지 않은 18살 미만 아동이에요."), item("user_check", "뇌병변, 지적, 자폐성, 청각, 언어, 시각 장애예요.")]},
            {"title": A, "items": [item("hourglass", "병원에서 하는 물리치료는 지원이 안 돼요."), item("money", "내는 돈은 한 달 0원부터 8만 원까지예요.")]},
        ],
    },
    "family_care_support": {
        "groups": [
            {"title": W, "items": [item("sun", "돌봄을 1년에 1,200시간까지 받을 수 있어요."), item("calendar", "한 달에는 160시간 안에서 이용해요.")]},
            {"title": N, "items": [item("user_check", "18살 미만, 장애 정도가 심한 아동이에요."), item("money", "소득이 많지 않으면 무료예요.")]},
            {"title": A, "items": [item("money", "소득이 많아도 이용료의 40%를 내고 받아요."), item("user_check", "장애인활동지원이나 아이돌봄과 같이 못 써요.")]},
        ],
    },
    "dd_parent_counsel": {
        "groups": [
            {"title": W, "items": [item("users", "개별 상담이나 집단 상담을 해요."), item("calendar", "기본 12개월 동안 지원해요.")]},
            {"title": N, "items": [item("user_check", "지적장애나 자폐성 장애 자녀의 부모예요."), item("user_check", "자녀 나이 조건은 따로 안 적혀 있어요.")]},
            {"title": A, "items": [item("hourglass", "상담사가 필요하다고 보면 한 번 더 연장해요."), item("calendar", "끝난 뒤 2년 동안은 다시 못 받아요.")]},
        ],
    },
    "dd_family_rest": {
        "groups": [
            {"title": W, "items": [item("users", "가족 캠프와 테마여행을 지원해요."), item("money", "한 사람에게 최대 24만 원을 지원해요.")]},
            {"title": N, "items": [item("users", "발달장애인과 그 가족이에요."), item("user_check", "지적장애나 자폐성 장애가 해당돼요.")]},
            {"title": A, "items": [item("money", "비용이 더 들면 넘는 돈은 직접 내요."), item("user_check", "비슷한 서비스를 받고 있으면 못 받아요.")]},
        ],
        "steps": ["모집하는 때를 확인해요."],
    },
    "disabled_childcare_fee": {
        "groups": [
            {"title": W, "items": [item("money", "장애아반은 한 달 634,000원 지원 기준이에요.")]},
            {"title": N, "items": [item("user_check", "학교에 가기 전 12살 이하 장애아동이에요."), item("money", "집 소득은 따지지 않아요.")]},
            {"title": A, "items": [item("building", "어린이집에 다니거나 곧 다닐 때 신청해요."), item("user_check", "학교에 다니기 시작하면 대상에서 빠져요.")]},
        ],
    },
    "special_education": {
        "groups": [
            {"title": W, "items": [item("sun", "검사로 특수교육이 필요한지 살펴봐요."), item("building", "일반학급, 특수학급, 특수학교 중 배울 곳을 정해요.")]},
            {"title": N, "items": [item("user_check", "3살부터 17살까지 대상이 될 수 있어요."), item("user_check", "장애 등록을 안 해도 신청할 수 있어요.")]},
            {"title": A, "items": [item("user_check", "3살 미만도 검사를 받은 뒤 무료로 배울 수 있어요."), item("hourglass", "처리 기간은 44일이에요.")]},
        ],
    },
    "special_transport": {
        "summary": "걷기 힘든 장애인이 타는 전용 차량이에요.",
        "groups": [
            {"title": W, "items": [item("money", "10km까지 기본요금은 1,700원이에요."), item("money", "더 가면 5km마다 100원이 더 붙어요.")]},
            {"title": N, "items": [item("user_check", "걷기 어려운 심한 장애가 있는 분이에요."), item("user_check", "지적장애나 자폐성 장애는 걷기 어렵다는 서류가 필요해요.")]},
            {"title": A, "items": [item("users", "보호자와 함께 타는 것이 원칙이에요."), item("user_check", "휠체어를 쓰면 종합병원 진단서로 대상이 될 수 있어요.")]},
        ],
        "steps": ["용인시 교통약자이동지원센터에 가요.", "신청서와 동의서를 내요.", "이용 전에 대상자로 등록해요."],
    },
    "parking_sticker": {
        "groups": [
            {"title": W, "items": [item("building", "장애인이 쓰는 자동차에 붙이는 표지예요.")]},
            {"title": N, "items": [item("user_check", "내 이름이나 같이 사는 가족 이름의 차가 있어야 해요."), item("money", "집 소득은 따지지 않아요.")]},
            {"title": A, "items": [item("user_check", "주차 가능 표지는 걷기 어려운 장애가 기준이에요."), item("building", "차를 팔거나 폐차하면 표지를 바로 돌려줘야 해요.")]},
        ],
        "steps": ["주소지 읍면동 행정복지센터에 가요.", "장애인 등록 및 서비스 신청서를 내요.", "차를 1년 이상 빌려 쓰면 계약서 사본도 내요."],
    },
    "pension_basic": {
        "groups": [
            {"title": W, "items": [item("money", "2026년 최고 금액은 한 달 349,700원이에요."), item("money", "부부가 함께 받으면 금액이 줄 수 있어요.")]},
            {"title": N, "items": [item("user_check", "18살부터 64살까지인 등록 중증장애인이에요."), item("money", "소득과 재산이 많으면 못 받을 수 있어요.")]},
            {"title": A, "items": [item("hourglass", "신청 뒤 처리 기간은 30일이에요."), item("calendar", "65살이 되면 기초연금을 따로 신청해요.")]},
        ],
        "steps": ["주소지 행정복지센터나 복지로에서 신청해요.", "신청서와 소득 재산 신고서를 내요."],
        "terms": [{"word": "소득인정액", "meaning": "소득과 재산을 함께 따져 만든 금액이에요."},
                  {"word": "중증장애인", "meaning": "연금 기준으로 장애가 심하다고 판정된 사람이에요."}],
    },
    "disability_allowance": {
        "groups": [
            {"title": W, "items": [item("money", "기초생활 수급자와 차상위계층은 한 달 6만 원이에요."), item("money", "보장시설 수급자는 한 달 3만 원이에요.")]},
            {"title": N, "items": [item("money", "기초생활 수급자나 차상위계층이어야 해요."), item("user_check", "장애인으로 등록했고 중증이 아니어야 해요.")]},
            {"title": A, "items": [item("user_check", "중증이면 장애인연금을 신청해요."), item("user_check", "18살에서 20살은 학교에 다니면 대상이 아니에요.")]},
        ],
    },
}


# 검수 2 (뒤 15개). 재료로 풀이를 확인할 수 없는 terms(차상위계층, 구간, 거치 등)는 넣지 않고 지운다
FIXES.update({
    "pension_additional": {
        "groups": [
            {"title": W, "items": [item("money", "집 소득 수준과 나이에 따라 금액이 달라요."), item("money", "65살 전에는 0원부터 9만원까지예요.")]},
            {"title": N, "items": [item("user_check", "18살부터 장애인연금을 받는 중증장애인이에요."), item("money", "기초생활 수급자이거나 소득 기준에 맞아야 해요.")]},
            {"title": A, "items": [item("money", "65살부터는 0원부터 439,700원까지예요."), item("user_check", "장애인연금을 신청하면 함께 결정돼요.")]},
        ],
    },
    "yongin_activity_extra": {
        "groups": [
            {"title": W, "items": [item("ticket", "한 달에 1~13구간은 20시간, 14구간은 10시간이에요."), item("money", "본인 돈은 내지 않아요.")]},
            {"title": N, "items": [item("user_check", "6살부터 64살까지, 활동지원 1~14구간 대상자예요."), item("money", "소득 기준은 없어요.")]},
            {"title": A, "items": [item("hourglass", "나라와 도 시간을 먼저 쓰고, 남은 시간은 그달에 없어져요."), item("hourglass", "나라와 도 시간이 480시간을 넘으면 시 지원은 없어요.")]},
        ],
        "steps": ["주소지 읍면동 행정복지센터에 가요.", "용인시 추가 신청서를 내요."],
    },
    "assistive_device": {
        "groups": [
            {"title": W, "items": [item("home", "품목에 따라 보조기기를 물건으로 지원해요.")]},
            {"title": N, "items": [item("user_check", "국민연금공단 검사에서 적격이어야 해요."), item("money", "기초생활수급자나 차상위계층이어야 해요.")]},
            {"title": A, "items": [item("hourglass", "예산이 다 쓰이면 신청이 끝날 수 있어요."), item("building", "같은 품목 지원을 받고 있다면 먼저 확인해요.")]},
        ],
        "terms": [],
    },
    "medicaid_assistive_device": {
        "groups": [
            {"title": W, "items": [item("money", "나라가 정한 금액과 산 금액 중 낮은 쪽을 지원해요.")]},
            {"title": N, "items": [item("user_check", "의료급여를 받으며 장애인으로 등록한 사람이에요."), item("user_check", "나이, 장애 종류나 정도에 따른 조건은 없어요.")]},
            {"title": A, "items": [item("money", "소모품과 수리비는 지원하지 않아요."), item("money", "전동휠체어와 전동스쿠터 전지는 예외예요.")]},
        ],
    },
    "medical_cost": {
        "summary": "병원에서 내는 본인부담금을 지원하는 서비스예요.",
        "groups": [
            {"title": W, "items": [item("money", "건강보험이 되는 진료의 본인부담금만 지원해요.")]},
            {"title": N, "items": [item("user_check", "의료급여 2종을 받는 등록장애인이에요."),
                                    item("user_check", "건강보험 경감 대상인 만성질환자와 18살 미만도 돼요.")]},
            {"title": A, "items": [item("user_check", "일할 수 있는 사람이 있는 기초생활 가구도 돼요."), item("users", "가족이어도 비장애인은 대상이 아니에요.")]},
        ],
        "steps": ["주소지 읍면동 행정복지센터에 가요.", "병원비에서 미리 빼 줘요."],
    },
    "diagnosis_fee": {
        "groups": [
            {"title": W, "items": [item("money", "지적, 자폐성, 정신장애 진단서는 최대 4만 원이에요."),
                                    item("money", "그 밖의 진단서는 1만 5천 원, 검사는 10만 원까지예요.")]},
            {"title": N, "items": [item("user_check", "새로 등록하는 수급자는 나이와 관계없이 대상이에요."),
                                    item("user_check", "장애 정도를 다시 심사받으면 수급자나 차상위계층이에요.")]},
            {"title": A, "items": [item("user_check", "조정 신청과 이의 신청은 지원하지 않아요."), item("money", "정한 금액보다 더 든 돈은 본인이 내요.")]},
        ],
        "terms": [{"word": "수급자", "meaning": "생계, 의료, 주거, 교육급여 중 하나를 받는 사람이에요."}],
    },
    "yongin_diagnosis_extra": {
        "groups": [
            {"title": W, "items": [item("money", "진단비는 나라 지원 뒤 최대 5만 원까지예요."), item("money", "검사비는 나라 지원 뒤 최대 10만 원까지예요.")]},
            {"title": N, "items": [item("user_check", "진단비는 생계급여나 의료급여를 받는 사람이에요."),
                                    item("user_check", "검사비는 다시 심사받는 수급자와 차상위계층이에요.")]},
            {"title": A, "items": [item("hourglass", "나라 지원을 받은 뒤 신청해요."), item("user_check", "새로 등록할 때는 검사비를 지원하지 않아요.")]},
        ],
        "terms": [],
    },
    "self_reliance_loan": {
        "groups": [
            {"title": W, "items": [item("sun", "일이나 이동, 치료에 필요한 돈을 빌려요."), item("money", "보증인 없이 빌리면 가구당 1,200만 원까지예요.")]},
            {"title": N, "items": [item("user_check", "장애인으로 등록한 19살부터 신청할 수 있어요."), item("money", "집 소득이 정해진 범위 안에 있어야 해요.")]},
            {"title": A, "items": [item("money", "이자는 연 2%예요."), item("calendar", "5년 뒤부터 5년 동안 나눠 갚아요.")]},
        ],
        "terms": [],
    },
    "transport_discounts": {
        "groups": [
            {"title": W, "items": [item("ticket", "심한 장애인은 철도 요금이 50% 할인돼요."),
                                    item("ticket", "심하지 않으면 30%예요. 토, 일, 공휴일은 빼요.")]},
            {"title": N, "items": [item("user_check", "등록한 장애인이면 나이와 종류에 제한이 없어요.")]},
            {"title": A, "items": [item("building", "항공 할인은 항공사마다 달라요."), item("building", "고속도로 할인은 확인이 필요해요.")]},
        ],
        "steps": ["예매나 탑승 때 장애인등록증을 보여 줘요.", "항공은 항공사에 할인 여부를 물어요.", "고속도로는 주소지 행정복지센터에 신청해요."],
    },
    "car_excise_tax": {
        "groups": [
            {"title": W, "items": [item("money", "개별소비세를 최대 500만 원까지 면제해요.")]},
            {"title": N, "items": [item("user_check", "장애의 정도가 심한 사람이 대상이에요."), item("users", "차는 본인이나 같이 사는 가족 이름이어야 해요.")]},
            {"title": A, "items": [item("user_check", "장애인 한 명에 승용차 한 대만 대상이에요."), item("money", "5년 안에 팔면 세금을 다시 내야 할 수 있어요.")]},
        ],
    },
    "health_insurance_reduction": {
        "groups": [
            {"title": W, "items": [item("money", "직장 건강보험이 아닌 집의 보험료를 줄여요."), item("money", "정도가 심하면 30%, 심하지 않으면 20% 줄여요.")]},
            {"title": N, "items": [item("user_check", "장애인 등록을 한 사람이 있는 집이에요."), item("user_check", "나이는 따지지 않아요.")]},
            {"title": A, "items": [item("money", "다른 보험료 할인과 함께 받으면 적용 방법을 확인해요.")]},
        ],
        "steps": ["공단 지사나 행정복지센터에 문의해요.", "전화는 1577-1000이에요."],
    },
    "job_program": {
        "summary": "등록한 장애인에게 일자리를 주는 사업이에요.",
        "groups": [
            {"title": W, "items": [item("sun", "행정도우미나 복지서비스 일을 해요."), item("money", "일자리 종류마다 월급이 달라요.")]},
            {"title": N, "items": [item("user_check", "18살부터 신청할 수 있어요."), item("user_check", "일하고 있지 않은 등록 장애인이에요.")]},
            {"title": A, "items": [item("calendar", "모집은 해마다 해요. 시 공고를 확인해요."),
                                    item("user_check", "회사에 다니거나 사업자면 뽑히지 않을 수 있어요.")]},
        ],
        "steps": ["행정복지센터나 시청, 구청에 가요.", "참여신청서를 내요.", "개인정보 안내 서식도 내요."],
    },
    "employment_package": {
        "groups": [
            {"title": W, "items": [item("sun", "취업 준비를 단계별로 도와줘요."), item("money", "단계에 따라 참여수당을 받을 수 있어요.")]},
            {"title": N, "items": [item("user_check", "취업을 원하는 장애인이에요."), item("user_check", "18살부터 69살까지, 소득과 장애 정도는 관계없어요.")]},
            {"title": A, "items": [item("user_check", "69살이 넘으면 상담과 평가로 참여를 정해요."), item("money", "집 소득이 적으면 별도 서비스가 있어요.")]},
        ],
    },
    "yongin_mobility_repair": {
        "groups": [
            {"title": W, "items": [item("money", "수동휠체어와 전동휠체어, 전동스쿠터 수리비예요."),
                                    item("money", "수급자와 차상위 계층은 전액, 그 밖은 50%예요.")]},
            {"title": N, "items": [item("user_check", "용인시에 등록된 장애인이 대상이에요."), item("user_check", "나이, 장애 종류나 정도에 따른 제한은 없어요.")]},
            {"title": A, "items": [item("calendar", "한 해에 전액은 20만원까지, 50%는 10만원까지예요."),
                                    item("building", "나라에서 받는 수리 지원과 관련해 확인이 필요해요.")]},
        ],
    },
    "housing_modification": {
        "groups": [
            {"title": W, "items": [item("home", "집 안 편의시설을 고치거나 설치해요."), item("home", "집 상태와 장애 종류, 본인 의견을 살펴 시설을 골라요.")]},
            {"title": N, "items": [item("user_check", "나이와 장애 종류, 정도에 제한이 없어요."), item("money", "집 소득이 지난해 도시 집들의 월평균 이하여야 해요.")]},
            {"title": A, "items": [item("money", "예산보다 신청이 많으면 소득 기준으로 먼저 뽑아요."), item("calendar", "해마다 예산으로 하는 사업이라 신청 때를 확인해요.")]},
        ],
    },
})


def main():
    data = json.loads(OUT.read_text(encoding="utf-8"))
    material = load_material()
    reviewed = sys.argv[1:]  # 검수 통과(고칠 것 없음)로 확인된 id
    failed = {}
    for sid, fix in FIXES.items():
        entry = data["services"][sid]
        entry.update(fix)
        problems = check(entry, json.dumps(material[sid], ensure_ascii=False))
        problems += [f"{g['title']} {len(g['items'])}줄" for g in entry["groups"] if len(g["items"]) > 2]
        entry["problems"] = problems
        entry["reviewed"] = not problems
        if problems:
            failed[sid] = problems
    for sid in reviewed:
        data["services"][sid]["reviewed"] = True
    OUT.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    done = sum(1 for v in data["services"].values() if v["reviewed"])
    print(f"반영 {len(FIXES)}개, 검수 완료 {done}/{len(data['services'])}")
    for sid, problems in failed.items():
        print(" ", sid, problems)


if __name__ == "__main__":
    main()
