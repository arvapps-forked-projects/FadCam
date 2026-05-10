package com.fadcam.ui.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Rounded pill badge span for chip count numbers. */
public class PillBadgeSpan extends ReplacementSpan {
    private final int bgColor;
    private final int fgColor;
    private final int padH;
    private final int cornerRadius;

    public PillBadgeSpan(int bgColor, int fgColor, float density) {
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.padH = (int)(6f * density);
        this.cornerRadius = Math.round(6f * density);
    }

    @Override public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                                 @Nullable Paint.FontMetricsInt fmi) {
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        if (fmi != null) {
            fmi.ascent = fm.ascent;
            fmi.descent = fm.descent;
            fmi.top = fm.top;
            fmi.bottom = fm.bottom;
        }
        float w = paint.measureText(text, start, end);
        return Math.round(w + padH * 2);
    }

    @Override public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                               float x, int top, int y, int bottom, @NonNull Paint paint) {
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        float textH = fm.descent - fm.ascent;
        float textW = paint.measureText(text, start, end);
        float badgeH = textH * 1.3f;
        float badgeW = textW + padH * 2;
        float cy = (top + bottom) / 2f;

        RectF rect = new RectF(x, cy - badgeH / 2f, x + badgeW, cy + badgeH / 2f);

        int savedColor = paint.getColor();
        Paint.Style savedStyle = paint.getStyle();

        paint.setColor(bgColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        paint.setColor(fgColor);
        // Center text vertically in badge: baseline = cy - (ascent + descent) / 2
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, start, end, x + padH, baseline, paint);

        paint.setColor(savedColor);
        paint.setStyle(savedStyle);
    }
}
