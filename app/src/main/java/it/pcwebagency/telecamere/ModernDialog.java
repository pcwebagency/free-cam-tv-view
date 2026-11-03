package it.pcwebagency.telecamere;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Finestra di dialogo con lo stile dell'app (scheda scura arrotondata, pulsanti a pillola).
 * Stessi metodi principali di AlertDialog.Builder: Fire OS impone il proprio tema alle
 * AlertDialog di sistema.
 */
public class ModernDialog {
    public static final int BUTTON_POSITIVE = 0;
    public static final int BUTTON_NEUTRAL = 1;
    public static final int BUTTON_NEGATIVE = 2;

    private final Activity activity;
    private String title;
    private String message;
    private View view;
    private boolean cancelable = true;
    private int focus = BUTTON_POSITIVE;
    private final String[] labels = new String[3];
    private final DialogInterface.OnClickListener[] listeners = new DialogInterface.OnClickListener[3];
    private String[] items;
    private boolean[] checked;
    private DialogInterface.OnMultiChoiceClickListener multiListener;

    public ModernDialog(Activity activity) {
        this.activity = activity;
    }

    public ModernDialog setTitle(String t) { title = t; return this; }
    public ModernDialog setMessage(String m) { message = m; return this; }
    public ModernDialog setView(View v) { view = v; return this; }
    public ModernDialog setCancelable(boolean c) { cancelable = c; return this; }
    /** Pulsante che riceve il focus all'apertura (default: positivo). */
    public ModernDialog setFocus(int which) { focus = which; return this; }

    public ModernDialog setPositiveButton(String l, DialogInterface.OnClickListener cb) { return button(BUTTON_POSITIVE, l, cb); }
    public ModernDialog setNeutralButton(String l, DialogInterface.OnClickListener cb) { return button(BUTTON_NEUTRAL, l, cb); }
    public ModernDialog setNegativeButton(String l, DialogInterface.OnClickListener cb) { return button(BUTTON_NEGATIVE, l, cb); }

    private ModernDialog button(int which, String l, DialogInterface.OnClickListener cb) {
        labels[which] = l;
        listeners[which] = cb;
        return this;
    }

    public ModernDialog setMultiChoiceItems(String[] items, boolean[] checked, DialogInterface.OnMultiChoiceClickListener cb) {
        this.items = items;
        this.checked = checked;
        this.multiListener = cb;
        return this;
    }

    public Dialog show() {
        Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCancelable(cancelable);

        FrameLayout scrim = new FrameLayout(activity);
        scrim.setBackgroundColor(0xB3000000);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(activity, 28);
        card.setPadding(pad, Ui.dp(activity, 24), pad, Ui.dp(activity, 20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1F26);
        bg.setCornerRadius(Ui.dp(activity, 18));
        card.setBackground(bg);

        if (title != null) card.addView(Ui.text(activity, title, 20, Ui.TEXT, true), Ui.margins(activity, -1, -2, 0, 0, 0, 12));

        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        if (message != null) body.addView(Ui.text(activity, message, 15, Ui.TEXT_DIM, false));
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                int which = i;
                CheckBox cb = new CheckBox(activity);
                cb.setText(items[i]);
                cb.setTextColor(Ui.TEXT);
                cb.setTextSize(15);
                cb.setChecked(checked[i]);
                cb.setPadding(Ui.dp(activity, 8), Ui.dp(activity, 8), 0, Ui.dp(activity, 8));
                cb.setBackground(Ui.focusable(activity, 0x00000000, Ui.SURFACE_FOCUS, 10, false));
                cb.setOnCheckedChangeListener((b, on) -> {
                    checked[which] = on;
                    if (multiListener != null) multiListener.onClick(d, which, on);
                });
                body.addView(cb, new LinearLayout.LayoutParams(-1, -2));
            }
        }
        if (view != null) body.addView(view, new LinearLayout.LayoutParams(-1, -2));

        int maxBody = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.62f);
        ScrollView scroll = new ScrollView(activity) {
            @Override
            protected void onMeasure(int w, int h) {
                super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxBody, MeasureSpec.AT_MOST));
            }
        };
        scroll.addView(body);
        card.addView(scroll, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setGravity(Gravity.END);
        // spazio per l'ingrandimento del pulsante selezionato
        buttons.setPadding(Ui.dp(activity, 8), Ui.dp(activity, 4), Ui.dp(activity, 8), Ui.dp(activity, 4));
        List<View> created = new ArrayList<>(3);
        for (int which = 0; which < 3; which++) created.add(null);
        for (int which : new int[]{BUTTON_POSITIVE, BUTTON_NEUTRAL, BUTTON_NEGATIVE}) {
            if (labels[which] == null) continue;
            int w = which;
            TextView b = Ui.pill(activity, labels[which], v -> {
                d.dismiss();
                if (listeners[w] != null) listeners[w].onClick(d, w);
            });
            if (which == BUTTON_POSITIVE) b.setTextColor(Ui.textColors(0xFF64B5F6));
            buttons.addView(b, Ui.margins(activity, -2, -2, 12, 0, 0, 0));
            created.set(which, b);
        }
        if (buttons.getChildCount() > 0) card.addView(buttons, Ui.margins(activity, -1, -2, 0, 20, 0, 0));

        int width = (int) Math.min(activity.getResources().getDisplayMetrics().widthPixels * 0.62f, Ui.dp(activity, 760));
        scrim.addView(card, new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        d.setContentView(scrim);
        Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        d.show();
        View f = created.get(focus);
        if (f == null) for (View v : created) if (v != null) { f = v; break; }
        if (f != null) f.requestFocus();
        return d;
    }
}
