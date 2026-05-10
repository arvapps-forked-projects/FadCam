package com.fadcam.ui.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Draws the count number inside a rounded rect badge — material design style. */
public class ChipCountSpan extends ReplacementSpan {
    private final int bgColor;
    private final int fgColor;
    private final int paddingH;
    private final int radius;

    public ChipCountSpan(int bgColor, int fgColor, float density) {
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.paddingH = (int) (6 * density);
        this.radius = (int) (8 * density);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fmi) {
        float textW = paint.measureText(text, start, end);
        int total = (int) (textW + paddingH * 2);
        if (fmi != null) {
            fmi.ascent = paint.getFontMetricsInt().ascent;
            fmi.descent = paint.getFontMetricsInt().descent;
        }
        return total;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
        float textH = fmi.descent - fmi.ascent;
        float textW = paint.measureText(text, start, end);
        float padH = paddingH;
        float badgeH = textH * 1.3f;
        float badgeW = textW + padH * 2;
        float cy = (top + bottom) / 2f;

        RectF rect = new RectF(x, cy - badgeH / 2f, x + badgeW, cy + badgeH / 2f);

        paint.setColor(bgColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, radius, radius, paint);

        paint.setColor(fgColor);
        canvas.drawText(text, start, end, x + padH, cy - (fmi.ascent + fmi.descent) / 2f, paint);
    }
}
