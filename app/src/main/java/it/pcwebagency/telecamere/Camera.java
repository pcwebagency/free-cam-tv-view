package it.pcwebagency.telecamere;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

/** Un flusso video: una telecamera IP o un canale di un NVR. */
public class Camera {
    public String name = "";
    public String ip = "";
    public int port = 554;
    public String user = "admin";
    public String password = "";
    public int channel = 1;
    /** Se valorizzato sostituisce l'URL Dahua (telecamere di altre marche). */
    public String customUrl = "";
    /** Gruppo/luogo, es. "Casa" o "Ufficio". */
    public String group = DEFAULT_GROUP;

    public static final String DEFAULT_GROUP = "Studio";
    /** Qualità alta: flusso principale HD anche in griglia (standard: secondario in griglia). */
    public boolean highQuality = false;
    /** Videocitofono Dahua (VTO): mostra l'apriporta a schermo intero. */
    public boolean door = false;
    /** Porta HTTP per i comandi CGI (apriporta, canali NVR). */
    public int httpPort = 80;

    public String url(boolean hd) {
        if (!customUrl.isEmpty()) return customUrl;
        String auth = user.isEmpty() ? "" : Uri.encode(user) + ":" + Uri.encode(password) + "@";
        return "rtsp://" + auth + ip + ":" + port
                + "/cam/realmonitor?channel=" + channel + "&subtype=" + (hd ? 0 : 1);
    }

    public String label() {
        return name.isEmpty() ? ip + " ch" + channel : name;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("name", name).put("ip", ip).put("port", port)
                .put("user", user).put("password", password)
                .put("channel", channel).put("customUrl", customUrl)
                .put("group", group).put("highQuality", highQuality)
                .put("door", door).put("httpPort", httpPort);
    }

    static Camera fromJson(JSONObject o) {
        Camera c = new Camera();
        c.name = o.optString("name");
        c.ip = o.optString("ip");
        c.port = o.optInt("port", 554);
        c.user = o.optString("user", "admin");
        c.password = o.optString("password");
        c.channel = o.optInt("channel", 1);
        c.customUrl = o.optString("customUrl");
        c.group = o.optString("group", DEFAULT_GROUP);
        if (c.group.isEmpty()) c.group = DEFAULT_GROUP;
        c.highQuality = o.optBoolean("highQuality", false);
        c.door = o.optBoolean("door", false);
        c.httpPort = o.optInt("httpPort", 80);
        return c;
    }
}
