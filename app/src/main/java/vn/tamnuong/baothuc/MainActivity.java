package vn.tamnuong.baothuc;

import android.app.Activity;
import android.app.AlertDialog;
import android.speech.tts.Voice;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private static final int RED = 0xFFB8141C, INK = 0xFF1D1919, MUTE = 0xFF6B6262, LINE = 0xFFECE6E6;
    private float dp;
    private TextView wd, day, my, lunar, stamp, nextDaily, nextTest, checks, list;
    private Switch swDaily, swTest;
    private VietnameseSpeech speech;
    private final Handler h = new Handler();
    private final Runnable tick = new Runnable() {
        public void run() {
            refreshTimes();
            h.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        Sched.ensureChannel(this);
        Sched.rescheduleAll(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(RED);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(16), px(24), px(16), px(32));
        sv.addView(col);

        TextView h1 = text("Báo ngày Tam Nương", 28, Color.WHITE, true);
        col.addView(h1);

        // Tờ lịch hôm nay
        LinearLayout sheet = card(col, 18);
        sheet.setGravity(Gravity.CENTER_HORIZONTAL);
        wd = text("", 17, MUTE, true);
        day = text("", 96, RED, true);
        my = text("", 16, INK, true);
        lunar = text("", 18, INK, false);
        stamp = text("", 20, RED, true);
        stamp.setPadding(px(16), px(8), px(16), px(8));
        for (TextView t : new TextView[]{wd, day, my, lunar, stamp}) {
            t.setGravity(Gravity.CENTER);
            sheet.addView(t);
        }
        ((LinearLayout.LayoutParams) stamp.getLayoutParams()).topMargin = px(12);

        LinearLayout audio = card(col, 14);
        audio.addView(text("Giọng đọc tiếng Việt", 18, INK, true));
        Button choose = button("Chọn giọng đọc", false);
        Button preview = button("Đọc trạng thái hôm nay", false);
        audio.addView(choose);
        audio.addView(preview);
        choose.setEnabled(false);
        preview.setEnabled(false);
        speech = new VietnameseSpeech(this, false, () -> {
            choose.setEnabled(true);
            preview.setEnabled(true);
        });
        choose.setOnClickListener(v -> chooseVoice());
        preview.setOnClickListener(v -> previewVoice());

        // Công tắc báo
        LinearLayout ctl = card(col, 14);
        swDaily = new Switch(this);
        swDaily.setText("Báo 7:00 và 17:00");
        style(swDaily);
        ctl.addView(swDaily);
        nextDaily = text("", 14, MUTE, false);
        ctl.addView(nextDaily);
        divider(ctl);
        swTest = new Switch(this);
        swTest.setText("Chế độ test: báo mỗi 2 phút");
        style(swTest);
        ctl.addView(swTest);
        nextTest = text("", 14, MUTE, false);
        ctl.addView(nextTest);
        divider(ctl);
        Button once = button("Báo thử sau 10 giây (bấm rồi tắt màn hình)", false);
        ctl.addView(once);

        swDaily.setChecked(Sched.daily(this));
        swTest.setChecked(Sched.test(this));
        swDaily.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton v, boolean on) {
                Sched.prefs(MainActivity.this).edit().putBoolean("daily", on).apply();
                Sched.scheduleDaily(MainActivity.this);
                refreshTimes();
            }
        });
        swTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton v, boolean on) {
                Sched.prefs(MainActivity.this).edit().putBoolean("test", on).apply();
                Sched.scheduleTest(MainActivity.this);
                refreshTimes();
            }
        });
        once.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Sched.scheduleOnce(MainActivity.this, 10000);
                Toast.makeText(MainActivity.this, "Sẽ kêu sau 10 giây. Bạn có thể tắt màn hình ngay.", Toast.LENGTH_LONG).show();
            }
        });

        // Kiểm tra cài đặt để báo chắc chắn
        LinearLayout set = card(col, 14);
        set.addView(text("Cài đặt để chuông kêu chắc chắn", 18, INK, true));
        checks = text("", 15, INK, false);
        set.addView(checks);
        Button bNotif = button("Mở cài đặt thông báo và âm chuông", false);
        Button bFull = button("Cho phép hiện toàn màn hình khi khoá máy", false);
        Button bBatt = button("Không giới hạn pin cho ứng dụng", false);
        set.addView(bNotif);
        if (Build.VERSION.SDK_INT >= 34) set.addView(bFull);
        if (Build.VERSION.SDK_INT >= 23) set.addView(bBatt);
        set.addView(text("Điện thoại Xiaomi, Oppo, Vivo, Realme: vào Cài đặt > Ứng dụng > Báo Tam Nương, bật \"Tự khởi động\" và chọn \"Không giới hạn\" pin.", 14, MUTE, false));
        bNotif.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = Build.VERSION.SDK_INT >= 26
                        ? new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                        : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                open(i);
            }
        });
        bFull.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT", Uri.parse("package:" + getPackageName())));
            }
        });
        bBatt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
            }
        });

        // Các ngày sắp tới
        LinearLayout up = card(col, 14);
        up.addView(text("Các ngày Tam Nương sắp tới", 18, INK, true));
        list = text("", 16, INK, false);
        list.setLineSpacing(0, 1.35f);
        up.addView(list);
        TextView foot = text("Tam Nương: mùng 3, 7, 13, 18, 22, 27 mỗi tháng âm lịch (âm lịch Việt Nam, UTC+7). Chuông dùng bộ hẹn giờ báo thức của hệ thống nên vẫn kêu khi tắt màn hình hoặc khởi động lại máy.", 13, Color.WHITE, false);
        foot.setPadding(px(4), px(14), px(4), 0);
        col.addView(foot);

        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
        h.post(tick);
    }

    @Override
    protected void onPause() {
        h.removeCallbacks(tick);
        super.onPause();
    }

    private void chooseVoice() {
        List<Voice> voices = speech.voices();
        if (voices.isEmpty()) {
            previewVoice();
            return;
        }
        String[] names = new String[voices.size() + 1];
        names[0] = "Tự động (tiếng Việt)";
        int selected = 0;
        for (int i = 0; i < voices.size(); i++) {
            names[i + 1] = voices.get(i).getName();
            if (names[i + 1].equals(speech.selected())) selected = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle("Chọn giọng đọc tiếng Việt")
                .setSingleChoiceItems(names, selected, (dialog, index) -> {
                    speech.select(index == 0 ? "" : names[index]);
                    dialog.dismiss();
                    previewVoice();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void previewVoice() {
        if (!speech.speak(VietnameseSpeech.today())) {
            Toast.makeText(this, "Cài dữ liệu giọng tiếng Việt trong mục Chuyển văn bản thành giọng nói của Android.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (speech != null) speech.close();
        super.onDestroy();
    }

    private void refreshAll() {
        Calendar t = Lunar.vnCal(System.currentTimeMillis());
        int[] L = Lunar.lunarOf(t);
        boolean tn = Lunar.isTN(L[0]);
        wd.setText(Lunar.thu(t));
        day.setText(String.valueOf(t.get(Calendar.DAY_OF_MONTH)));
        my.setText("Tháng " + (t.get(Calendar.MONTH) + 1) + " năm " + t.get(Calendar.YEAR));
        lunar.setText("Âm lịch: " + Lunar.ngay(L[0]) + " " + Lunar.thang(L) + " năm " + L[2]);
        stamp.setText(tn ? "HÔM NAY LÀ NGÀY TAM NƯƠNG" : "Không phải ngày Tam Nương");
        stamp.setTextColor(tn ? RED : MUTE);
        GradientDrawable st = new GradientDrawable();
        st.setCornerRadius(px(10));
        st.setStroke(px(3), tn ? RED : LINE);
        stamp.setBackground(st);

        StringBuilder sb = new StringBuilder();
        List<Calendar> ds = Lunar.upcoming(Lunar.plusDays(t, 1).getTimeInMillis(), 10);
        for (Calendar d : ds) {
            int[] x = Lunar.lunarOf(d);
            sb.append(Lunar.thu(d)).append(" ").append(Lunar.dmy(d)).append("  ·  ").append(Lunar.ngay(x[0])).append(" ").append(Lunar.thang(x)).append("\n");
        }
        list.setText(sb.toString().trim());

        StringBuilder c = new StringBuilder();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean notifOk = Build.VERSION.SDK_INT < 24 || nm.areNotificationsEnabled();
        c.append(notifOk ? "✅ Thông báo: đang bật" : "❌ Thông báo: đang TẮT – chuông sẽ không kêu").append("\n");
        if (Build.VERSION.SDK_INT >= 34) {
            boolean full = true;
            try { full = (Boolean) NotificationManager.class.getMethod("canUseFullScreenIntent").invoke(nm); } catch (Exception ignored) {}
            c.append(full ? "✅ Hiện toàn màn hình khi khoá máy: được phép" : "⚠️ Hiện toàn màn hình: chưa cho phép (chỉ kêu chuông + thông báo)").append("\n");
        }
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean batt = pm.isIgnoringBatteryOptimizations(getPackageName());
            c.append(batt ? "✅ Pin: không giới hạn" : "⚠️ Pin: đang bị tối ưu – nên tắt để báo đúng giờ");
        }
        checks.setText(c.toString().trim());
        refreshTimes();
    }

    private void refreshTimes() {
        long nd = Sched.nextDaily(this), nt = Sched.nextTest(this), now = System.currentTimeMillis();
        if (!Sched.daily(this)) nextDaily.setText("Đang tắt");
        else if (nd > 0) {
            Calendar d = Lunar.vnCal(nd);
            nextDaily.setText("Lần báo kế tiếp: " + Lunar.thu(d) + " " + Lunar.dmy(d) + " lúc " + Lunar.hm(nd));
        } else nextDaily.setText("Chưa hẹn được giờ báo");
        if (!Sched.test(this)) nextTest.setText("Đang tắt");
        else if (nt > now) {
            long s = (nt - now + 999) / 1000;
            nextTest.setText("Đang bật · kêu lần tới sau " + (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60) + " (lúc " + Lunar.hm(nt) + ")");
        } else nextTest.setText("Đang bật · sắp kêu…");
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {}
        }
    }

    private int px(float v) { return (int) (v * dp + 0.5f); }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private LinearLayout card(LinearLayout parent, int pad) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(px(pad + 4), px(pad), px(pad + 4), px(pad));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(px(14));
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(16);
        parent.addView(c, lp);
        return c;
    }

    private void style(Switch s) {
        s.setTextSize(18);
        s.setTextColor(INK);
        s.setTypeface(Typeface.DEFAULT_BOLD);
        s.setMinHeight(px(52));
        s.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void divider(LinearLayout p) {
        View v = new View(this);
        v.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(1));
        lp.topMargin = px(12);
        lp.bottomMargin = px(8);
        p.addView(v, lp);
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(primary ? Color.WHITE : INK);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? INK : 0xFFF6F2F2);
        bg.setCornerRadius(px(12));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(52));
        lp.topMargin = px(10);
        b.setLayoutParams(lp);
        return b;
    }
}
