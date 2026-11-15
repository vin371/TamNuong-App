package vn.tamnuong.baothuc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Âm lịch Việt Nam (thuật toán Hồ Ngọc Đức, múi giờ +7) và lịch báo Tam Nương. */
public final class Lunar {
    public static final int[] TN = {3, 7, 13, 18, 22, 27};
    public static final TimeZone VN = TimeZone.getTimeZone("GMT+07:00");
    private static final double TZ = 7.0;
    private static final String[] THU = {"Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    private static final String[] THANG = {"", "Giêng", "Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy", "Tám", "Chín", "Mười", "Mười Một", "Chạp"};

    private Lunar() {}

    private static int INT(double d) { return (int) Math.floor(d); }

    static int jdFromDate(int dd, int mm, int yy) {
        int a = INT((14 - mm) / 12.0), y = yy + 4800 - a, m = mm + 12 * a - 3;
        int jd = dd + INT((153 * m + 2) / 5.0) + 365 * y + INT(y / 4.0) - INT(y / 100.0) + INT(y / 400.0) - 32045;
        if (jd < 2299161) jd = dd + INT((153 * m + 2) / 5.0) + 365 * y + INT(y / 4.0) - 32083;
        return jd;
    }

    static double newMoon(int k) {
        double T = k / 1236.85, T2 = T * T, T3 = T2 * T, dr = Math.PI / 180;
        double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        Jd1 = Jd1 + 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
        C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
        C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
        C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
        C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
        C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
        C1 = C1 + 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
        double deltat = T < -11
                ? 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
                : -0.000278 + 0.000265 * T + 0.000262 * T2;
        return Jd1 + C1 - deltat;
    }

    static double sunLongitude(double jdn) {
        double T = (jdn - 2451545.0) / 36525, T2 = T * T, dr = Math.PI / 180;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
        DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290 * Math.sin(dr * 3 * M);
        double L = (L0 + DL) * dr;
        return L - Math.PI * 2 * INT(L / (Math.PI * 2));
    }

    static int getSunLongitude(int day, double tz) { return INT(sunLongitude(day - 0.5 - tz / 24) / Math.PI * 6); }
    static int getNewMoonDay(int k, double tz) { return INT(newMoon(k) + 0.5 + tz / 24); }

    static int getLunarMonth11(int yy, double tz) {
        double off = jdFromDate(31, 12, yy) - 2415021;
        int k = INT(off / 29.530588853);
        int nm = getNewMoonDay(k, tz);
        if (getSunLongitude(nm, tz) >= 9) nm = getNewMoonDay(k - 1, tz);
        return nm;
    }

    static int getLeapMonthOffset(int a11, double tz) {
        int k = INT((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int i = 1, last;
        int arc = getSunLongitude(getNewMoonDay(k + i, tz), tz);
        do {
            last = arc;
            i++;
            arc = getSunLongitude(getNewMoonDay(k + i, tz), tz);
        } while (arc != last && i < 14);
        return i - 1;
    }

    /** Trả về {ngày, tháng, năm, nhuận(0/1)} âm lịch. */
    public static int[] solar2lunar(int dd, int mm, int yy) {
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = INT((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = getNewMoonDay(k + 1, TZ);
        if (monthStart > dayNumber) monthStart = getNewMoonDay(k, TZ);
        int a11 = getLunarMonth11(yy, TZ), b11 = a11, lunarYear;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = getLunarMonth11(yy - 1, TZ);
        } else {
            lunarYear = yy + 1;
            b11 = getLunarMonth11(yy + 1, TZ);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = INT((monthStart - a11) / 29.0);
        int lunarLeap = 0, lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapMonthDiff = getLeapMonthOffset(a11, TZ);
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) lunarLeap = 1;
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12;
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1;
        return new int[]{lunarDay, lunarMonth, lunarYear, lunarLeap};
    }

    public static boolean isTN(int lunarDay) {
        for (int x : TN) if (x == lunarDay) return true;
        return false;
    }

    public static Calendar vnCal(long ms) {
        Calendar c = Calendar.getInstance(VN);
        c.setTimeInMillis(ms);
        return c;
    }

    public static int[] lunarOf(Calendar c) {
        return solar2lunar(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }

    public static boolean isTN(Calendar c) { return isTN(lunarOf(c)[0]); }

    public static Calendar plusDays(Calendar c, int n) {
        Calendar x = (Calendar) c.clone();
        x.add(Calendar.DATE, n);
        return x;
    }

    public static String thu(Calendar c) { return THU[c.get(Calendar.DAY_OF_WEEK) - 1]; }

    public static String dmy(Calendar c) {
        return String.format(Locale.US, "%02d/%02d/%d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
    }

    public static String hm(long ms) {
        Calendar c = vnCal(ms);
        return String.format(Locale.US, "%d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static String ngay(int d) { return (d <= 10 ? "mùng " : "ngày ") + d; }

    public static String thang(int[] L) { return "tháng " + THANG[L[1]] + (L[3] == 1 ? " nhuận" : ""); }

    /** "Chủ nhật 15/11/2026, mùng 7 tháng Mười âm lịch" */
    public static String describe(Calendar c) {
        int[] L = lunarOf(c);
        return thu(c) + " " + dmy(c) + ", " + ngay(L[0]) + " " + thang(L) + " âm lịch";
    }

    private static long at(Calendar day, int hour) {
        Calendar c = (Calendar) day.clone();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Giờ báo kế tiếp sau "after": 7:00 ngày Tam Nương; 17:00 ngày Tam Nương hoặc hôm trước ngày Tam Nương. */
    public static long nextDaily(long after) {
        Calendar d = vnCal(after);
        boolean tnToday = isTN(d);
        for (int i = 0; i < 400; i++) {
            Calendar tom = plusDays(d, 1);
            boolean tnTom = isTN(tom);
            if (tnToday) {
                long t7 = at(d, 7);
                if (t7 > after) return t7;
            }
            if (tnToday || tnTom) {
                long t17 = at(d, 17);
                if (t17 > after) return t17;
            }
            d = tom;
            tnToday = tnTom;
        }
        return -1;
    }

    public static List<Calendar> upcoming(long from, int n) {
        List<Calendar> r = new ArrayList<Calendar>();
        Calendar d = vnCal(from);
        for (int i = 0; i < 400 && r.size() < n; i++) {
            if (isTN(d)) r.add((Calendar) d.clone());
            d = plusDays(d, 1);
        }
        return r;
    }
}
