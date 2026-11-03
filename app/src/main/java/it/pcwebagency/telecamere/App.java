package it.pcwebagency.telecamere;

import android.app.Application;
import android.util.Log;

/**
 * Rete di sicurezza: un errore nel thread di riproduzione di una singola telecamera non deve
 * chiudere l'app. Il watchdog di {@link LiveView} riavvia poi il flusso.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.UncaughtExceptionHandler system = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            if (thread.getName().startsWith("ExoPlayer:")) {
                Log.e("Telecamere", "Errore nel player ignorato", e);
                return;
            }
            if (system != null) system.uncaughtException(thread, e);
        });
    }
}
