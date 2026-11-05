package it.pcwebagency.telecamere;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chiamate CGI Dahua (autenticazione digest) per leggere i canali di un NVR. */
public final class DahuaApi {

    public static class Channel {
        public final int number;
        public final String name;

        Channel(int number, String name) {
            this.number = number;
            this.name = name;
        }
    }

    private DahuaApi() {}

    /** Canali dell'NVR; se il dispositivo lo supporta, solo quelli con una telecamera collegata. */
    public static List<Channel> channels(String host, int httpPort, String user, String pass) throws IOException {
        String base = "http://" + host + ":" + httpPort;
        String titles = get(base + "/cgi-bin/configManager.cgi?action=getConfig&name=ChannelTitle", user, pass);
        List<Channel> all = new ArrayList<>();
        Matcher m = Pattern.compile("ChannelTitle\\[(\\d+)]\\.Name=(.*)").matcher(titles);
        while (m.find()) all.add(new Channel(Integer.parseInt(m.group(1)) + 1, m.group(2).trim()));

        Set<Integer> connected = new HashSet<>();
        try {
            String states = get(base
                    + "/cgi-bin/LogicDeviceManager.cgi?action=getCameraState&uniqueChannels[0]=-1", user, pass);
            Matcher s = Pattern.compile("states\\[\\d+]\\.channel=(\\d+)\\s+states\\[\\d+]\\.connectionState=(\\w+)")
                    .matcher(states);
            while (s.find()) {
                if (s.group(2).equalsIgnoreCase("Connected")) connected.add(Integer.parseInt(s.group(1)) + 1);
            }
        } catch (IOException ignored) {
            // NVR più vecchi non hanno questa API: si tengono tutti i canali
        }
        if (connected.isEmpty()) return all;
        List<Channel> active = new ArrayList<>();
        for (Channel c : all) if (connected.contains(c.number)) active.add(c);
        return active;
    }

    /** Apre la serratura {@code door} (1 o 2) di un videocitofono Dahua. */
    public static void openDoor(Camera cam, int door) throws IOException {
        String r = get("http://" + cam.ip + ":" + cam.httpPort
                + "/cgi-bin/accessControl.cgi?action=openDoor&channel=" + door + "&UserID=101&Type=Remote",
                cam.user, cam.password);
        if (!r.trim().equalsIgnoreCase("OK")) throw new IOException(r.trim());
    }

    static String get(String url, String user, String pass) throws IOException {
        HttpURLConnection c = open(url);
        int code = c.getResponseCode();
        if (code == 401) {
            String challenge = c.getHeaderField("WWW-Authenticate");
            c.disconnect();
            if (challenge == null || !challenge.startsWith("Digest")) throw new IOException(App.s(R.string.err_auth_unsupported));
            c = open(url);
            c.setRequestProperty("Authorization", digest(challenge, new URL(url).getFile(), user, pass));
            code = c.getResponseCode();
        }
        if (code == 401) throw new IOException(App.s(R.string.err_wrong_credentials));
        if (code != 200) throw new IOException("HTTP " + code);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(8000);
        return c;
    }

    private static String digest(String challenge, String uri, String user, String pass) {
        String realm = param(challenge, "realm"), nonce = param(challenge, "nonce");
        String qop = param(challenge, "qop"), opaque = param(challenge, "opaque");
        String ha1 = md5(user + ":" + realm + ":" + pass);
        String ha2 = md5("GET:" + uri);
        String cnonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String nc = "00000001";
        StringBuilder h = new StringBuilder("Digest username=\"").append(user)
                .append("\", realm=\"").append(realm)
                .append("\", nonce=\"").append(nonce)
                .append("\", uri=\"").append(uri).append('"');
        if (qop != null) {
            h.append(", qop=auth, nc=").append(nc).append(", cnonce=\"").append(cnonce)
                    .append("\", response=\"").append(md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2)).append('"');
        } else {
            h.append(", response=\"").append(md5(ha1 + ":" + nonce + ":" + ha2)).append('"');
        }
        if (opaque != null) h.append(", opaque=\"").append(opaque).append('"');
        return h.toString();
    }

    private static String param(String header, String name) {
        Matcher m = Pattern.compile(name + "=\"?([^\",]*)\"?").matcher(header);
        return m.find() ? m.group(1) : null;
    }

    private static String md5(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
