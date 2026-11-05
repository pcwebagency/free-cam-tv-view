package it.pcwebagency.telecamere;

import android.app.Application;
import android.util.Log;

/**
 * Rete di sicurezza: un errore nel thread di riproduzione di una singola telecamera non deve
 * chiudere l'app. Il watchdog di {@link LiveView} riavvia poi il flusso.
 */
public class App extends Application {
    private static App instance;

    /** Testo localizzato per le classi senza Context (strings.xml: inglese, values-it: italiano). */
    static String s(int id, Object... args) {
        return instance.getString(id, args);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Thread.UncaughtExceptionHandler system = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            if (thread.getName().startsWith("ExoPlayer:")) {
                Log.e("FreeCamTVView", "Errore nel player ignorato", e);
                return;
            }
            if (system != null) system.uncaughtException(thread, e);
        });
    }
}
