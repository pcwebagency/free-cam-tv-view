package it.pcwebagency.telecamere;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Ricerca automatica, aggiunta manuale e modifica di telecamere e NVR. */
public class SettingsActivity extends Activity {
    private List<Camera> cams;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        cams = CameraStore.load(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(64, 32, 64, 32);
        root.setBackgroundColor(0xFF121212);

        TextView title = text("Impostazioni telecamere", 26);
        root.addView(title);

        LinearLayout actions = new LinearLayout(this);
        actions.addView(button("Cerca in rete", v -> search()));
        actions.addView(button("Aggiungi manualmente", v -> edit(null)));
        actions.addView(button("Stesse credenziali per tutte", v -> askCredentials(
                "Credenziali per tutte le telecamere", (u, p) -> {
                    for (Camera c : cams) {
                        c.user = u;
                        c.password = p;
                    }
                    saveAndRefresh();
                })));
        actions.addView(button("Informazioni e aggiornamenti", v -> about()));
        root.addView(actions);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        refresh();
        actions.getChildAt(0).requestFocus();
    }

    private void refresh() {
        list.removeAllViews();
        list.addView(text(cams.isEmpty() ? "\nNessuna telecamera: usa «Cerca in rete»."
                : "\nTelecamere configurate (OK per modificare):", 18));
        for (String group : CameraStore.groups(cams)) {
            TextView header = text("\n" + group, 20);
            header.setTextColor(0xFF64B5F6);
            list.addView(header);
            for (Camera c : CameraStore.filter(cams, group)) addCameraRow(c);
        }
    }

    private void addCameraRow(Camera c) {
        String info = c.label() + "   —   " + (c.customUrl.isEmpty() ? c.ip + "  canale " + c.channel : c.customUrl)
                + (c.highQuality ? "   · qualità alta" : "")
                + (c.door ? "   · apriporta" : "")
                + (c.password.isEmpty() ? "   ⚠ senza password" : "");
        Button b = button(info, v -> edit(c));
        b.setAllCaps(false);
        list.addView(b, new LinearLayout.LayoutParams(-1, -2));
    }

    private void saveAndRefresh() {
        CameraStore.save(this, cams);
        refresh();
    }

    // --- Ricerca automatica ---

    private void search() {
        AlertDialog wait = new AlertDialog.Builder(this)
                .setMessage("Ricerca di telecamere e NVR in corso…").setCancelable(false).show();
        new Thread(() -> {
            List<Discovery.Device> found = Discovery.scan(this);
            runOnUiThread(() -> {
                wait.dismiss();
                showResults(found);
            });
        }).start();
    }

    private void showResults(List<Discovery.Device> found) {
        if (found.isEmpty()) {
            new AlertDialog.Builder(this).setMessage("Nessun dispositivo trovato in rete.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] items = new String[found.size()];
        boolean[] checked = new boolean[found.size()];
        for (int i = 0; i < found.size(); i++) {
            Discovery.Device d = found.get(i);
            boolean already = isConfigured(d.ip);
            items[i] = d.describe() + (already ? "  (già aggiunto)" : "");
            checked[i] = d.hasVideo() && !already;
        }
        new AlertDialog.Builder(this)
                .setTitle("Dispositivi trovati: scegli quali aggiungere")
                .setMultiChoiceItems(items, checked, (dlg, which, on) -> checked[which] = on)
                .setPositiveButton("Aggiungi", (dlg, w) -> {
                    List<Discovery.Device> chosen = new ArrayList<>();
                    for (int i = 0; i < found.size(); i++) if (checked[i]) chosen.add(found.get(i));
                    if (!chosen.isEmpty()) addDiscovered(chosen);
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private boolean isConfigured(String ip) {
        for (Camera c : cams) if (c.ip.equals(ip)) return true;
        return false;
    }

    private void addDiscovered(List<Discovery.Device> chosen) {
        boolean hasRecorder = false;
        for (Discovery.Device d : chosen) hasRecorder |= d.isRecorder();
        String msg = hasRecorder ? "Credenziali dei dispositivi selezionati\n(per gli NVR servono per leggere i canali)"
                : "Credenziali dei dispositivi selezionati";
        askGroupAndCredentials(msg, (u, p, group) -> new Thread(() -> {
            List<Camera> added = new ArrayList<>();
            StringBuilder errors = new StringBuilder();
            for (Discovery.Device d : chosen) {
                if (d.isRecorder()) {
                    try {
                        for (DahuaApi.Channel ch : DahuaApi.channels(d.ip, 80, u, p)) {
                            String name = ch.name.isEmpty() ? "Canale " + ch.number : ch.name;
                            added.add(newCamera(name, d.ip, u, p, ch.number));
                        }
                        continue;
                    } catch (Exception e) {
                        errors.append(d.ip).append(": ").append(e.getMessage()).append('\n');
                    }
                }
                Camera cam = newCamera(defaultName(d), d.ip, u, p, 1);
                cam.door = d.deviceClass.equalsIgnoreCase("VTO");
                added.add(cam);
            }
            for (Camera c : added) c.group = group;
            runOnUiThread(() -> {
                cams.addAll(added);
                saveAndRefresh();
                String done = "Aggiunti " + added.size() + " flussi video.";
                if (errors.length() > 0) {
                    new AlertDialog.Builder(this).setTitle(done)
                            .setMessage("Canali NVR non letti, aggiunto solo il canale 1:\n" + errors)
                            .setPositiveButton("OK", null).show();
                } else {
                    Toast.makeText(this, done, Toast.LENGTH_LONG).show();
                }
            });
        }).start());
    }

    private static String defaultName(Discovery.Device d) {
        String n = d.ip.substring(d.ip.lastIndexOf('.') + 1);
        if (d.deviceClass.equalsIgnoreCase("VTO")) return "Videocitofono " + n;
        if (d.isRecorder()) return "NVR " + n;
        return "Telecamera " + n;
    }

    private static Camera newCamera(String name, String ip, String user, String pass, int channel) {
        Camera c = new Camera();
        c.name = name;
        c.ip = ip;
        c.user = user;
        c.password = pass;
        c.channel = channel;
        return c;
    }

    // --- Dialoghi ---

    private interface CredentialsCallback {
        void onCredentials(String user, String pass);
    }

    private interface DiscoveredCallback {
        void onConfirm(String user, String pass, String group);
    }

    private void askCredentials(String message, CredentialsCallback cb) {
        LinearLayout form = form();
        EditText user = field(form, "Utente", "admin", false);
        EditText pass = field(form, "Password", "", true);
        new AlertDialog.Builder(this).setTitle(message).setView(form)
                .setPositiveButton("OK", (d, w) -> cb.onCredentials(user.getText().toString().trim(), pass.getText().toString()))
                .setNeutralButton("Più tardi", (d, w) -> cb.onCredentials("admin", ""))
                .setNegativeButton("Annulla", null)
                .show();
    }

    /** Gruppo e credenziali per i dispositivi trovati dalla ricerca. */
    private void askGroupAndCredentials(String message, DiscoveredCallback cb) {
        LinearLayout form = form();
        EditText group = groupField(form, defaultGroup());
        EditText user = field(form, "Utente", "admin", false);
        EditText pass = field(form, "Password", "", true);
        new AlertDialog.Builder(this).setTitle(message).setView(form)
                .setPositiveButton("OK", (d, w) -> cb.onConfirm(
                        user.getText().toString().trim(), pass.getText().toString(), groupName(group)))
                .setNeutralButton("Password più tardi", (d, w) -> cb.onConfirm("admin", "", groupName(group)))
                .setNegativeButton("Annulla", null)
                .show();
    }

    private String defaultGroup() {
        String sel = CameraStore.selectedGroup(this);
        if (!sel.equals(CameraStore.ALL)) return sel;
        List<String> groups = CameraStore.groups(cams);
        return groups.isEmpty() ? Camera.DEFAULT_GROUP : groups.get(0);
    }

    private EditText groupField(LinearLayout form, String value) {
        List<String> groups = CameraStore.groups(cams);
        String label = "Gruppo (es. Studio, Casa)" + (groups.isEmpty() ? "" : " — esistenti: " + TextUtils.join(", ", groups));
        return field(form, label, value, false);
    }

    private static String groupName(EditText e) {
        String g = e.getText().toString().trim();
        return g.isEmpty() ? Camera.DEFAULT_GROUP : g;
    }

    /** Modifica di una telecamera esistente, o aggiunta manuale se {@code cam} è null. */
    private void edit(Camera cam) {
        boolean isNew = cam == null;
        Camera c = isNew ? new Camera() : cam;
        if (isNew) c.group = defaultGroup();
        LinearLayout form = form();
        EditText name = field(form, "Nome", c.name, false);
        EditText group = groupField(form, c.group);
        EditText ip = field(form, "Indirizzo IP o nome host (DDNS per sedi remote)", c.ip, false);
        EditText port = field(form, "Porta RTSP", String.valueOf(c.port), false);
        EditText user = field(form, "Utente", c.user, false);
        EditText pass = field(form, "Password", c.password, true);
        EditText channel = field(form, "Canale (1 per le telecamere; per gli NVR anche più canali: 1-8 oppure 1,3,5)",
                String.valueOf(c.channel), false);
        EditText custom = field(form, "URL RTSP personalizzato (solo per altre marche, facoltativo)", c.customUrl, false);
        EditText httpPort = field(form, "Porta HTTP (apriporta e canali NVR)", String.valueOf(c.httpPort), false);
        httpPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        CheckBox door = new CheckBox(this);
        door.setText("Videocitofono: mostra l'apriporta a schermo intero");
        door.setChecked(c.door);
        form.addView(door);
        port.setInputType(InputType.TYPE_CLASS_NUMBER);

        TextView qualityLabel = new TextView(this);
        qualityLabel.setText("Qualità del flusso");
        form.addView(qualityLabel);
        RadioGroup quality = new RadioGroup(this);
        RadioButton standard = new RadioButton(this);
        standard.setText("Standard — griglia leggera, HD a schermo intero");
        standard.setId(View.generateViewId());
        RadioButton high = new RadioButton(this);
        high.setText("Alta — HD anche in griglia (più carico per TV e rete)");
        high.setId(View.generateViewId());
        quality.addView(standard);
        quality.addView(high);
        quality.check(c.highQuality ? high.getId() : standard.getId());
        form.addView(quality);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(isNew ? "Aggiungi telecamera o NVR" : "Modifica " + c.label())
                .setView(scroll)
                .setPositiveButton("Salva", (d, w) -> {
                    c.name = name.getText().toString().trim();
                    c.group = groupName(group);
                    c.highQuality = quality.getCheckedRadioButtonId() == high.getId();
                    c.httpPort = parseInt(httpPort, 80);
                    c.door = door.isChecked();
                    c.ip = ip.getText().toString().trim();
                    c.port = parseInt(port, 554);
                    c.user = user.getText().toString().trim();
                    c.password = pass.getText().toString();
                    List<Integer> chans = parseChannels(channel.getText().toString());
                    if (chans.isEmpty()) {
                        Toast.makeText(this, "Canale non valido: usa ad esempio 1, 1-8 oppure 1,3,5", Toast.LENGTH_LONG).show();
                        return;
                    }
                    c.channel = chans.get(0);
                    c.customUrl = custom.getText().toString().trim();
                    if (c.ip.isEmpty() && c.customUrl.isEmpty()) {
                        Toast.makeText(this, "Serve almeno l'indirizzo IP", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (isNew) cams.add(c);
                    if (chans.size() > 1) addChannels(c, chans);
                    saveAndRefresh();
                });
        if (isNew) {
            b.setNeutralButton("Importa tutti i canali NVR", (d, w) -> {
                Camera t = new Camera();
                t.ip = ip.getText().toString().trim();
                t.port = parseInt(port, 554);
                t.httpPort = parseInt(httpPort, 80);
                t.user = user.getText().toString().trim();
                t.password = pass.getText().toString();
                t.group = groupName(group);
                t.highQuality = quality.getCheckedRadioButtonId() == high.getId();
                importNvr(t);
            });
            b.setNegativeButton("Annulla", null);
        } else {
            b.setNeutralButton("Elimina", (d, w) -> {
                cams.remove(c);
                saveAndRefresh();
            });
            b.setNegativeButton("Annulla", null);
        }
        b.show();
    }

    /**
     * Più canali dello stesso NVR: {@code c} prende il primo, per gli altri si creano copie
     * con lo stesso host, porte e credenziali. I nomi diventano "Nome 1", "Nome 2"…
     */
    private void addChannels(Camera c, List<Integer> chans) {
        String base = c.name.isEmpty() ? "Canale" : c.name;
        c.name = base + " " + chans.get(0);
        int pos = cams.indexOf(c);
        for (int i = 1; i < chans.size(); i++) {
            Camera x = c.copy();
            x.channel = chans.get(i);
            x.name = base + " " + x.channel;
            x.door = false;
            cams.add(pos + i, x);
        }
        Toast.makeText(this, "Aggiunti " + chans.size() + " canali", Toast.LENGTH_LONG).show();
    }

    /** "1-8", "1,3,5", "2" → elenco di canali (1…128, al massimo 64). */
    static List<Integer> parseChannels(String text) {
        List<Integer> out = new ArrayList<>();
        try {
            for (String part : text.replace(" ", "").split(",")) {
                if (part.isEmpty()) continue;
                String[] r = part.split("-", 2);
                int from = Integer.parseInt(r[0]), to = r.length > 1 ? Integer.parseInt(r[1]) : from;
                for (int ch = from; ch <= to && out.size() < 64; ch++) {
                    if (ch >= 1 && ch <= 128 && !out.contains(ch)) out.add(ch);
                }
            }
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
        return out;
    }

    /** Legge i canali dall'NVR (CGI sulla porta HTTP) e crea una voce per ciascuno, copiando {@code t}. */
    private void importNvr(Camera t) {
        if (t.ip.isEmpty()) {
            Toast.makeText(this, "Inserisci l'indirizzo IP dell'NVR", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                List<DahuaApi.Channel> chans = DahuaApi.channels(t.ip, t.httpPort, t.user, t.password);
                runOnUiThread(() -> {
                    for (DahuaApi.Channel ch : chans) {
                        Camera c = t.copy();
                        c.name = ch.name.isEmpty() ? "Canale " + ch.number : ch.name;
                        c.channel = ch.number;
                        cams.add(c);
                    }
                    saveAndRefresh();
                    Toast.makeText(this, "Importati " + chans.size() + " canali", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Impossibile leggere i canali")
                        .setMessage(t.ip + ":" + t.httpPort + " — " + e.getMessage()
                                + "\n\nDa remoto, se la porta HTTP dell'NVR non è raggiungibile, inserisci i canali "
                                + "a mano nel campo Canale (es. 1-8).")
                        .setPositiveButton("OK", null).show());
            }
        }).start();
    }

    // --- Informazioni, credits e aggiornamenti ---

    private void about() {
        boolean auto = Updater.isAutoCheck(this);
        String msg = "Versione " + BuildConfig.VERSION_NAME + "\n\n"
                + "Sviluppata da PC Web Agency\n"
                + "Ing. Carmine Palermo\n"
                + "www.pcwebagency.it · info@pcwebagency.it\n\n"
                + "Credits\n"
                + "• AndroidX Media3 — The Android Open Source Project (Apache 2.0)\n"
                + "• AndroidX Core e Annotation — The Android Open Source Project (Apache 2.0)\n\n"
                + "Licenza Apache 2.0\n"
                + "github.com/" + BuildConfig.GITHUB_REPO + "\n\n"
                + "Controllo automatico degli aggiornamenti: " + (auto ? "attivo" : "disattivato");
        new AlertDialog.Builder(this)
                .setTitle("Free Cam TV View")
                .setMessage(msg)
                .setPositiveButton("Controlla aggiornamenti", (d, w) -> Updater.check(this, true))
                .setNeutralButton(auto ? "Disattiva controllo automatico" : "Attiva controllo automatico", (d, w) -> {
                    Updater.setAutoCheck(this, !auto);
                    about();
                })
                .setNegativeButton("Chiudi", null)
                .show();
    }

    // --- Helper UI ---

    private LinearLayout form() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(48, 16, 48, 0);
        return f;
    }

    private EditText field(LinearLayout form, String label, String value, boolean password) {
        TextView l = new TextView(this);
        l.setText(label);
        form.addView(l);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(value);
        e.setInputType(password ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        form.addView(e);
        return e;
    }

    private static int parseInt(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        return t;
    }

    private Button button(String label, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(l);
        return b;
    }
}
