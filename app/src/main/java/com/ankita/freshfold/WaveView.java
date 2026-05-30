package com.ankita.freshfold;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View {

    private Paint wavePaint1, wavePaint2;
    private Path wavePath1, wavePath2;
    private float waveOffset = 0f;
    private boolean animating = true;

    private final Runnable waveRunnable = new Runnable() {
        @Override
        public void run() {
            waveOffset += 2f;
            if (waveOffset > getWidth()) waveOffset = 0f;
            invalidate();
            if (animating) postDelayed(this, 16);
        }
    };

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        wavePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint1.setColor(0x33FFFFFF); // semi-transparent white
        wavePaint1.setStyle(Paint.Style.FILL);

        wavePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint2.setColor(0x1AFFFFFF); // lighter wave
        wavePaint2.setStyle(Paint.Style.FILL);

        wavePath1 = new Path();
        wavePath2 = new Path();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animating = true;
        post(waveRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animating = false;
        removeCallbacks(waveRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // Wave 1
        wavePath1.reset();
        wavePath1.moveTo(-waveOffset, h * 0.5f);
        for (int x = 0; x <= w + 100; x += 20) {
            float y = (float) (h * 0.45f + Math.sin((x + waveOffset) * 0.02f) * h * 0.15f);
            wavePath1.lineTo(x - waveOffset % 20, y);
        }
        wavePath1.lineTo(w, h);
        wavePath1.lineTo(0, h);
        wavePath1.close();
        canvas.drawPath(wavePath1, wavePaint1);

        // Wave 2 (offset)
        wavePath2.reset();
        wavePath2.moveTo(0, h * 0.6f);
        for (int x = 0; x <= w + 100; x += 20) {
            float y = (float) (h * 0.55f + Math.sin((x + waveOffset * 1.3f) * 0.025f) * h * 0.12f);
            wavePath2.lineTo(x, y);
        }
        wavePath2.lineTo(w, h);
        wavePath2.lineTo(0, h);
        wavePath2.close();
        canvas.drawPath(wavePath2, wavePaint2);
    }
}
