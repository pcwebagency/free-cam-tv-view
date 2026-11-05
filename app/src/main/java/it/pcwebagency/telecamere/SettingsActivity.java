package it.pcwebagency.telecamere;

import android.app.Activity;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
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
        root.setPadding(Ui.dp(this, 48), Ui.dp(this, 28), Ui.dp(this, 48), Ui.dp(this, 16));
        root.setBackgroundColor(Ui.BG);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        root.addView(Ui.text(this, getString(R.string.set_title), 26, Ui.TEXT, true));
        root.addView(Ui.text(this, getString(R.string.set_version, BuildConfig.VERSION_NAME), 13, Ui.TEXT_DIM, false),
                Ui.margins(this, -2, -2, 0, 2, 0, 18));

        LinearLayout actions = new LinearLayout(this);
        actions.setClipChildren(false);
        actions.addView(button(getString(R.string.set_search), v -> search()));
        actions.addView(button(getString(R.string.set_add), v -> edit(null)));
        actions.addView(button(getString(R.string.set_creds_all), v -> askCredentials(
                getString(R.string.set_creds_all_title), (u, p) -> {
                    for (Camera c : cams) {
                        c.user = u;
                        c.password = p;
                    }
                    saveAndRefresh();
                })));
        actions.addView(button(getString(BuildConfig.UPDATER_ENABLED ? R.string.set_info_updates : R.string.set_info), v -> about()));
        root.addView(actions);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 16));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        refresh();
        actions.getChildAt(0).requestFocus();
    }

    private void refresh() {
        list.removeAllViews();
        if (cams.isEmpty()) {
            list.addView(Ui.text(this, getString(R.string.set_empty),
                    16, Ui.TEXT_DIM, false), Ui.margins(this, -2, -2, 0, 28, 0, 0));
            return;
        }
        for (String group : CameraStore.groups(cams)) {
            List<Camera> inGroup = CameraStore.filter(cams, group);
            TextView header = Ui.text(this, group.toUpperCase() + "  ·  " + inGroup.size(), 12, 0xFF64B5F6, true);
            header.setLetterSpacing(0.12f);
            list.addView(header, Ui.margins(this, -2, -2, 4, 24, 0, 8));
            for (Camera c : inGroup) addCameraRow(c);
        }
    }

    /** Scheda di una telecamera: nome, dettagli ed etichette; OK apre la modifica. */
    private void addCameraRow(Camera c) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        card.setBackground(Ui.focusable(this, Ui.SURFACE, Ui.SURFACE_FOCUS, 14, true));
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(v -> edit(c));
        Ui.zoomOnFocus(card, 1.015f);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, c.label(), 16, Ui.TEXT, true));
        String detail = c.customUrl.isEmpty() ? getString(R.string.set_detail, c.ip, c.channel, c.port) : c.customUrl;
        texts.addView(Ui.text(this, detail, 13, Ui.TEXT_DIM, false));
        card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        if (c.password.isEmpty()) card.addView(Ui.badge(this, getString(R.string.badge_no_password), Ui.WARN), Ui.margins(this, -2, -2, 8, 0, 0, 0));
        if (c.highQuality) card.addView(Ui.badge(this, getString(R.string.badge_grid_hd), 0xFF81C784), Ui.margins(this, -2, -2, 8, 0, 0, 0));
        if (!c.fullHd) card.addView(Ui.badge(this, getString(R.string.badge_full_ld), Ui.TEXT_DIM), Ui.margins(this, -2, -2, 8, 0, 0, 0));
        if (c.door) card.addView(Ui.badge(this, getString(R.string.badge_door), 0xFF64B5F6), Ui.margins(this, -2, -2, 8, 0, 0, 0));
        card.addView(Ui.text(this, "›", 24, Ui.TEXT_DIM, false), Ui.margins(this, -2, -2, 16, 0, 0, 0));

        list.addView(card, Ui.margins(this, -1, -2, 0, 0, 0, 8));
    }

    private void saveAndRefresh() {
        CameraStore.save(this, cams);
        refresh();
    }

    // --- Ricerca automatica ---

    private void search() {
        android.app.Dialog wait = new ModernDialog(this)
                .setMessage(getString(R.string.set_searching)).setCancelable(false).show();
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
            new ModernDialog(this).setMessage(getString(R.string.set_none_found))
                    .setPositiveButton(getString(R.string.ok), null).show();
            return;
        }
        String[] items = new String[found.size()];
        boolean[] checked = new boolean[found.size()];
        for (int i = 0; i < found.size(); i++) {
            Discovery.Device d = found.get(i);
            boolean already = isConfigured(d.ip);
            items[i] = d.describe() + (already ? getString(R.string.set_already) : "");
            checked[i] = d.hasVideo() && !already;
        }
        new ModernDialog(this)
                .setTitle(getString(R.string.set_found_title))
                .setMultiChoiceItems(items, checked, (dlg, which, on) -> checked[which] = on)
                .setPositiveButton(getString(R.string.set_add), (dlg, w) -> {
                    List<Discovery.Device> chosen = new ArrayList<>();
                    for (int i = 0; i < found.size(); i++) if (checked[i]) chosen.add(found.get(i));
                    if (!chosen.isEmpty()) addDiscovered(chosen);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private boolean isConfigured(String ip) {
        for (Camera c : cams) if (c.ip.equals(ip)) return true;
        return false;
    }

    private void addDiscovered(List<Discovery.Device> chosen) {
        boolean hasRecorder = false;
        for (Discovery.Device d : chosen) hasRecorder |= d.isRecorder();
        String msg = getString(hasRecorder ? R.string.set_creds_selected_nvr : R.string.set_creds_selected);
        askGroupAndCredentials(msg, (u, p, group) -> new Thread(() -> {
            List<Camera> added = new ArrayList<>();
            StringBuilder errors = new StringBuilder();
            for (Discovery.Device d : chosen) {
                if (d.isRecorder()) {
                    try {
                        for (DahuaApi.Channel ch : DahuaApi.channels(d.ip, 80, u, p)) {
                            String name = ch.name.isEmpty() ? getString(R.string.channel_n, ch.number) : ch.name;
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
                String done = getString(R.string.set_added_streams, added.size());
                if (errors.length() > 0) {
                    new ModernDialog(this).setTitle(done)
                            .setMessage(getString(R.string.set_nvr_not_read, errors))
                            .setPositiveButton(getString(R.string.ok), null).show();
                } else {
                    Toast.makeText(this, done, Toast.LENGTH_LONG).show();
                }
            });
        }).start());
    }

    private static String defaultName(Discovery.Device d) {
        String n = d.ip.substring(d.ip.lastIndexOf('.') + 1);
        if (d.deviceClass.equalsIgnoreCase("VTO")) return App.s(R.string.name_vto, n);
        if (d.isRecorder()) return App.s(R.string.name_nvr, n);
        return App.s(R.string.name_cam, n);
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
        EditText user = field(form, getString(R.string.field_user), "admin", false);
        EditText pass = field(form, getString(R.string.field_password), "", true);
        new ModernDialog(this).setTitle(message).setView(form)
                .setPositiveButton(getString(R.string.ok), (d, w) -> cb.onCredentials(user.getText().toString().trim(), pass.getText().toString()))
                .setNeutralButton(getString(R.string.later), (d, w) -> cb.onCredentials("admin", ""))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /** Gruppo e credenziali per i dispositivi trovati dalla ricerca. */
    private void askGroupAndCredentials(String message, DiscoveredCallback cb) {
        LinearLayout form = form();
        EditText group = groupField(form, defaultGroup());
        EditText user = field(form, getString(R.string.field_user), "admin", false);
        EditText pass = field(form, getString(R.string.field_password), "", true);
        new ModernDialog(this).setTitle(message).setView(form)
                .setPositiveButton(getString(R.string.ok), (d, w) -> cb.onConfirm(
                        user.getText().toString().trim(), pass.getText().toString(), groupName(group)))
                .setNeutralButton(getString(R.string.password_later), (d, w) -> cb.onConfirm("admin", "", groupName(group)))
                .setNegativeButton(getString(R.string.cancel), null)
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
        String label = getString(R.string.field_group) + (groups.isEmpty() ? "" : getString(R.string.field_group_existing, TextUtils.join(", ", groups)));
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
        EditText name = field(form, getString(R.string.field_name), c.name, false);
        EditText group = groupField(form, c.group);
        EditText ip = field(form, getString(R.string.field_host), c.ip, false);
        EditText port = field(form, getString(R.string.field_rtsp_port), String.valueOf(c.port), false);
        EditText user = field(form, getString(R.string.field_user), c.user, false);
        EditText pass = field(form, getString(R.string.field_password), c.password, true);
        EditText channel = field(form, getString(R.string.field_channel),
                String.valueOf(c.channel), false);
        EditText custom = field(form, getString(R.string.field_custom_url), c.customUrl, false);
        EditText httpPort = field(form, getString(R.string.field_http_port), String.valueOf(c.httpPort), false);
        httpPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        CheckBox door = new CheckBox(this);
        door.setText(R.string.chk_door);
        door.setChecked(c.door);
        form.addView(door);
        port.setInputType(InputType.TYPE_CLASS_NUMBER);

        CheckBox gridHd = new CheckBox(this);
        gridHd.setText(R.string.chk_grid_hd);
        gridHd.setChecked(c.highQuality);
        form.addView(gridHd);
        CheckBox fullHd = new CheckBox(this);
        fullHd.setText(R.string.chk_full_hd);
        fullHd.setChecked(c.fullHd);
        form.addView(fullHd);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        ModernDialog b = new ModernDialog(this)
                .setTitle(isNew ? getString(R.string.edit_add_title) : getString(R.string.edit_title, c.label()))
                .setView(scroll)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    c.name = name.getText().toString().trim();
                    c.group = groupName(group);
                    c.highQuality = gridHd.isChecked();
                    c.fullHd = fullHd.isChecked();
                    c.httpPort = parseInt(httpPort, 80);
                    c.door = door.isChecked();
                    c.ip = ip.getText().toString().trim();
                    c.port = parseInt(port, 554);
                    c.user = user.getText().toString().trim();
                    c.password = pass.getText().toString();
                    List<Integer> chans = parseChannels(channel.getText().toString());
                    if (chans.isEmpty()) {
                        Toast.makeText(this, R.string.err_channel, Toast.LENGTH_LONG).show();
                        return;
                    }
                    c.channel = chans.get(0);
                    c.customUrl = custom.getText().toString().trim();
                    if (c.ip.isEmpty() && c.customUrl.isEmpty()) {
                        Toast.makeText(this, R.string.err_ip, Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (isNew) cams.add(c);
                    if (chans.size() > 1) addChannels(c, chans);
                    saveAndRefresh();
                });
        if (isNew) {
            b.setNeutralButton(getString(R.string.import_nvr), (d, w) -> {
                Camera t = new Camera();
                t.ip = ip.getText().toString().trim();
                t.port = parseInt(port, 554);
                t.httpPort = parseInt(httpPort, 80);
                t.user = user.getText().toString().trim();
                t.password = pass.getText().toString();
                t.group = groupName(group);
                t.highQuality = gridHd.isChecked();
                t.fullHd = fullHd.isChecked();
                importNvr(t);
            });
            b.setNegativeButton(getString(R.string.cancel), null);
        } else {
            b.setNeutralButton(getString(R.string.delete), (d, w) -> {
                cams.remove(c);
                saveAndRefresh();
            });
            b.setNegativeButton(getString(R.string.cancel), null);
        }
        b.show();
    }

    /**
     * Più canali dello stesso NVR: {@code c} prende il primo, per gli altri si creano copie
     * con lo stesso host, porte e credenziali. I nomi diventano "Nome 1", "Nome 2"…
     */
    private void addChannels(Camera c, List<Integer> chans) {
        String base = c.name.isEmpty() ? getString(R.string.channel_word) : c.name;
        c.name = base + " " + chans.get(0);
        int pos = cams.indexOf(c);
        for (int i = 1; i < chans.size(); i++) {
            Camera x = c.copy();
            x.channel = chans.get(i);
            x.name = base + " " + x.channel;
            x.door = false;
            cams.add(pos + i, x);
        }
        Toast.makeText(this, getString(R.string.added_channels, chans.size()), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, R.string.err_nvr_ip, Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                List<DahuaApi.Channel> chans = DahuaApi.channels(t.ip, t.httpPort, t.user, t.password);
                runOnUiThread(() -> {
                    for (DahuaApi.Channel ch : chans) {
                        Camera c = t.copy();
                        c.name = ch.name.isEmpty() ? getString(R.string.channel_n, ch.number) : ch.name;
                        c.channel = ch.number;
                        cams.add(c);
                    }
                    saveAndRefresh();
                    Toast.makeText(this, getString(R.string.imported_channels, chans.size()), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> new ModernDialog(this).setTitle(getString(R.string.channels_failed_title))
                        .setMessage(getString(R.string.channels_failed_msg, t.ip, t.httpPort, e.getMessage()))
                        .setPositiveButton(getString(R.string.ok), null).show());
            }
        }).start();
    }

    // --- Informazioni, credits e aggiornamenti ---

    private void about() {
        boolean updater = BuildConfig.UPDATER_ENABLED;
        boolean auto = Updater.isAutoCheck(this);
        String msg = getString(R.string.about_text, BuildConfig.VERSION_NAME, BuildConfig.GITHUB_REPO)
                + (updater ? getString(R.string.about_auto, getString(auto ? R.string.on : R.string.off)) : "");
        ModernDialog dialog = new ModernDialog(this)
                .setTitle("Free Cam TV View")
                .setMessage(msg);
        if (updater) {
            // pulsanti di aggiornamento solo se l.aggiornamento interno è abilitato
            dialog.setPositiveButton(getString(R.string.check_updates), (d, w) -> Updater.check(this, true))
                    .setNeutralButton(getString(auto ? R.string.disable_auto : R.string.enable_auto), (d, w) -> {
                        Updater.setAutoCheck(this, !auto);
                        about();
                    })
                    .setNegativeButton(getString(R.string.close), null);
        } else {
            dialog.setPositiveButton(getString(R.string.close), null);
        }
        dialog.show();
    }

    // --- Helper UI ---

    private LinearLayout form() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setPadding(48, 16, 48, 0);
        return f;
    }

    private EditText field(LinearLayout form, String label, String value, boolean password) {
        TextView l = Ui.text(this, label, 13, Ui.TEXT_DIM, false);
        l.setPadding(0, Ui.dp(this, 10), 0, 0);
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

    private TextView button(String label, View.OnClickListener l) {
        TextView b = Ui.pill(this, label, l);
        b.setLayoutParams(Ui.margins(this, -2, -2, 0, 0, 12, 0));
        return b;
    }
}
