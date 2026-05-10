package com.fadcam.ui.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Material Design 3 pill badge for chip count numbers. */
public class ChipBadgeDrawable extends Drawable {
    private final String text;
    private final int bgColor;
    private final int fgColor;
    private final float textSizePx;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ChipBadgeDrawable(String text, int bgColor, int fgColor, float textSizePx) {
        this.text = text;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        this.textSizePx = textSizePx;
        textPaint.setTextSize(textSizePx);
        textPaint.setColor(fgColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        bgPaint.setColor(bgColor);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;

        float h = bounds.height();
        float cy = bounds.exactCenterY();
        float cx = bounds.exactCenterX();
        float radius = h / 2f;

        // Rounded pill background
        canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                radius, radius, bgPaint);

        // Centered text
        float baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(text, cx, baseline, textPaint);
    }

    @Override public int getIntrinsicWidth() {
        float textW = textPaint.measureText(text);
        float h = getIntrinsicHeight();
        float padX = h * 0.5f;
        return Math.round(textW + padX * 2);
    }

    @Override public int getIntrinsicHeight() {
        return Math.round(textSizePx * 1.5f);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(@Nullable ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
