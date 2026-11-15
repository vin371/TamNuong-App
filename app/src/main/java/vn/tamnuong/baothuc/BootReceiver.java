package vn.tamnuong.baothuc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Hẹn lại báo thức sau khi khởi động lại máy, cập nhật ứng dụng hoặc đổi giờ. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Sched.rescheduleAll(c);
    }
}
