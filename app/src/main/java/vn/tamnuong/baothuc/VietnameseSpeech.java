package vn.tamnuong.baothuc;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class VietnameseSpeech {
    private final Context context;
    private final TextToSpeech engine;
    private boolean ready;
    private boolean closed;
    private String pending;

    VietnameseSpeech(Context owner, boolean alarm, Runnable onReady) {
        context = owner.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        engine = new TextToSpeech(context, status -> main.post(() -> initialize(status, alarm, onReady)));
    }

    private void initialize(int status, boolean alarm, Runnable onReady) {
        if (closed) return;
        if (status == TextToSpeech.SUCCESS) {
            ready = engine.setLanguage(new Locale("vi", "VN")) >= TextToSpeech.LANG_AVAILABLE;
            engine.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(alarm ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        }
        if (pending != null) speak(pending);
        pending = null;
        onReady.run();
    }

    List<Voice> voices() {
        List<Voice> available = new ArrayList<>();
        if (ready && engine.getVoices() != null) {
            for (Voice voice : engine.getVoices()) {
                if ("vi".equals(voice.getLocale().getLanguage()) && !voice.isNetworkConnectionRequired()) {
                    available.add(voice);
                }
            }
        }
        java.util.Collections.sort(available, (left, right) -> {
            int quality = Integer.compare(right.getQuality(), left.getQuality());
            return quality != 0 ? quality : left.getName().compareTo(right.getName());
        });
        return available;
    }

    String selected() {
        return Sched.prefs(context).getString("vietnameseVoice", "");
    }

    void select(String name) {
        Sched.prefs(context).edit().putString("vietnameseVoice", name).apply();
    }

    boolean speak(String sentence) {
        List<Voice> available = voices();
        if (available.isEmpty()) return false;
        Voice chosen = available.get(0);
        for (Voice voice : available) {
            if (voice.getName().equals(selected())) {
                chosen = voice;
                break;
            }
        }
        return engine.setVoice(chosen) == TextToSpeech.SUCCESS
                && engine.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "tam-nuong") == TextToSpeech.SUCCESS;
    }

    void speakWhenReady(String sentence) {
        if (ready) speak(sentence);
        else pending = sentence;
    }

    void close() {
        closed = true;
        pending = null;
        engine.stop();
        engine.shutdown();
    }

    static String today() {
        return Lunar.isTN(Lunar.vnCal(System.currentTimeMillis()))
                ? "Hôm nay là ngày Tam Nương."
                : "Hôm nay không phải ngày Tam Nương.";
    }
}
