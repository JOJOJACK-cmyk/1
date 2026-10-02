#!/usr/bin/env python3
"""개발·시연용 *가짜* 인허가 CSV를 만든다. 실제 김포 데이터가 아니다.

실제 지방행정 인허가 CSV와 같은 컬럼명을 쓰므로, 진짜 파일을 받기 전에 적재·집계·화면을 확인할 수 있다.
시드가 고정이라 다시 돌려도 같은 파일이 나온다.

    python3 scripts/generate_sample_data.py > sample-data/gimpo-sample.csv
"""
import csv
import math
import random
import sys
from datetime import date, timedelta

random.seed(20260930)
END = date(2026, 9, 30)  # 샘플 데이터의 마지막 날짜 (실행일과 무관하게 고정)

# (읍면동, 기준 비중, 급증이 시작되는 해, 급증이 끝나는 해)  -- 신도시는 뒤늦게 급증하도록 가정
DISTRICTS = [
    ("구래동", 10, 2012, 2018), ("장기동", 10, 2012, 2018), ("운양동", 8, 2012, 2019),
    ("마산동", 5, 2014, 2020), ("풍무동", 8, 2014, 2021), ("사우동", 9, None, None),
    ("북변동", 7, None, None), ("걸포동", 5, 2013, 2019), ("감정동", 4, None, None),
    ("고촌읍", 6, 2012, 2018), ("통진읍", 4, None, None), ("양촌읍", 6, 2013, 2019),
    ("대곶면", 2, None, None), ("월곶면", 2, None, None), ("하성면", 2, None, None),
]

# (업태구분명, 비중, 영업 기간 중앙값(년))
CATEGORIES = [
    ("한식", 34, 6.0), ("커피숍", 8, 3.5), ("까페", 4, 3.5), ("호프/통닭", 10, 4.5),
    ("분식", 8, 4.0), ("김밥(도시락)", 3, 4.0), ("중국식", 5, 7.0), ("일식", 6, 4.5),
    ("경양식", 3, 5.0), ("제과점영업", 4, 6.5), ("정종/대포집/소주방", 9, 3.0),
    ("기타", 6, 4.0),
]

STREETS = ["김포한강9로", "김포한강4로", "김포대로", "승가로", "태장로", "봉화로", "양촌로", "고촌로", "통진로"]


def weight(start, ramp_from, ramp_to):
    """해당 연도에 이 동네에서 얼마나 개업이 몰리는지 (1.0 = 평상시)."""
    year = start.year
    if ramp_from is None:
        return 1.0
    if year < ramp_from:
        return 0.15
    if year >= ramp_to:
        return 1.8
    return 0.15 + (1.8 - 0.15) * (year - ramp_from) / (ramp_to - ramp_from)


def fmt(d):
    return d.isoformat() if d else ""


def main():
    w = csv.writer(sys.stdout, lineterminator="\n")
    w.writerow(["번호", "개방서비스명", "관리번호", "인허가일자", "영업상태명", "폐업일자",
                "소재지전체주소", "도로명전체주소", "사업장명", "업태구분명"])
    n = 0
    start_day = date(2005, 1, 1)
    span = (END - start_day).days
    rows = []
    while len(rows) < 3600:
        d = random.choices(DISTRICTS, weights=[x[1] for x in DISTRICTS])[0]
        licensed = start_day + timedelta(days=random.randrange(span))
        if random.random() > min(1.0, weight(licensed, d[2], d[3]) / 1.8):
            continue
        cat = random.choices(CATEGORIES, weights=[x[1] for x in CATEGORIES])[0]
        # 영업 기간은 로그정규 분포: 오래 버티는 곳은 오래, 금방 닫는 곳은 금방
        lifetime_days = int(random.lognormvariate(math.log(cat[2] * 365), 0.8))
        closed = licensed + timedelta(days=lifetime_days)
        if closed >= END:
            closed = None
        rows.append((d[0], cat[0], licensed, closed))

    rows.sort(key=lambda r: r[2])
    for i, (dong, cat, licensed, closed) in enumerate(rows, 1):
        n += 1
        status = "폐업" if closed else "영업/정상"
        jibun = f"경기도 김포시 {dong} {random.randint(1, 1999)}-{random.randint(1, 30)}"
        road = f"경기도 김포시 {random.choice(STREETS)} {random.randint(1, 300)}"
        w.writerow([n, "일반음식점", f"SAMPLE-{i:05d}", fmt(licensed), status, fmt(closed),
                    jibun, road, f"샘플식당 {i:04d}", cat])

    # 적재 시 걸러지는 행도 몇 개 섞어 둔다 (지역 불일치 / 날짜 누락)
    for k in range(5):
        n += 1
        w.writerow([n, "일반음식점", f"SAMPLE-X{k}", "2020-01-01", "영업/정상", "",
                    "서울특별시 강서구 화곡동 1-1", "", f"타지역샘플 {k}", "한식"])
    for k in range(3):
        n += 1
        w.writerow([n, "일반음식점", f"SAMPLE-Y{k}", "", "영업/정상", "",
                    "경기도 김포시 사우동 1-1", "", f"날짜없음샘플 {k}", "한식"])


if __name__ == "__main__":
    main()
