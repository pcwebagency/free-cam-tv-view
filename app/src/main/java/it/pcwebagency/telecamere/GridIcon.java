package it.pcwebagency.telecamere;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Icona di layout: griglia n×n di riquadri arrotondati (1, 4, 9 telecamere). */
public class GridIcon extends Drawable {
    private final int n;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GridIcon(int n, int color) {
        this.n = n;
        paint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF b = new RectF(getBounds());
        float gap = b.width() * 0.08f;
        float cell = (b.width() - gap * (n - 1)) / n;
        float cellH = (b.height() - gap * (n - 1)) / n;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                float x = b.left + c * (cell + gap), y = b.top + r * (cellH + gap);
                canvas.drawRoundRect(new RectF(x, y, x + cell, y + cellH), cell * 0.18f, cell * 0.18f, paint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
