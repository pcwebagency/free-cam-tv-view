package it.pcwebagency.telecamere;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.util.List;

/**
 * Una telecamera a schermo intero in HD; ◀ ▶ passano alla precedente/successiva.
 * Sui videocitofoni OK apre la conferma dell'apriporta.
 */
public class PlayerActivity extends Activity {
    public static final String EXTRA_INDEX = "index";

    private List<Camera> cams;
    private int index;
    private LiveView live;
    private android.view.View hint;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable hideHint = () -> {
        if (hint != null) hint.setVisibility(android.view.View.GONE);
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cams = CameraStore.filter(CameraStore.load(this), CameraStore.selectedGroup(this));
        index = getIntent().getIntExtra(EXTRA_INDEX, 0);
        if (cams.isEmpty()) finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!cams.isEmpty()) showCamera();
    }

    /** L'indicazione dei comandi sparisce dopo 5 secondi e ricompare a ogni tasto. */
    private void showHint() {
        if (hint != null) hint.setVisibility(android.view.View.VISIBLE);
        handler.removeCallbacks(hideHint);
        handler.postDelayed(hideHint, 5000);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) showHint();
        return super.dispatchKeyEvent(e);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(hideHint);
        if (live != null) live.stop();
    }

    private void showCamera() {
        if (live != null) live.stop();
        index = (index + cams.size()) % cams.size();
        Camera cam = cams.get(index);
        live = new LiveView(this, cam, true);
        live.setLabelVisible(CameraStore.showNames(this));
        hint = live.addHint(getString(live.isHd() ? R.string.player_to_ld : R.string.player_to_hd) + (cam.door ? getString(R.string.player_door) : ""));
        setContentView(live.root);
        live.start();
        showHint();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            Camera cam = cams.get(index);
            cam.fullHd = !cam.fullHd;
            CameraStore.updateQuality(this, cam);
            showCamera();
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) && cams.get(index).door) {
            DoorOpener.ask(this, cams.get(index));
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            index++;
            showCamera();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            index--;
            showCamera();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }
}
