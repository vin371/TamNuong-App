"""Tạo file lịch .ics các ngày Tam Nương (âm lịch Việt Nam, UTC+7), báo 7:00 và 17:00.
Cách dùng: python3 tools/tao_lich_ics.py <file.ics> [số_năm=5]
Thuật toán âm lịch theo Hồ Ngọc Đức (dùng cho lịch Việt Nam)."""
import math, datetime, uuid

PI = math.pi
F = math.floor
TZ = 7.0

def jd_from_date(dd, mm, yy):
    a = (14 - mm) // 12; y = yy + 4800 - a; m = mm + 12 * a - 3
    jd = dd + (153 * m + 2) // 5 + 365 * y + y // 4 - y // 100 + y // 400 - 32045
    if jd < 2299161:
        jd = dd + (153 * m + 2) // 5 + 365 * y + y // 4 - 32083
    return jd

def new_moon(k):
    T = k / 1236.85; T2 = T * T; T3 = T2 * T; dr = PI / 180
    Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3
    Jd1 += 0.00033 * math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr)
    M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3
    Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3
    Fa = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3
    s = math.sin
    C1 = (0.1734 - 0.000393 * T) * s(M * dr) + 0.0021 * s(2 * dr * M)
    C1 = C1 - 0.4068 * s(Mpr * dr) + 0.0161 * s(dr * 2 * Mpr)
    C1 = C1 - 0.0004 * s(dr * 3 * Mpr)
    C1 = C1 + 0.0104 * s(dr * 2 * Fa) - 0.0051 * s(dr * (M + Mpr))
    C1 = C1 - 0.0074 * s(dr * (M - Mpr)) + 0.0004 * s(dr * (2 * Fa + M))
    C1 = C1 - 0.0004 * s(dr * (2 * Fa - M)) - 0.0006 * s(dr * (2 * Fa + Mpr))
    C1 = C1 + 0.0010 * s(dr * (2 * Fa - Mpr)) + 0.0005 * s(dr * (2 * Mpr + M))
    if T < -11:
        deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
    else:
        deltat = -0.000278 + 0.000265 * T + 0.000262 * T2
    return Jd1 + C1 - deltat

def sun_longitude(jdn):
    T = (jdn - 2451545.0) / 36525; T2 = T * T; dr = PI / 180
    M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2
    L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2
    DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * math.sin(dr * M)
    DL += (0.019993 - 0.000101 * T) * math.sin(dr * 2 * M) + 0.000290 * math.sin(dr * 3 * M)
    L = (L0 + DL) * dr
    return L - PI * 2 * F(L / (PI * 2))

def get_sun_longitude(day, tz): return F(sun_longitude(day - 0.5 - tz / 24) / PI * 6)
def get_new_moon_day(k, tz): return F(new_moon(k) + 0.5 + tz / 24)

def get_lunar_month11(yy, tz):
    off = jd_from_date(31, 12, yy) - 2415021
    k = F(off / 29.530588853)
    nm = get_new_moon_day(k, tz)
    if get_sun_longitude(nm, tz) >= 9:
        nm = get_new_moon_day(k - 1, tz)
    return nm

def get_leap_month_offset(a11, tz):
    k = F((a11 - 2415021.076998695) / 29.530588853 + 0.5)
    i = 1
    arc = get_sun_longitude(get_new_moon_day(k + i, tz), tz)
    while True:
        last = arc; i += 1
        arc = get_sun_longitude(get_new_moon_day(k + i, tz), tz)
        if not (arc != last and i < 14):
            break
    return i - 1

def solar_to_lunar(dd, mm, yy, tz=TZ):
    day = jd_from_date(dd, mm, yy)
    k = F((day - 2415021.076998695) / 29.530588853)
    ms = get_new_moon_day(k + 1, tz)
    if ms > day:
        ms = get_new_moon_day(k, tz)
    a11 = get_lunar_month11(yy, tz); b11 = a11
    if a11 >= ms:
        ly = yy; a11 = get_lunar_month11(yy - 1, tz)
    else:
        ly = yy + 1; b11 = get_lunar_month11(yy + 1, tz)
    ld = day - ms + 1
    diff = F((ms - a11) / 29)
    leap = 0; lm = diff + 11
    if b11 - a11 > 365:
        lmd = get_leap_month_offset(a11, tz)
        if diff >= lmd:
            lm = diff + 10
            if diff == lmd:
                leap = 1
    if lm > 12: lm -= 12
    if lm >= 11 and diff < 4: ly -= 1
    return ld, lm, ly, leap

TAM_NUONG = {3, 7, 13, 18, 22, 27}
THU = ["Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"]

def tam_nuong_days(start, end):
    d = start
    while d <= end:
        ld, lm, ly, leap = solar_to_lunar(d.day, d.month, d.year)
        if ld in TAM_NUONG:
            yield d, ld, lm, ly, leap
        d += datetime.timedelta(days=1)

def fold(line):
    if len(line.encode()) <= 75: return line
    parts, cur, lim = [], b"", 75
    for ch in line:
        e = ch.encode()
        if len(cur) + len(e) > lim: parts.append(cur.decode()); cur = b""; lim = 74
        cur += e
    parts.append(cur.decode()); return "\r\n ".join(parts)

def build_ics(start, end, path):
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//TamNuong//VI", "CALSCALE:GREGORIAN",
             "METHOD:PUBLISH", "X-WR-CALNAME:Ngày Tam Nương", "X-WR-TIMEZONE:Asia/Ho_Chi_Minh",
             "BEGIN:VTIMEZONE", "TZID:Asia/Ho_Chi_Minh", "BEGIN:STANDARD", "DTSTART:19700101T000000",
             "TZOFFSETFROM:+0700", "TZOFFSETTO:+0700", "TZNAME:ICT", "END:STANDARD", "END:VTIMEZONE"]
    n = 0
    for d, ld, lm, ly, leap in tam_nuong_days(start, end):
        n += 1
        thang = f"{lm}{' nhuận' if leap else ''}"
        desc = f"{THU[d.weekday()]} {d:%d/%m/%Y} là ngày {ld} tháng {thang} âm lịch – NGÀY TAM NƯƠNG (3, 7, 13, 18, 22, 27 âm lịch)."
        for hh, buoi in (("07", "sáng"), ("17", "chiều")):
            lines += ["BEGIN:VEVENT", f"UID:tamnuong-{d:%Y%m%d}-{hh}@local", f"DTSTAMP:{stamp}",
                      f"DTSTART;TZID=Asia/Ho_Chi_Minh:{d:%Y%m%d}T{hh}0000",
                      f"DTEND;TZID=Asia/Ho_Chi_Minh:{d:%Y%m%d}T{hh}1000",
                      f"SUMMARY:⚠️ Hôm nay ngày Tam Nương ({ld}/{thang} ÂL)",
                      f"DESCRIPTION:{desc}", "TRANSP:TRANSPARENT",
                      "BEGIN:VALARM", "ACTION:DISPLAY", f"DESCRIPTION:Hôm nay là ngày Tam Nương (báo {buoi})",
                      "TRIGGER:PT0M", "END:VALARM", "END:VEVENT"]
    lines.append("END:VCALENDAR")
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write("\r\n".join(fold(l) for l in lines) + "\r\n")
    return n

if __name__ == "__main__":
    import sys
    years = int(sys.argv[2]) if len(sys.argv) > 2 else 5
    start = (datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=7)).date()
    end = datetime.date(start.year + years, 12, 31)
    n = build_ics(start, end, sys.argv[1])
    print(f"{n} ngày Tam Nương từ {start} đến {end} -> {sys.argv[1]}")
