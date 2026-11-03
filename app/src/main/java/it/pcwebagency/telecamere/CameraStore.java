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
    /** Valore del gruppo selezionato che mostra tutte le telecamere. */
    public static final String ALL = "";

    private CameraStore() {}

    public static List<Camera> load(Context ctx) {
        List<Camera> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(Camera.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
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

    public static void save(Context ctx, List<Camera> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Camera c : list) arr.put(c.toJson());
        } catch (JSONException ignored) {
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply();
    }
}
