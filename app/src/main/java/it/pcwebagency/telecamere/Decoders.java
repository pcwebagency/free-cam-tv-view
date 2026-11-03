package it.pcwebagency.telecamere;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.HashMap;
import java.util.Map;

/** Limiti dei decoder video hardware della TV (es. H.265 fino a 1920×1088 sui chip Amlogic). */
public final class Decoders {
    private static final Map<String, Boolean> cache = new HashMap<>();

    private Decoders() {}

    public static synchronized boolean hardwareSupports(String mime, int width, int height) {
        String key = mime + width + "x" + height;
        Boolean cached = cache.get(key);
        if (cached != null) return cached;
        boolean ok = false;
        boolean anyHardware = false;
        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
            if (info.isEncoder() || isSoftware(info.getName())) continue;
            for (String type : info.getSupportedTypes()) {
                if (!type.equalsIgnoreCase(mime)) continue;
                anyHardware = true;
                MediaCodecInfo.VideoCapabilities caps = info.getCapabilitiesForType(type).getVideoCapabilities();
                if (caps != null && caps.isSizeSupported(width, height)) ok = true;
            }
        }
        // senza decoder hardware per questo formato non si può giudicare: si lascia provare
        if (!anyHardware) ok = true;
        cache.put(key, ok);
        return ok;
    }

    private static boolean isSoftware(String name) {
        String n = name.toLowerCase();
        return n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.contains(".sw.");
    }
}
