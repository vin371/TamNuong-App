package vn.tamnuong.baothuc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;

import org.junit.Test;

public class LunarTest {

    /** Thời điểm theo giờ Việt Nam. month: 1-12 */
    private static long vn(int y, int m, int d, int hh, int mm) {
        Calendar c = Calendar.getInstance(Lunar.VN);
        c.clear();
        c.set(y, m - 1, d, hh, mm, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void ngay15_11_2026_laMung7Thang10() {
        assertArrayEquals(new int[]{7, 10, 2026, 0}, Lunar.solar2lunar(15, 11, 2026));
        assertTrue(Lunar.isTN(7));
    }

    @Test
    public void tetNguyenDan() {
        assertArrayEquals(new int[]{1, 1, 2026, 0}, Lunar.solar2lunar(17, 2, 2026));
        assertArrayEquals(new int[]{1, 1, 2027, 0}, Lunar.solar2lunar(6, 2, 2027));
        // Lịch Việt Nam (UTC+7) ăn Tết 02/02/2030, sớm hơn lịch Trung Quốc một ngày
        assertArrayEquals(new int[]{1, 1, 2030, 0}, Lunar.solar2lunar(2, 2, 2030));
    }

    @Test
    public void thangNhuan() {
        assertArrayEquals(new int[]{1, 6, 2025, 1}, Lunar.solar2lunar(25, 7, 2025));
        assertArrayEquals(new int[]{1, 5, 2028, 1}, Lunar.solar2lunar(23, 6, 2028));
    }

    @Test
    public void co307NgayTamNuongDenHet2030() {
        Calendar c = Lunar.vnCal(vn(2026, 11, 15, 12, 0));
        long end = vn(2030, 12, 31, 12, 0);
        int n = 0;
        while (c.getTimeInMillis() <= end) {
            if (Lunar.isTN(c)) n++;
            c = Lunar.plusDays(c, 1);
        }
        assertEquals(307, n);
    }

    @Test
    public void gioBaoKeTiep() {
        // 15/11/2026 là ngày Tam Nương
        assertEquals(vn(2026, 11, 15, 7, 0), Lunar.nextDaily(vn(2026, 11, 15, 6, 0)));
        assertEquals(vn(2026, 11, 15, 17, 0), Lunar.nextDaily(vn(2026, 11, 15, 7, 30)));
        // 21/11 là ngày Tam Nương nên 17:00 ngày 20/11 báo trước
        assertEquals(vn(2026, 11, 20, 17, 0), Lunar.nextDaily(vn(2026, 11, 15, 17, 30)));
        assertEquals(vn(2026, 11, 21, 7, 0), Lunar.nextDaily(vn(2026, 11, 20, 17, 0)));
    }

    @Test
    public void ngayThuongKhongBao() {
        assertFalse(Lunar.isTN(Lunar.vnCal(vn(2026, 11, 16, 12, 0))));
    }

    @Test
    public void danhSachSapToi() {
        List<Calendar> up = Lunar.upcoming(vn(2026, 11, 16, 12, 0), 3);
        assertEquals("21/11/2026", Lunar.dmy(up.get(0)));
        assertEquals("26/11/2026", Lunar.dmy(up.get(1)));
        assertEquals("30/11/2026", Lunar.dmy(up.get(2)));
    }

    @Test
    public void cachGoiNgay() {
        assertEquals("mùng 7", Lunar.ngay(7));
        assertEquals("ngày 13", Lunar.ngay(13));
        assertEquals("Chủ nhật 15/11/2026, mùng 7 tháng Mười âm lịch", Lunar.describe(Lunar.vnCal(vn(2026, 11, 15, 9, 0))));
    }
}
