package it.pcwebagency.telecamere;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Griglia live (1, 4, 9 o 16 riquadri) delle telecamere del gruppo scelto, con pagine. */
public class MainActivity extends Activity {
    private static final int[] LAYOUTS = {1, 4, 9, 16};
    private int pageSize = 4;
    private static final int BORDER_MS = 1000;

    private final List<LiveView> views = new ArrayList<>();
    private List<Camera> cams = new ArrayList<>();
    private int page;
    private LinearLayout grid;
    private LinearLayout tabs;
    private TextView footer;
    private TextView doorHint;
    private LinearLayout layoutButtons;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View bordered;
    private final Runnable hideBorder = () -> {
        if (bordered != null) bordered.setBackgroundColor(Color.BLACK);
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        tabs = new LinearLayout(this);
        tabs.setPadding(8, 4, 8, 4);
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout bottom = new LinearLayout(this);
        footer = new TextView(this);
        footer.setTextColor(Color.GRAY);
        footer.setTextSize(13);
        footer.setPadding(16, 4, 16, 6);
        bottom.addView(footer, new LinearLayout.LayoutParams(0, -2, 1));
        doorHint = new TextView(this);
        doorHint.setTextColor(Color.GRAY);
        doorHint.setTextSize(13);
        doorHint.setPadding(16, 4, 16, 6);
        doorHint.setText("OK tenuto premuto: LD/HD e opzioni");
        doorHint.setVisibility(View.GONE);
        bottom.addView(doorHint, new LinearLayout.LayoutParams(-2, -2));
        layoutButtons = new LinearLayout(this);
        layoutButtons.setPadding(0, 0, 12, 0);
        layoutButtons.setGravity(Gravity.CENTER_VERTICAL);
        bottom.addView(layoutButtons, new LinearLayout.LayoutParams(-2, -2));
        root.addView(bottom, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        Updater.autoCheck(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroup();
        page = Math.min(page, Math.max(0, pageCount() - 1));
        show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(hideBorder);
        stopAll();
    }

    /** Carica le telecamere del gruppo selezionato e ricostruisce la barra dei gruppi. */
    private void loadGroup() {
        List<Camera> all = CameraStore.load(this);
        List<String> groups = CameraStore.groups(all);
        String selected = CameraStore.selectedGroup(this);
        if (!selected.equals(CameraStore.ALL) && !groups.contains(selected)) selected = CameraStore.ALL;
        cams = CameraStore.filter(all, selected);
        pageSize = CameraStore.layout(this);
        buildLayoutButtons();

        tabs.removeAllViews();
        tabs.setVisibility(groups.size() > 1 ? View.VISIBLE : View.GONE);
        if (groups.size() < 2) return;
        tabs.addView(tab("Tutte (" + all.size() + ")", CameraStore.ALL, selected));
        for (String g : groups) tabs.addView(tab(g + " (" + CameraStore.filter(all, g).size() + ")", g, selected));
    }

    /** Barra in basso: interruttore dei nomi e icona del layout attuale (OK apre la scelta 1 · 4 · 9 · 16). */
    private void buildLayoutButtons() {
        layoutButtons.removeAllViews();
        boolean names = CameraStore.showNames(this);
        TextView namesToggle = new TextView(this);
        namesToggle.setText("Aa");
        namesToggle.setTextSize(11);
        namesToggle.setTypeface(null, android.graphics.Typeface.BOLD);
        namesToggle.setTextColor(names ? Color.WHITE : 0xFF666666);
        namesToggle.setPaintFlags(names ? 0 : android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        namesToggle.setGravity(Gravity.CENTER);
        namesToggle.setIncludeFontPadding(false);
        namesToggle.setFocusable(true);
        namesToggle.setContentDescription(names ? "Nascondi i nomi" : "Mostra i nomi");
        namesToggle.setBackground(focusedBackground());
        namesToggle.setOnClickListener(v -> {
            CameraStore.setShowNames(this, !names);
            for (LiveView lv : views) lv.setLabelVisible(!names);
            buildLayoutButtons();
            layoutButtons.getChildAt(0).requestFocus();
        });
        layoutButtons.addView(namesToggle, iconParams());
        ImageView current = layoutIcon(pageSize, Color.WHITE);
        current.setContentDescription("Riquadri: " + pageSize);
        current.setOnClickListener(this::showLayoutPicker);
        layoutButtons.addView(current, iconParams());
    }

    private void showLayoutPicker(View anchor) {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(8, 8, 8, 8);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE202124);
        bg.setCornerRadius(12);
        box.setBackground(bg);
        PopupWindow popup = new PopupWindow(box, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        View selected = null;
        for (int n : LAYOUTS) {
            ImageView icon = layoutIcon(n, n == pageSize ? Color.WHITE : Color.GRAY);
            icon.setContentDescription(n + " riquadri");
            icon.setOnClickListener(v -> {
                popup.dismiss();
                CameraStore.setLayout(this, n);
                pageSize = n;
                page = 0;
                buildLayoutButtons();
                show();
            });
            box.addView(icon, iconParams());
            if (n == pageSize) selected = icon;
        }
        box.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popup.showAsDropDown(anchor, anchor.getWidth() - box.getMeasuredWidth(),
                -anchor.getHeight() - box.getMeasuredHeight() - 8);
        if (selected != null) selected.requestFocus();
    }

    private ImageView layoutIcon(int n, int color) {
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(new GridIcon((int) Math.round(Math.sqrt(n)), color));
        icon.setPadding(7, 4, 7, 4);
        icon.setFocusable(true);
        icon.setBackground(focusedBackground());
        return icon;
    }

    private LinearLayout.LayoutParams iconParams() {
        int px = (int) (15 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px + 14, px + 8);
        lp.gravity = Gravity.CENTER_VERTICAL;
        lp.leftMargin = 6;
        return lp;
    }

    private static StateListDrawable focusedBackground() {
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xFF2196F3);
        focused.setCornerRadius(8);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        return bg;
    }

    private boolean isTile(View v) {
        for (LiveView lv : views) if (lv.root == v) return true;
        return false;
    }

    private TextView tab(String label, String group, String selected) {
        boolean active = group.equals(selected);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(13);
        t.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4));
        t.setFocusable(true);
        t.setTextColor(Ui.textColors(active ? Color.WHITE : 0xFF8A8F98));
        t.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        t.setBackground(focusedBackground());
        t.setOnClickListener(v -> {
            CameraStore.setSelectedGroup(this, group);
            page = 0;
            loadGroup();
            show();
        });
        return t;
    }

    private int pageCount() {
        return (cams.size() + pageSize - 1) / pageSize;
    }

    private void stopAll() {
        for (LiveView v : views) v.stop();
        views.clear();
    }

    private void show() {
        stopAll();
        grid.removeAllViews();
        doorHint.setVisibility(View.GONE);
        if (cams.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Nessuna telecamera configurata\n\nPremi OK per cercarle in rete");
            empty.setTextColor(Color.WHITE);
            empty.setTextSize(24);
            empty.setGravity(Gravity.CENTER);
            empty.setFocusable(true);
            empty.setOnClickListener(v -> openSettings());
            grid.addView(empty, new LinearLayout.LayoutParams(-1, -1));
            footer.setText("≡ Menu: impostazioni");
            empty.requestFocus();
            return;
        }
        int from = page * pageSize, to = Math.min(cams.size(), from + pageSize);
        int gridCols = (int) Math.round(Math.sqrt(pageSize));
        int count = to - from;
        int cols = Math.min(gridCols, (int) Math.ceil(Math.sqrt(count)));
        int rows = (count + cols - 1) / cols;
        View first = null;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            grid.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
            for (int c = 0; c < cols; c++) {
                int idx = from + r * cols + c;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
                if (idx >= to) {
                    row.addView(new View(this), lp);
                    continue;
                }
                LiveView lv = new LiveView(this, cams.get(idx), false);
                lv.setLabelVisible(CameraStore.showNames(this));
                lv.root.setFocusable(true);
                lv.root.setPadding(6, 6, 6, 6);
                lv.root.setBackgroundColor(Color.BLACK);
                lv.root.setOnFocusChangeListener((v, has) -> {
                    if (has) {
                        showBorder(v);
                        doorHint.setText(v.getTag() instanceof Camera
                                ? "OK tenuto premuto: LD/HD e opzioni   ·   ⏯: apri porta"
                                : "OK tenuto premuto: LD/HD e opzioni");
                        doorHint.setVisibility(View.VISIBLE);
                    } else {
                        v.setBackgroundColor(Color.BLACK);
                        doorHint.setVisibility(View.GONE);
                    }
                });
                lv.root.setOnClickListener(v -> startActivity(
                        new Intent(this, PlayerActivity.class).putExtra(PlayerActivity.EXTRA_INDEX, idx)));
                Camera cam = cams.get(idx);
                if (cam.door) lv.root.setTag(cam);
                lv.root.setOnLongClickListener(v -> {
                    quickMenu(cam, idx);
                    return true;
                });
                row.addView(lv.root, lp);
                views.add(lv);
                if (first == null) first = lv.root;
            }
        }
        String hint = "OK: schermo intero  ·  ≡ Menu: impostazioni";
        if (tabs.getVisibility() == View.VISIBLE) hint += "  ·  ▲ gruppi";
        hint += "  ·  ▼ riquadri";
        if (pageCount() > 1) hint += "  ·  Pagina " + (page + 1) + "/" + pageCount() + " (◀ ▶)";
        footer.setText(hint);
        first.requestFocus();
        // dopo il layout, così il focus di default non finisce sulle icone della barra in basso
        View firstTile = first;
        grid.post(firstTile::requestFocus);
        for (LiveView v : views) v.start();
    }

    /** Menu rapido di una telecamera (OK tenuto premuto): qualità LD/HD, schermo intero, apriporta. */
    private void quickMenu(Camera cam, int idx) {
        ModernDialog m = new ModernDialog(this)
                .setTitle(cam.label())
                .setMessage("Qualità in griglia: " + (cam.highQuality ? "HD (flusso principale)" : "LD (flusso secondario)"))
                .setPositiveButton(cam.highQuality ? "Passa a LD" : "Passa a HD", (d, w) -> {
                    cam.highQuality = !cam.highQuality;
                    CameraStore.updateQuality(this, cam);
                    show();
                })
                .setNeutralButton("Schermo intero", (d, w) -> startActivity(
                        new Intent(this, PlayerActivity.class).putExtra(PlayerActivity.EXTRA_INDEX, idx)));
        if (cam.door) m.setNegativeButton("Apri porta", (d, w) -> DoorOpener.ask(this, cam));
        else m.setNegativeButton("Chiudi", null);
        m.show();
    }

    /** Bordo blu sulla telecamera selezionata, che sparisce dopo {@link #BORDER_MS}. */
    private void showBorder(View v) {
        if (bordered != null && bordered != v) bordered.setBackgroundColor(Color.BLACK);
        bordered = v;
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.BLACK);
        border.setStroke(6, 0xFF2196F3);
        v.setBackground(border);
        handler.removeCallbacks(hideBorder);
        handler.postDelayed(hideBorder, BORDER_MS);
    }

    private void changePage(int delta) {
        int n = pageCount();
        if (n <= 1) return;
        page = (page + delta + n) % n;
        show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            switch (e.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    View focused = getCurrentFocus();
                    if (focused != null && focused.getTag() instanceof Camera) {
                        DoorOpener.ask(this, (Camera) focused.getTag());
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_SETTINGS:
                    openSettings();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    changePage(1);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    changePage(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    View current = getCurrentFocus();
                    if (current != null && isTile(current)) showBorder(current);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    View f = getCurrentFocus();
                    if (f != null && isTile(f)) showBorder(f);
                    boolean right = e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
                    if (pageCount() > 1 && f != null && isTile(f)
                            && f.focusSearch(right ? View.FOCUS_RIGHT : View.FOCUS_LEFT) == null) {
                        changePage(right ? 1 : -1);
                        return true;
                    }
                    break;
            }
        }
        return super.dispatchKeyEvent(e);
    }
}
