package vn.tamnuong.baothuc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Màn hình báo thức hiện đè lên màn hình khoá, tự bật sáng màn hình. */
public class AlarmActivity extends Activity {
    private TextView title, body;
    private LinearLayout root;
    private final Handler h = new Handler();
    private boolean dark;
    private final Runnable blink = new Runnable() {
        public void run() {
            dark = !dark;
            root.setBackgroundColor(dark ? 0xFF7A0A0F : 0xFFB8141C);
            h.postDelayed(this, 600);
        }
    };
    private final BroadcastReceiver closer = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) { finish(); }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        float dp = getResources().getDisplayMetrics().density;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding((int) (28 * dp), (int) (28 * dp), (int) (28 * dp), (int) (28 * dp));
        root.setBackgroundColor(0xFFB8141C);

        TextView bell = new TextView(this);
        bell.setText("⏰");
        bell.setTextSize(64);
        bell.setGravity(Gravity.CENTER);
        root.addView(bell);

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, (int) (16 * dp), 0, (int) (12 * dp));
        root.addView(title);

        body = new TextView(this);
        body.setTextColor(Color.WHITE);
        body.setTextSize(20);
        body.setGravity(Gravity.CENTER);
        root.addView(body);

        Button stop = new Button(this);
        stop.setText("TẮT CHUÔNG");
        stop.setTextSize(22);
        stop.setTypeface(Typeface.DEFAULT_BOLD);
        stop.setTextColor(0xFFB8141C);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(40 * dp);
        stop.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (260 * dp), (int) (68 * dp));
        lp.topMargin = (int) (40 * dp);
        root.addView(stop, lp);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Sched.stop(AlarmActivity.this);
                finish();
            }
        });

        setContentView(root);
        fill(getIntent());
        registerReceiver(closer, new IntentFilter(Sched.ACTION_CLOSE));
        h.post(blink);
    }

    private void fill(Intent i) {
        String t = i.getStringExtra("title"), bo = i.getStringExtra("body");
        title.setText(t != null ? t : "Báo ngày Tam Nương");
        body.setText(bo != null ? bo : "");
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        fill(i);
    }

    @Override
    public void onBackPressed() {
        Sched.stop(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(blink);
        try { unregisterReceiver(closer); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
