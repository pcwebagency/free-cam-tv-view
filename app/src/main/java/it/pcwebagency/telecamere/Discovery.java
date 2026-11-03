package it.pcwebagency.telecamere;

import android.content.Context;
import android.net.wifi.WifiManager;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Ricerca dispositivi in rete: protocollo Dahua DHIP (UDP 37810) che restituisce il modello,
 * più una scansione della porta RTSP 554 per le telecamere di altre marche.
 */
public final class Discovery {

    public static class Device {
        public String ip;
        public String model = "";
        /** Classe Dahua: IPC, VTO, VTH, NVR, XVR, DVR... vuota se non Dahua. */
        public String deviceClass = "";

        public boolean isRecorder() {
            String c = deviceClass.toUpperCase();
            return c.contains("NVR") || c.contains("DVR") || c.contains("XVR") || c.contains("HCVR");
        }

        /** I monitor interni dei videocitofoni (VTH) non hanno telecamera. */
        public boolean hasVideo() {
            return !deviceClass.equalsIgnoreCase("VTH");
        }

        public String describe() {
            if (deviceClass.isEmpty()) return ip + " — RTSP generico";
            String kind;
            switch (deviceClass.toUpperCase()) {
                case "IPC": kind = "Telecamera"; break;
                case "VTO": kind = "Videocitofono esterno"; break;
                case "VTH": kind = "Monitor interno (senza video)"; break;
                default: kind = isRecorder() ? "Registratore " + deviceClass : deviceClass;
            }
            return ip + " — " + kind + " " + model;
        }
    }

    private Discovery() {}

    public static List<Device> scan(Context ctx) {
        Map<String, Device> found = new LinkedHashMap<>();
        dhip(ctx, found);
        rtspScan(found);
        List<Device> list = new ArrayList<>(found.values());
        Collections.sort(list, (a, b) -> Integer.compare(lastOctet(a.ip), lastOctet(b.ip)));
        return list;
    }

    private static void dhip(Context ctx, Map<String, Device> found) {
        WifiManager wifi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock lock = wifi.createMulticastLock("telecamere");
        lock.acquire();
        try (DatagramSocket s = new DatagramSocket()) {
            s.setBroadcast(true);
            s.setSoTimeout(500);
            byte[] body = "{\"method\":\"DHDiscover.search\",\"params\":{\"mac\":\"\",\"uni\":1}}"
                    .getBytes(StandardCharsets.UTF_8);
            ByteBuffer pkt = ByteBuffer.allocate(32 + body.length).order(ByteOrder.LITTLE_ENDIAN);
            pkt.putInt(0x20).put("DHIP".getBytes(StandardCharsets.US_ASCII))
                    .putInt(0).putInt(0).putInt(body.length).putInt(0).putInt(body.length).putInt(0)
                    .put(body);
            byte[] data = pkt.array();
            for (String dst : new String[]{"239.255.255.251", "255.255.255.255"}) {
                s.send(new DatagramPacket(data, data.length, InetAddress.getByName(dst), 37810));
            }
            byte[] buf = new byte[65535];
            long end = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < end) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    s.receive(p);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                String txt = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                int start = txt.indexOf('{');
                if (start < 0) continue;
                try {
                    JSONObject info = new JSONObject(txt.substring(start).trim())
                            .getJSONObject("params").getJSONObject("deviceInfo");
                    Device d = new Device();
                    d.ip = p.getAddress().getHostAddress();
                    d.model = info.optString("DeviceType");
                    d.deviceClass = info.optString("DeviceClass");
                    found.put(d.ip, d);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            lock.release();
        }
    }

    private static void rtspScan(Map<String, Device> found) {
        String prefix = localPrefix();
        if (prefix == null) return;
        ExecutorService pool = Executors.newFixedThreadPool(48);
        for (int i = 1; i < 255; i++) {
            String ip = prefix + i;
            if (found.containsKey(ip)) continue;
            pool.execute(() -> {
                try (Socket sock = new Socket()) {
                    sock.connect(new InetSocketAddress(ip, 554), 400);
                    Device d = new Device();
                    d.ip = ip;
                    synchronized (found) {
                        found.put(ip, d);
                    }
                } catch (Exception ignored) {
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private static String localPrefix() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        String ip = a.getHostAddress();
                        return ip.substring(0, ip.lastIndexOf('.') + 1);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int lastOctet(String ip) {
        return Integer.parseInt(ip.substring(ip.lastIndexOf('.') + 1));
    }
}
