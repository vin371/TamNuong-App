package vn.tamnuong.baothuc;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.Calendar;

/** Hẹn giờ bằng AlarmManager.setAlarmClock (kêu cả khi tắt màn hình / chế độ ngủ) và đổ chuông. */
public final class Sched {
    static final String CHANNEL = "tn_alarm_v1";
    static final int NOTIF_ID = 7;
    static final String ACTION_ALARM = "vn.tamnuong.baothuc.ALARM";
    static final String ACTION_STOP = "vn.tamnuong.baothuc.STOP";
    static final String ACTION_CLOSE = "vn.tamnuong.baothuc.CLOSE";
    static final long TEST_EVERY = 2 * 60 * 1000L;

    private Sched() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("tn", Context.MODE_PRIVATE); }
    static boolean daily(Context c) { return prefs(c).getBoolean("daily", true); }
    static boolean test(Context c) { return prefs(c).getBoolean("test", false); }
    static long nextDaily(Context c) { return prefs(c).getLong("nextDaily", -1); }
    static long nextTest(Context c) { return prefs(c).getLong("nextTest", -1); }

    static int piFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

    private static PendingIntent alarmPi(Context c, String kind, int req) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_ALARM + "." + kind).putExtra("kind", kind);
        return PendingIntent.getBroadcast(c, req, i, piFlags());
    }

    private static void setAt(Context c, long when, PendingIntent op) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent show = PendingIntent.getActivity(c, 9, new Intent(c, MainActivity.class), piFlags());
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), op);
    }

    private static void cancel(Context c, PendingIntent op) {
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(op);
    }

    static long scheduleDaily(Context c) {
        PendingIntent op = alarmPi(c, "daily", 1);
        cancel(c, op);
        long t = -1;
        if (daily(c)) {
            t = Lunar.nextDaily(System.currentTimeMillis() + 1000);
            if (t > 0) setAt(c, t, op);
        }
        prefs(c).edit().putLong("nextDaily", t).apply();
        return t;
    }

    static long scheduleTest(Context c) {
        PendingIntent op = alarmPi(c, "test", 2);
        cancel(c, op);
        long t = -1;
        if (test(c)) {
            t = System.currentTimeMillis() + TEST_EVERY;
            setAt(c, t, op);
        }
        prefs(c).edit().putLong("nextTest", t).apply();
        return t;
    }

    static long scheduleOnce(Context c, long delayMs) {
        long t = System.currentTimeMillis() + delayMs;
        setAt(c, t, alarmPi(c, "once", 3));
        return t;
    }

    static void rescheduleAll(Context c) {
        scheduleDaily(c);
        scheduleTest(c);
    }

    static Uri alarmSound() {
        Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (u == null) u = Settings.System.DEFAULT_NOTIFICATION_URI;
        return u;
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Chuông báo Tam Nương", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Chuông báo thức ngày Tam Nương và chế độ test");
        ch.setSound(alarmSound(), new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
        ch.setBypassDnd(true);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    /** Lời báo cho thời điểm hiện tại. kind: daily | test | once */
    static String[] message(String kind) {
        Calendar today = Lunar.vnCal(System.currentTimeMillis());
        boolean tn = Lunar.isTN(today);
        Calendar tom = Lunar.plusDays(today, 1);
        boolean tnTom = Lunar.isTN(tom);
        String body = Lunar.describe(today) + ".";
        if (tnTom) body += " Ngày mai (" + Lunar.dmy(tom) + ") là ngày Tam Nương.";
        String title;
        if ("daily".equals(kind)) {
            if (tn) title = "Hôm nay là ngày Tam Nương";
            else if (tnTom) title = "Ngày mai là ngày Tam Nương";
            else return null;
        } else {
            title = "TEST – " + (tn ? "Hôm nay là ngày Tam Nương" : "Hôm nay không phải ngày Tam Nương");
        }
        return new String[]{title, body};
    }

    @SuppressWarnings("deprecation")
    static void ring(Context c, String title, String body) {
        ensureChannel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent ai = new Intent(c, AlarmActivity.class)
                .putExtra("title", title).putExtra("body", body)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent full = PendingIntent.getActivity(c, 4, ai, piFlags());
        PendingIntent stop = PendingIntent.getBroadcast(c, 5,
                new Intent(c, AlarmReceiver.class).setAction(ACTION_STOP), piFlags());

        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .setDeleteIntent(stop)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Tắt chuông", stop).build());
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_MAX)
                    .setSound(alarmSound(), AudioManager.STREAM_ALARM)
                    .setVibrate(new long[]{0, 800, 400, 800, 400, 800});
        }
        Notification n = b.build();
        n.flags |= Notification.FLAG_INSISTENT; // chuông lặp lại cho tới khi tắt
        nm.notify(NOTIF_ID, n);
        prefs(c).edit().putString("lastTitle", title).putLong("lastAt", System.currentTimeMillis()).apply();
    }

    static void stop(Context c) {
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIF_ID);
        c.sendBroadcast(new Intent(ACTION_CLOSE).setPackage(c.getPackageName()));
    }
}
