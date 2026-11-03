package it.pcwebagency.telecamere;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Aggiornamenti dalle Releases di GitHub: legge l'ultima release, confronta la versione,
 * scarica l'APK allegato e avvia l'installazione (l'utente conferma con OK).
 */
public final class Updater {
    private static final String PREFS = "updater";
    private static final String KEY_AUTO = "autoCheck";
    private static final String KEY_LAST = "lastCheck";
    private static final long AUTO_INTERVAL_MS = 12 * 60 * 60 * 1000L;

    public static class Release {
        public String version = "";
        public String notes = "";
        public String apkUrl = "";
    }

    private Updater() {}

    public static boolean isAutoCheck(Context ctx) {
        return prefs(ctx).getBoolean(KEY_AUTO, true);
    }

    public static void setAutoCheck(Context ctx, boolean on) {
        prefs(ctx).edit().putBoolean(KEY_AUTO, on).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Controllo automatico all'avvio (al massimo ogni 12 ore): propone l'aggiornamento solo se esiste. */
    public static void autoCheck(Activity activity) {
        if (!isAutoCheck(activity)) return;
        long now = System.currentTimeMillis();
        if (now - prefs(activity).getLong(KEY_LAST, 0) < AUTO_INTERVAL_MS) return;
        prefs(activity).edit().putLong(KEY_LAST, now).apply();
        check(activity, false);
    }

    /** @param manual se true mostra anche "nessun aggiornamento" e gli errori */
    public static void check(Activity activity, boolean manual) {
        if (manual) Toast.makeText(activity, "Controllo aggiornamenti…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Release r = latest();
                boolean newer = compare(r.version, BuildConfig.VERSION_NAME) > 0;
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) return;
                    if (newer && !r.apkUrl.isEmpty()) offer(activity, r);
                    else if (manual) info(activity, "Nessun aggiornamento",
                            "Stai usando l'ultima versione (" + BuildConfig.VERSION_NAME + ").");
                });
            } catch (Exception e) {
                if (manual) activity.runOnUiThread(() ->
                        info(activity, "Controllo non riuscito", e.getMessage()));
            }
        }).start();
    }

    static Release latest() throws Exception {
        if (BuildConfig.GITHUB_REPO.isEmpty()) throw new IOException("Repository GitHub non configurato");
        HttpURLConnection c = open("https://api.github.com/repos/" + BuildConfig.GITHUB_REPO + "/releases/latest");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        int code = c.getResponseCode();
        if (code == 404) throw new IOException("Nessuna release pubblicata su GitHub");
        if (code != 200) throw new IOException("GitHub HTTP " + code);
        JSONObject json = new JSONObject(read(c.getInputStream()));
        Release r = new Release();
        r.version = json.optString("tag_name").replaceFirst("^[vV]", "");
        r.notes = json.optString("body");
        JSONArray assets = json.optJSONArray("assets");
        for (int i = 0; assets != null && i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.optString("name").endsWith(".apk")) {
                r.apkUrl = a.optString("browser_download_url");
                break;
            }
        }
        return r;
    }

    private static void offer(Activity activity, Release r) {
        String notes = r.notes.trim().isEmpty() ? "" : "\n\n" + r.notes.trim();
        new ModernDialog(activity)
                .setTitle("Nuova versione " + r.version + " disponibile")
                .setMessage("Versione installata: " + BuildConfig.VERSION_NAME + notes)
                .setPositiveButton("Aggiorna ora", (d, w) -> download(activity, r))
                .setNegativeButton("Più tardi", null)
                .show();
    }

    private static void download(Activity activity, Release r) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            new ModernDialog(activity)
                    .setTitle("Permesso necessario")
                    .setMessage("Per installare l'aggiornamento consenti a «Free Cam TV View» di installare app "
                            + "(Fire TV: Impostazioni → La mia Fire TV → Opzioni sviluppatore → Installa app sconosciute), "
                            + "poi riprova.")
                    .setPositiveButton("Apri impostazioni", (d, w) -> {
                        try {
                            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + activity.getPackageName())));
                        } catch (Exception e) {
                            Toast.makeText(activity, "Apri il menu dalle impostazioni della TV", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Annulla", null)
                    .show();
            return;
        }
        android.app.Dialog wait = new ModernDialog(activity)
                .setMessage("Download della versione " + r.version + "…").setCancelable(false).show();
        new Thread(() -> {
            try {
                File dir = new File(activity.getCacheDir(), "updates");
                if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cartella non disponibile");
                File apk = new File(dir, "FreeCamTVView-" + r.version + ".apk");
                HttpURLConnection c = open(r.apkUrl);
                if (c.getResponseCode() != 200) throw new IOException("Download HTTP " + c.getResponseCode());
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buf = new byte[64 * 1024];
                    for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                }
                activity.runOnUiThread(() -> {
                    wait.dismiss();
                    install(activity, apk);
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    wait.dismiss();
                    info(activity, "Download non riuscito", e.getMessage());
                });
            }
        }).start();
    }

    private static void install(Activity activity, File apk) {
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updates", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(i);
    }

    private static void info(Activity activity, String title, String msg) {
        new ModernDialog(activity).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show();
    }

    /** Confronto numerico di versioni "1.2.10" > "1.2.9". */
    static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int num(String s) {
        String d = s.replaceAll("\\D.*", "");
        return d.isEmpty() ? 0 : Integer.parseInt(d);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "FreeCamTVView/" + BuildConfig.VERSION_NAME);
        c.setInstanceFollowRedirects(true);
        return c;
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = is.read(buf)) > 0; ) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
}
