package vn.tamnuong.baothuc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Nhận báo thức từ hệ thống, đổ chuông rồi hẹn lần kế tiếp. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (Sched.ACTION_STOP.equals(action)) {
            Sched.stop(c);
            return;
        }
        String kind = intent.getStringExtra("kind");
        if (kind == null) kind = "daily";
        if ("test".equals(kind)) {
            if (!Sched.test(c)) return;
            Sched.scheduleTest(c);
        } else if ("daily".equals(kind)) {
            Sched.scheduleDaily(c);
            if (!Sched.daily(c)) return;
        }
        String[] m = Sched.message(kind);
        if (m != null) Sched.ring(c, m[0], m[1]);
    }
}
