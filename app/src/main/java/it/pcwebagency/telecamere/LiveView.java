package it.pcwebagency.telecamere;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Tracks;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

/** Riquadro con il flusso live di una telecamera, che si riconnette da solo se cade. */
@OptIn(markerClass = UnstableApi.class)
public class LiveView {
    private static final int RETRY_MS = 5000;
    /** Con password errata si riprova di rado: gli NVR Dahua bloccano l'account dopo pochi tentativi. */
    private static final int AUTH_RETRY_MS = 120000;
    private static final int WATCHDOG_MS = 15000;

    public final FrameLayout root;
    private final Context ctx;
    private final PlayerView playerView;
    private final TextView status;
    private final TextView label;
    private final TextView quality;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Camera cam;
    private final boolean hd;
    private ExoPlayer player;
    private long lastPosition = -1;
    /**
     * Ripiego quando il decoder della TV non riesce a riprodurre il flusso HD (es. 4K oltre il
     * limite 1080p): 0 flusso principale, 1 flusso extra Dahua (subtype 2), 2 LD.
     */
    private int hdStage = 0;
    private boolean forceLd = false;

    /** Riavvia il flusso se non è partito o se l'immagine è ferma da {@link #WATCHDOG_MS}. */
    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;
            long pos = player.getCurrentPosition();
            if (player.getPlaybackState() != Player.STATE_READY || pos == lastPosition) {
                retry(App.s(R.string.live_no_signal));
                return;
            }
            lastPosition = pos;
            handler.postDelayed(this, WATCHDOG_MS);
        }
    };

    public LiveView(Context ctx, Camera cam, boolean hd) {
        this.ctx = ctx;
        this.cam = cam;
        this.hd = hd;
        root = new FrameLayout(ctx);
        root.setBackgroundColor(Color.BLACK);

        AspectRatioFrameLayout frame = (AspectRatioFrameLayout)
                LayoutInflater.from(ctx).inflate(R.layout.player_view, root, false);
        frame.setAspectRatio(16f / 9f);
        root.addView(frame);
        playerView = frame.findViewById(R.id.player);

        label = new TextView(ctx);
        label.setText(cam.label());
        label.setTextColor(Color.WHITE);
        label.setTextSize(hd ? 20 : 15);
        label.setBackgroundColor(0x99000000);
        label.setPadding(16, 6, 16, 6);
        root.addView(label, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));

        quality = new TextView(ctx);
        quality.setText(isHd() ? "HD" : "LD");
        quality.setTextColor(isHd() ? 0xFF81C784 : Color.LTGRAY);
        quality.setTextSize(hd ? 14 : 11);
        quality.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        quality.setBackgroundColor(0x99000000);
        quality.setPadding(12, 4, 12, 4);
        root.addView(quality, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END));

        status = new TextView(ctx);
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(hd ? 20 : 14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(24, 24, 24, 24);
        root.addView(status, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
    }

    public void setLabelVisible(boolean visible) {
        label.setVisibility(visible ? View.VISIBLE : View.GONE);
        quality.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Flusso principale (HD) o secondario (LD), secondo la scelta per griglia o schermo intero. */
    public boolean isHd() {
        return (hd ? cam.fullHd : cam.highQuality) && !forceLd;
    }

    /** Etichetta in basso a destra, es. il comando dell'apriporta. */
    public TextView addHint(String text) {
        TextView hint = new TextView(ctx);
        hint.setText(text);
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(hd ? 18 : 13);
        hint.setBackgroundColor(0x99000000);
        hint.setPadding(16, 6, 16, 6);
        root.addView(hint, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END));
        return hint;
    }

    public void start() {
        stop();
        if (hdStage == 0) {
            status.setText(R.string.live_connecting);
            status.setVisibility(View.VISIBLE);
        }
        DefaultLoadControl lowLatency = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 2000, 250, 250)
                .build();
        // Fallback al decoder software: i chip Amlogic hanno poche istanze del decoder HEVC
        // hardware e con più flussi H.265 in griglia quelli in più non si inizializzano.
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(ctx).setEnableDecoderFallback(true);
        player = new ExoPlayer.Builder(ctx, renderers).setLoadControl(lowLatency).build();
        player.setVolume(hd ? 1f : 0f);
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && hdStage == 0) status.setVisibility(View.GONE);
                else if (state == Player.STATE_READY) {
                    if (hdStage == 1) status.setText(R.string.live_hd_extra);
                    handler.postDelayed(() -> status.setVisibility(View.GONE), 4000);
                }
                else if (state == Player.STATE_ENDED) retry(App.s(R.string.live_stream_ended));
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                // un flusso oltre il limite del decoder hardware finirebbe sul decoder software,
                // troppo lento per il live: si cambia flusso subito, senza attendere un errore
                if (!isHd() || !cam.customUrl.isEmpty()) return;
                for (Tracks.Group g : tracks.getGroups()) {
                    if (g.getType() != C.TRACK_TYPE_VIDEO) continue;
                    Format f = g.getTrackFormat(0);
                    if (f.sampleMimeType != null && f.width > 0
                            && !Decoders.hardwareSupports(f.sampleMimeType, f.width, f.height)) {
                        handler.post(LiveView.this::fallbackHd);
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException e) {
                boolean decoder = e.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
                        || e.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                        || e.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES;
                if (isHd() && cam.customUrl.isEmpty() && (decoder || hdStage == 1)) {
                    fallbackHd();
                    return;
                }
                retry(describe(e));
            }
        });
        RtspMediaSource source = new RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(8000)
                .createMediaSource(MediaItem.fromUri(cam.url(!isHd() ? 1 : hdStage == 1 ? 2 : 0)));
        player.setMediaSource(source);
        player.setPlayWhenReady(true);
        player.prepare();
        lastPosition = -1;
        handler.postDelayed(watchdog, WATCHDOG_MS);
    }

    /** Flusso HD non riproducibile: prima il flusso extra Dahua, poi LD. */
    private void fallbackHd() {
        if (hdStage == 0) {
            hdStage = 1;
            status.setText(R.string.live_hd_too_big);
        } else {
            hdStage = 2;
            forceLd = true;
            quality.setText("LD");
            quality.setTextColor(Ui.WARN);
            status.setText(R.string.live_hd_unsupported);
        }
        status.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::start, 300);
    }

    private static String describe(PlaybackException e) {
        String msg = "";
        for (Throwable t = e; t != null; t = t.getCause()) if (t.getMessage() != null) msg = t.getMessage();
        for (Throwable t = e; t != null; t = t.getCause())
            if (t instanceof java.net.UnknownHostException) return App.s(R.string.live_host_not_found);
        if (msg.contains(" 401")) return App.s(R.string.live_auth_error);
        if (msg.contains(" 400") || msg.contains(" 404") || msg.contains("Invalid RTSP response"))
            return App.s(R.string.live_rtsp_unsupported);
        if (e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                || e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
            return App.s(R.string.live_unreachable);
        return msg.isEmpty() ? e.getErrorCodeName() : msg;
    }

    private void retry(String reason) {
        boolean auth = reason.equals(App.s(R.string.live_auth_error));
        status.setText(reason + App.s(auth ? R.string.live_retry_auth : R.string.live_retry));
        status.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::start, auth ? AUTH_RETRY_MS : RETRY_MS);
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }
}
