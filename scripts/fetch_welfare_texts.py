"""규칙표 서비스의 조건 원문을 사회보장정보원 API에서 받아 welfare_texts.json으로 저장한다.

카드 근거(/basis)는 이 파일의 문장만 인용한다. 규칙표를 바꾸면 다시 돌린다.
    DATA_GO_KR_KEY=... python scripts/fetch_welfare_texts.py
키는 data.go.kr 일반 인증키(Decoding). 중앙부처(15090532), 지자체(15108347) 복지서비스 API 활용신청 필요.
"""
import datetime
import json
import os
import pathlib
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

DATA = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/data"
APIS = [  # (주소, 복지로 링크의 wlfareInfoReldBztpCd)
    ("https://apis.data.go.kr/B554287/NationalWelfareInformationsV001/NationalWelfaredetailedV001", "01"),
    ("https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations/LcgvWelfaredetailed", "02"),
]
# 원문 필드 -> 화면에 보일 구분 이름. 중앙부처는 tgtrDtlCn, 지자체는 sprtTrgtCn
FIELDS = {"tgtrDtlCn": "지원대상", "sprtTrgtCn": "지원대상", "slctCritCn": "선정기준", "alwServCn": "지원내용"}
SKIP = {"지원대상의 내용을 참고해주시기 바랍니다."}


def fetch(key, serv_id):
    for url, bztp in APIS:
        query = urllib.parse.urlencode({"serviceKey": key, "callTp": "D", "servId": serv_id})
        root = ET.fromstring(urllib.request.urlopen(f"{url}?{query}", timeout=20).read())
        if root.findtext("servNm"):
            return root, bztp
    return None, None


def main():
    key = os.environ["DATA_GO_KR_KEY"]
    ids = []
    for name in ("rules_child_v2.json", "rules_adult_v2.json"):
        for service in json.loads((DATA / name).read_text(encoding="utf-8"))["services"]:
            ids += [i for i in service["welfare_ids"] if i not in ids]

    services = {}
    for serv_id in ids:
        root, bztp = fetch(key, serv_id)
        if root is None:
            print("원문 없음", serv_id)
            continue
        lines = []
        for field, section in FIELDS.items():
            for line in re.split(r"[\r\n]+", root.findtext(field) or ""):
                line = line.strip()
                if line and line not in SKIP:
                    lines.append({"section": section, "text": line})
        services[serv_id] = {
            "name": root.findtext("servNm"),
            "source": f"https://www.bokjiro.go.kr/ssis-tbu/twataa/wlfareInfo/moveTWAT52011M.do?wlfareInfoId={serv_id}&wlfareInfoReldBztpCd={bztp}",
            "lines": lines,
        }
        print(serv_id, root.findtext("servNm"), len(lines))

    out = {"fetched_at": datetime.date.today().isoformat(), "services": services}
    (DATA / "welfare_texts.json").write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
