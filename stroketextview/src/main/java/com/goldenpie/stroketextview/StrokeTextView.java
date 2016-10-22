package com.goldenpie.stroketextview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

public class StrokeTextView extends AppCompatTextView {
    private ArrayList<Shadow> outerShadows;
    private ArrayList<Shadow> innerShadows;

    private WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;

    private Canvas tempCanvas;
    private Bitmap tempBitmap;

    private Drawable foregroundDrawable;

    private float strokeWidth;
    private Integer strokeColor;
    private Join strokeJoin;
    private float strokeMiter;

    private int[] lockedCompoundPadding;
    private boolean frozen = false;

    private final PorterDuffXfermode SRC_ATOP = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
    private final PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    public StrokeTextView(Context context) {
        super(context);
        init(null);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        outerShadows = new ArrayList<>();
        innerShadows = new ArrayList<>();
        if (canvasStore == null) {
            canvasStore = new WeakHashMap<>();
        }

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StrokeTextView);

            String typefaceName = a.getString(R.styleable.StrokeTextView_str_typeface);
            if (typefaceName != null) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                setTypeface(tf);
            }

            if (a.hasValue(R.styleable.StrokeTextView_str_foreground)) {
                Drawable foreground = a.getDrawable(R.styleable.StrokeTextView_str_foreground);
                if (foreground != null) {
                    this.setForegroundDrawable(foreground);
                } else {
                    this.setTextColor(a.getColor(R.styleable.StrokeTextView_str_foreground, 0xff000000));
                }
            }

            if (a.hasValue(R.styleable.StrokeTextView_str_background)) {
                Drawable background = a.getDrawable(R.styleable.StrokeTextView_str_background);
                if (background != null) {
                    this.setBackground(background);
                } else {
                    this.setBackgroundColor(a.getColor(R.styleable.StrokeTextView_str_background, 0xff000000));
                }
            }

            if (a.hasValue(R.styleable.StrokeTextView_str_innerShadowColor)) {
                this.addInnerShadow(a.getDimensionPixelSize(R.styleable.StrokeTextView_str_innerShadowRadius, 0),
                        a.getDimensionPixelOffset(R.styleable.StrokeTextView_str_innerShadowDx, 0),
                        a.getDimensionPixelOffset(R.styleable.StrokeTextView_str_innerShadowDy, 0),
                        a.getColor(R.styleable.StrokeTextView_str_innerShadowColor, 0xff000000));
            }

            if (a.hasValue(R.styleable.StrokeTextView_str_outerShadowColor)) {
                this.addOuterShadow(a.getDimensionPixelSize(R.styleable.StrokeTextView_str_outerShadowRadius, 0),
                        a.getDimensionPixelOffset(R.styleable.StrokeTextView_str_outerShadowDx, 0),
                        a.getDimensionPixelOffset(R.styleable.StrokeTextView_str_outerShadowDy, 0),
                        a.getColor(R.styleable.StrokeTextView_str_outerShadowColor, 0xff000000));
            }

            if (a.hasValue(R.styleable.StrokeTextView_str_strokeColor)) {
                float strokeWidth = a.getDimensionPixelSize(R.styleable.StrokeTextView_str_strokeWidth, 1);
                int strokeColor = a.getColor(R.styleable.StrokeTextView_str_strokeColor, 0xff000000);
                float strokeMiter = a.getDimensionPixelSize(R.styleable.StrokeTextView_str_strokeMiter, 10);
                Join strokeJoin = null;
                switch (a.getInt(R.styleable.StrokeTextView_str_strokeJoinStyle, 0)) {
                    case (0):
                        strokeJoin = Join.MITER;
                        break;
                    case (1):
                        strokeJoin = Join.BEVEL;
                        break;
                    case (2):
                        strokeJoin = Join.ROUND;
                        break;
                }
                this.setStroke(strokeWidth, strokeColor, strokeJoin, strokeMiter);
            }

            a.recycle();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                && (innerShadows.size() > 0
                || foregroundDrawable != null
        )
                ) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    public void setStroke(float width, int color, Join join, float miter) {
        strokeWidth = width;
        strokeColor = color;
        strokeJoin = join;
        strokeMiter = miter;
    }

    public void setStroke(float width, int color) {
        setStroke(width, color, Join.MITER, 10);
    }

    public void addOuterShadow(float r, float dx, float dy, int color) {
        if (r == 0) {
            r = 0.0001f;
        }
        outerShadows.add(new Shadow(r, dx, dy, color));
    }

    public void addInnerShadow(float r, float dx, float dy, int color) {
        if (r == 0) {
            r = 0.0001f;
        }
        innerShadows.add(new Shadow(r, dx, dy, color));
    }

    public void clearInnerShadows() {
        innerShadows.clear();
    }

    public void clearOuterShadows() {
        outerShadows.clear();
    }

    public void setForegroundDrawable(Drawable d) {
        this.foregroundDrawable = d;
    }

    public Drawable getForeground() {
        return this.foregroundDrawable == null ? null : new ColorDrawable(this.getCurrentTextColor());
    }


    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        freeze();
        Drawable restoreBackground = this.getBackground();
        Drawable[] restoreDrawables = this.getCompoundDrawables();
        int restoreColor = this.getCurrentTextColor();

        this.setCompoundDrawables(null, null, null, null);

        for (Shadow shadow : outerShadows) {
            this.setShadowLayer(shadow.r, shadow.dx, shadow.dy, shadow.color);
            super.onDraw(canvas);
        }
        this.setShadowLayer(0, 0, 0, 0);
        this.setTextColor(restoreColor);

        if (this.foregroundDrawable != null && this.foregroundDrawable instanceof BitmapDrawable) {
            generateTempCanvas();
            super.onDraw(tempCanvas);
            Paint paint = ((BitmapDrawable) this.foregroundDrawable).getPaint();
            paint.setXfermode(SRC_ATOP);
            this.foregroundDrawable.setBounds(canvas.getClipBounds());
            this.foregroundDrawable.draw(tempCanvas);
            canvas.drawBitmap(tempBitmap, 0, 0, null);
            tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }

        if (strokeColor != null) {
            TextPaint paint = this.getPaint();
            paint.setStyle(Style.STROKE);
            paint.setStrokeJoin(strokeJoin);
            paint.setStrokeMiter(strokeMiter);
            this.setTextColor(strokeColor);
            paint.setStrokeWidth(strokeWidth);
            super.onDraw(canvas);
            paint.setStyle(Style.FILL);
            this.setTextColor(restoreColor);
        }
        if (innerShadows.size() > 0) {
            generateTempCanvas();
            TextPaint paint = this.getPaint();
            for (Shadow shadow : innerShadows) {
                this.setTextColor(shadow.color);
                super.onDraw(tempCanvas);
                this.setTextColor(0xFF000000);
                paint.setXfermode(DST_OUT);
                paint.setMaskFilter(new BlurMaskFilter(shadow.r, BlurMaskFilter.Blur.NORMAL));

                tempCanvas.save();
                tempCanvas.translate(shadow.dx, shadow.dy);
                super.onDraw(tempCanvas);
                tempCanvas.restore();
                canvas.drawBitmap(tempBitmap, 0, 0, null);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                paint.setXfermode(null);
                paint.setMaskFilter(null);
                this.setTextColor(restoreColor);
                this.setShadowLayer(0, 0, 0, 0);
            }
        }


        this.setCompoundDrawablesWithIntrinsicBounds(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
        this.setBackground(restoreBackground);
        this.setTextColor(restoreColor);

        unfreeze();
    }

    private void generateTempCanvas() {
        String key = String.format(Locale.getDefault(), "%dx%d", getWidth(), getHeight());
        Pair<Canvas, Bitmap> stored = canvasStore.get(key);
        if (stored != null) {
            tempCanvas = stored.first;
            tempBitmap = stored.second;
        } else {
            tempCanvas = new Canvas();
            tempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas.setBitmap(tempBitmap);
            canvasStore.put(key, new Pair<>(tempCanvas, tempBitmap));
        }
    }


    // Keep these things locked while onDraw in processing
    public void freeze() {
        lockedCompoundPadding = new int[]{
                getCompoundPaddingLeft(),
                getCompoundPaddingRight(),
                getCompoundPaddingTop(),
                getCompoundPaddingBottom()
        };
        frozen = true;
    }

    public void unfreeze() {
        frozen = false;
    }


    @Override
    public void requestLayout() {
        if (!frozen) super.requestLayout();
    }

    @Override
    public void postInvalidate() {
        if (!frozen) super.postInvalidate();
    }

    @Override
    public void postInvalidate(int left, int top, int right, int bottom) {
        if (!frozen) super.postInvalidate(left, top, right, bottom);
    }

    @Override
    public void invalidate() {
        if (!frozen) super.invalidate();
    }

    @Override
    public void invalidate(Rect rect) {
        if (!frozen) super.invalidate(rect);
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        if (!frozen) super.invalidate(l, t, r, b);
    }

    @Override
    public int getCompoundPaddingLeft() {
        return !frozen ? super.getCompoundPaddingLeft() : lockedCompoundPadding[0];
    }

    @Override
    public int getCompoundPaddingRight() {
        return !frozen ? super.getCompoundPaddingRight() : lockedCompoundPadding[1];
    }

    @Override
    public int getCompoundPaddingTop() {
        return !frozen ? super.getCompoundPaddingTop() : lockedCompoundPadding[2];
    }

    @Override
    public int getCompoundPaddingBottom() {
        return !frozen ? super.getCompoundPaddingBottom() : lockedCompoundPadding[3];
    }

    public static class Shadow {
        float r;
        float dx;
        float dy;
        int color;

        Shadow(float r, float dx, float dy, int color) {
            this.r = r;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
        }
    }
}