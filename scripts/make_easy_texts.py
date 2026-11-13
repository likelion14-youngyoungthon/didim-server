"""쉬운 말 문장 만들기 (judge 응답 easy).

규칙표(rules_*_v2.json)와 복지 원문(welfare_texts.json)만 재료로 OpenAI에 쉬운 문장을 받아
src/main/resources/data/easy_texts.json에 저장한다. 사람이 검수한 뒤 reviewed를 true로 바꾼다.

AI가 낸 문장은 코드로 다시 확인한다.
- 그룹 제목, 아이콘은 정해진 값만
- 숫자는 재료에 있는 숫자만 (금액, 나이를 지어내지 못하게)
- 한 줄 40자 이하, 가운뎃점 금지

실행: LLM_API_KEY가 환경변수나 .env에 있어야 한다. 키는 출력하지 않는다.
    python scripts/make_easy_texts.py dev_rehab_voucher dd_day_activity ...   (id를 안 주면 data/demo_personas.json 인물 결과 전체)
"""
import json
import os
import re
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data"
OUT = DATA / "easy_texts.json"
MODEL = os.environ.get("LLM_MODEL") or "gpt-6-luna"

TITLES = ["무엇을 해요", "누가 받아요", "알아 두세요"]
ICONS = ["sun", "ticket", "user_check", "money", "hourglass", "users", "home", "calendar", "building"]
MAX_LEN = 40

SYSTEM = """너는 발달장애인과 그 가족이 읽을 복지 안내를 쉬운 말로 쓰는 사람이다.
재료(규칙표와 공식 원문)에 있는 내용만 쓴다. 재료에 없는 금액, 나이, 기간, 조건을 만들지 않는다.
문장 규칙:
- 한 문장에 한 가지만. 40자 이하. "~해요", "~예요"로 끝낸다.
- 어려운 한자어, 법률 용어를 쓰지 않는다. "만 18세 이상" 대신 "18살부터", "기준중위소득" 같은 말은 쓰지 않고 "집 소득이 많지 않으면"처럼 쓴다.
- 가운뎃점(·)을 쓰지 않는다. 받을 수 있다고 단정하지 않는다.
구성:
- summary: 이 서비스가 무엇인지 한 문장. 예: "낮에 사람들과 함께 활동하는 서비스예요."
- groups: 순서대로 "무엇을 해요"(1~2줄), "누가 받아요"(1~2줄), "알아 두세요"(0~2줄, 주의할 점이 없으면 빼기).
  줄마다 아이콘 하나: sun(활동, 하루), ticket(이용권, 바우처), user_check(대상, 나이), money(돈, 소득, 금액), hourglass(기다림, 기간), users(가족, 함께), home(집, 시설), calendar(날짜, 마감, 매달), building(기관, 창구)
- steps: 신청하러 가서 할 일 1~3개. 예: "행정복지센터에 가요.", "신청서를 내요."
- terms: 화면에 남은 어려운 말 0~2개와 쉬운 풀이. 예: {"word": "이용권", "meaning": "정해진 곳에서 돈 대신 쓰는 표예요."}
예시(발달장애인 주간활동서비스):
summary "낮에 사람들과 함께 활동하는 서비스예요."
무엇을 해요: sun "낮에 여러 사람과 함께 활동해요." / ticket "한 달에 132시간이나 176시간 써요."
누가 받아요: user_check "18살부터 64살까지 받아요." / money "돈을 얼마나 벌어도 받을 수 있어요."
알아 두세요: hourglass "자리가 없으면 기다려야 해요."
"""

SCHEMA = {
    "type": "object",
    "properties": {
        "summary": {"type": "string"},
        "groups": {"type": "array", "items": {"type": "object", "properties": {
            "title": {"type": "string", "enum": TITLES},
            "items": {"type": "array", "items": {"type": "object", "properties": {
                "icon": {"type": "string", "enum": ICONS}, "text": {"type": "string"}},
                "required": ["icon", "text"]}}},
            "required": ["title", "items"]}},
        "steps": {"type": "array", "items": {"type": "string"}},
        "terms": {"type": "array", "items": {"type": "object", "properties": {
            "word": {"type": "string"}, "meaning": {"type": "string"}}, "required": ["word", "meaning"]}},
    },
    "required": ["summary", "groups", "steps", "terms"],
}

# 재료로 넘길 규칙표 필드 (판정용 내부 값은 뺀다)
RULE_FIELDS = ["name", "age_min", "age_max", "age_note", "disability_types", "severity", "severity_note", "income",
               "office_note", "form_names", "documents", "processing_days", "deadline", "amount_note", "note",
               "exclusive_note", "situation_effects"]


def api_key():
    key = os.environ.get("LLM_API_KEY", "")
    env = ROOT / ".env"
    if not key and env.exists():
        for line in env.read_text(encoding="utf-8").splitlines():
            if line.startswith("LLM_API_KEY="):
                key = line.split("=", 1)[1].strip()
    if not key:
        sys.exit("LLM_API_KEY가 없습니다.")
    return key


def load_material():
    rules = {}
    for name in ["rules_child_v2.json", "rules_adult_v2.json"]:
        for rule in json.loads((DATA / name).read_text(encoding="utf-8"))["services"]:
            rules.setdefault(rule["id"], []).append({k: rule[k] for k in RULE_FIELDS if rule.get(k) not in (None, [], "")})
            rules[rule["id"]][-1]["_welfare_ids"] = rule.get("welfare_ids", [])
    texts = json.loads((DATA / "welfare_texts.json").read_text(encoding="utf-8"))["services"]
    material = {}
    for sid, entries in rules.items():
        lines = [line["text"] for entry in entries for wid in entry.pop("_welfare_ids") for line in texts.get(wid, {}).get("lines", [])]
        material[sid] = {"규칙표": entries, "공식 원문": list(dict.fromkeys(lines))}
    return material


def numbers(text):
    # 정수만 본다. 원문의 "11.27"(11월 27일)을 소수로 읽지 않게
    return set(re.findall(r"\d+", text.replace(",", "")))


def check(result, material_text):
    problems = []
    allowed = numbers(material_text)
    texts = [result["summary"]] + [i["text"] for g in result["groups"] for i in g["items"]] + result["steps"] + [t["meaning"] for t in result["terms"]]
    for text in texts:
        if len(text) > MAX_LEN:
            problems.append(f"40자 넘음: {text}")
        if "·" in text:
            problems.append(f"가운뎃점: {text}")
        if re.search(r"[一-鿿]", text):
            problems.append(f"한자: {text}")
        extra = numbers(text) - allowed
        if extra:
            problems.append(f"재료에 없는 숫자 {sorted(extra)}: {text}")
    for group in result["groups"]:
        if len(group["items"]) > 2:
            problems.append(f"{group['title']} {len(group['items'])}줄 (2줄까지)")
    titles = [g["title"] for g in result["groups"]]
    if titles[:2] != TITLES[:2] or any(t not in TITLES for t in titles):
        problems.append(f"그룹 제목 순서: {titles}")
    for term in result["terms"]:
        if term["word"] not in material_text:
            problems.append(f"재료에 없는 말 풀이: {term['word']}")
    return problems


def call(key, sid, material):
    body = {"model": MODEL, "messages": [
        {"role": "system", "content": SYSTEM},
        {"role": "user", "content": "재료:\n" + json.dumps(material, ensure_ascii=False)}],
        "response_format": {"type": "json_schema", "json_schema": {"name": "easy", "schema": SCHEMA}}}
    req = urllib.request.Request("https://api.openai.com/v1/chat/completions", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json", "Authorization": "Bearer " + key})
    with urllib.request.urlopen(req, timeout=60) as res:
        return json.loads(json.load(res)["choices"][0]["message"]["content"])


def make(key, sid, material):
    material_text = json.dumps(material, ensure_ascii=False)
    for _ in range(2):  # 검사에 걸리면 한 번 더 받는다
        result = call(key, sid, material)
        problems = check(result, material_text)
        if not problems:
            break
    # 두 번 받아도 3줄이면 앞의 2줄만 쓴다
    result["groups"] = [{**g, "items": g["items"][:2]} for g in result["groups"] if g["items"]]
    problems = [p for p in problems if "줄 (2줄까지)" not in p]
    return sid, {**result, "reviewed": False, "problems": problems}


def persona_services():
    """체험 인물 4명 결과에 보이는 서비스 id. 배포 서버 /judge를 부른다."""
    ids = []
    personas = json.loads((DATA / "demo_personas.json").read_text(encoding="utf-8"))["personas"]
    for persona in personas:
        req = urllib.request.Request("https://dojang-api.duckdns.org/api/v1/judge", data=json.dumps(persona["request"]).encode(),
                                     headers={"Content-Type": "application/json"})
        for sid, item in json.load(urllib.request.urlopen(req))["data"].items():
            if not item["hidden"] and (item["verdict"] != "not_eligible" or item.get("eligible_from_age")):
                ids.append(sid)
    return list(dict.fromkeys(ids))


def main():
    key = api_key()
    material = load_material()
    ids = sys.argv[1:] or persona_services()
    unknown = [i for i in ids if i not in material]
    if unknown:
        sys.exit(f"규칙표에 없는 id: {unknown}")
    with ThreadPoolExecutor(6) as pool:
        made = dict(pool.map(lambda sid: make(key, sid, material[sid]), ids))
    old = json.loads(OUT.read_text(encoding="utf-8"))["services"] if OUT.exists() else {}
    old.update(made)  # 검수 끝난 다른 서비스는 그대로 둔다
    OUT.write_text(json.dumps({"generated_at": str(date.today()), "model": MODEL, "services": old}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    bad = {sid: v["problems"] for sid, v in made.items() if v["problems"]}
    print(f"{len(made)}개 저장, 검사에 걸린 것 {len(bad)}개")
    for sid, problems in bad.items():
        print(" ", sid, problems)


if __name__ == "__main__":
    main()
