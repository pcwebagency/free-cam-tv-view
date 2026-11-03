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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Griglia live 2×2 delle telecamere del gruppo scelto, con pagine se sono più di quattro. */
public class MainActivity extends Activity {
    private static final int PAGE_SIZE = 4;
    private static final int BORDER_MS = 1000;

    private final List<LiveView> views = new ArrayList<>();
    private List<Camera> cams = new ArrayList<>();
    private int page;
    private LinearLayout grid;
    private LinearLayout tabs;
    private TextView footer;
    private TextView doorHint;
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
        doorHint.setText("⏯ o OK tenuto premuto: apri porta");
        doorHint.setVisibility(View.GONE);
        bottom.addView(doorHint, new LinearLayout.LayoutParams(-2, -2));
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

        tabs.removeAllViews();
        tabs.setVisibility(groups.size() > 1 ? View.VISIBLE : View.GONE);
        if (groups.size() < 2) return;
        tabs.addView(tab("Tutte (" + all.size() + ")", CameraStore.ALL, selected));
        for (String g : groups) tabs.addView(tab(g + " (" + CameraStore.filter(all, g).size() + ")", g, selected));
    }

    private TextView tab(String label, String group, String selected) {
        boolean active = group.equals(selected);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(16);
        t.setPadding(28, 8, 28, 8);
        t.setFocusable(true);
        t.setTextColor(active ? Color.WHITE : Color.GRAY);
        t.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xFF2196F3);
        focused.setCornerRadius(8);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        t.setBackground(bg);
        t.setOnClickListener(v -> {
            CameraStore.setSelectedGroup(this, group);
            page = 0;
            loadGroup();
            show();
        });
        return t;
    }

    private int pageCount() {
        return (cams.size() + PAGE_SIZE - 1) / PAGE_SIZE;
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
        int from = page * PAGE_SIZE, to = Math.min(cams.size(), from + PAGE_SIZE);
        int count = to - from, cols = count == 1 ? 1 : 2, rows = (count + cols - 1) / cols;
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
                lv.root.setFocusable(true);
                lv.root.setPadding(6, 6, 6, 6);
                lv.root.setBackgroundColor(Color.BLACK);
                lv.root.setOnFocusChangeListener((v, has) -> {
                    if (has) {
                        showBorder(v);
                        doorHint.setVisibility(v.getTag() instanceof Camera ? View.VISIBLE : View.GONE);
                    } else {
                        v.setBackgroundColor(Color.BLACK);
                    }
                });
                lv.root.setOnClickListener(v -> startActivity(
                        new Intent(this, PlayerActivity.class).putExtra(PlayerActivity.EXTRA_INDEX, idx)));
                Camera cam = cams.get(idx);
                if (cam.door) {
                    lv.root.setTag(cam);
                    lv.root.setOnLongClickListener(v -> {
                        DoorOpener.ask(this, cam);
                        return true;
                    });
                }
                row.addView(lv.root, lp);
                views.add(lv);
                if (first == null) first = lv.root;
            }
        }
        String hint = "OK: schermo intero  ·  ≡ Menu: impostazioni";
        if (tabs.getVisibility() == View.VISIBLE) hint += "  ·  ▲ gruppi";
        if (pageCount() > 1) hint += "  ·  Pagina " + (page + 1) + "/" + pageCount() + " (◀ ▶ o ⏪ ⏩)";
        footer.setText(hint);
        first.requestFocus();
        for (LiveView v : views) v.start();
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
                    if (current != null) showBorder(current);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    View f = getCurrentFocus();
                    if (f != null) showBorder(f);
                    boolean right = e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
                    if (pageCount() > 1 && f != null
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
