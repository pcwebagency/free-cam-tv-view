package it.pcwebagency.telecamere;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Stile grafico condiviso: colori, pulsanti a pillola, schede ed etichette. */
public final class Ui {
    public static final int BG = 0xFF0F1115;
    public static final int SURFACE = 0xFF181B22;
    public static final int SURFACE_FOCUS = 0xFF1E3A5F;
    public static final int ACCENT = 0xFF2196F3;
    public static final int TEXT = 0xFFECEFF1;
    public static final int TEXT_DIM = 0xFF9AA0A6;
    public static final int WARN = 0xFFFFB74D;

    private Ui() {}

    public static int dp(Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable shape(int color, float radius, int stroke, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke > 0) d.setStroke(stroke, strokeColor);
        return d;
    }

    /** Sfondo con stato "selezionato" dal telecomando. */
    public static StateListDrawable focusable(Context ctx, int normal, int focused, float radiusDp, boolean border) {
        float r = dp(ctx, radiusDp);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused},
                shape(focused, r, border ? dp(ctx, 2) : 0, ACCENT));
        s.addState(new int[]{}, shape(normal, r, 0, 0));
        return s;
    }

    /** Testo bianco quando l'elemento è selezionato, altrimenti {@code normal}. */
    public static ColorStateList textColors(int normal) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{0xFFFFFFFF, normal});
    }

    /** Leggero ingrandimento dell'elemento selezionato. */
    public static void zoomOnFocus(View v, float scale) {
        v.setOnFocusChangeListener((view, has) ->
                view.animate().scaleX(has ? scale : 1f).scaleY(has ? scale : 1f).setDuration(120).start());
    }

    public static TextView text(Context ctx, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    /** Pulsante arrotondato "a pillola". */
    public static TextView pill(Context ctx, String label, View.OnClickListener l) {
        TextView t = text(ctx, label, 14, TEXT, true);
        t.setTextColor(textColors(TEXT));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(ctx, 18), dp(ctx, 9), dp(ctx, 18), dp(ctx, 9));
        t.setBackground(focusable(ctx, SURFACE, ACCENT, 20, false));
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnClickListener(l);
        zoomOnFocus(t, 1.06f);
        return t;
    }

    /** Piccola etichetta colorata, es. "HD" o "Apriporta". */
    public static TextView badge(Context ctx, String label, int color) {
        TextView t = text(ctx, label, 11, color, true);
        t.setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2));
        GradientDrawable d = shape(0x00000000, dp(ctx, 10), dp(ctx, 1), color);
        t.setBackground(d);
        return t;
    }

    public static LinearLayout.LayoutParams margins(Context ctx, int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMargins(dp(ctx, l), dp(ctx, t), dp(ctx, r), dp(ctx, b));
        return lp;
    }
}
