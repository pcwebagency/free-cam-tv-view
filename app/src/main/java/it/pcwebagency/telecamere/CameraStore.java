package it.pcwebagency.telecamere;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Salva l'elenco delle telecamere nelle SharedPreferences dell'app. */
public final class CameraStore {
    private static final String PREFS = "telecamere";
    private static final String KEY = "cameras";
    private static final String KEY_GROUP = "selectedGroup";
    private static final String KEY_LAYOUT = "layout";
    private static final String KEY_NAMES = "showNames";
    /** Valore del gruppo selezionato che mostra tutte le telecamere. */
    public static final String ALL = "";

    private CameraStore() {}

    public static List<Camera> load(Context ctx) {
        List<Camera> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        boolean plaintext = false;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String pw = arr.getJSONObject(i).optString("password");
                plaintext |= !pw.isEmpty() && !Crypto.isEncrypted(pw);
                list.add(Camera.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        // password salvate in chiaro dalle versioni precedenti: si cifrano subito
        if (plaintext && Crypto.isAvailable()) save(ctx, list);
        return list;
    }

    /** Nomi dei gruppi nell'ordine in cui compaiono. */
    public static List<String> groups(List<Camera> list) {
        Set<String> g = new LinkedHashSet<>();
        for (Camera c : list) g.add(c.group);
        return new ArrayList<>(g);
    }

    public static List<Camera> filter(List<Camera> list, String group) {
        if (group.equals(ALL)) return list;
        List<Camera> out = new ArrayList<>();
        for (Camera c : list) if (c.group.equals(group)) out.add(c);
        return out;
    }

    public static String selectedGroup(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_GROUP, ALL);
    }

    public static void setSelectedGroup(Context ctx, String group) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_GROUP, group).apply();
    }

    /** Telecamere per pagina nella griglia: 1, 4, 9 o 16. */
    public static int layout(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_LAYOUT, 4);
    }

    public static void setLayout(Context ctx, int perPage) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_LAYOUT, perPage).apply();
    }

    public static boolean showNames(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_NAMES, true);
    }

    public static void setShowNames(Context ctx, boolean show) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_NAMES, show).apply();
    }

    /** Salva la qualità LD/HD di una telecamera senza toccare le altre. */
    public static void updateQuality(Context ctx, Camera cam) {
        List<Camera> all = load(ctx);
        for (Camera c : all) {
            if (c.sameAs(cam)) {
                c.highQuality = cam.highQuality;
                c.fullHd = cam.fullHd;
            }
        }
        save(ctx, all);
    }

    public static void save(Context ctx, List<Camera> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Camera c : list) arr.put(c.toJson());
        } catch (JSONException ignored) {
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply();
    }
}
