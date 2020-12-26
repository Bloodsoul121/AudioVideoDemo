package com.example.avd.media_player.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class RoundImageView extends AppCompatImageView {

    private PaintFlagsDrawFilter mPaintFlagsDrawFilter;

    public RoundImageView(Context context) {
        super(context);
        init();
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mFilterPaint.setColor(0x7f000000);
        mFilterPaint.setAntiAlias(true);
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    }

    private float mMarginLeft;
    private float mMarginTop;
    private boolean mIsPressed;
    private Paint mFilterPaint = new Paint();
    private Paint mPaint = new Paint();
    private OnClickListener mListener;

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
        super.setOnClickListener(listener);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mListener == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsPressed = true;
                break;
            case MotionEvent.ACTION_UP:
                mIsPressed = false;
                performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsPressed = false;
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        if (drawable.getClass() == NinePatchDrawable.class) {
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap == null) {
            return;
        }

        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        canvas.setDrawFilter(mPaintFlagsDrawFilter);
        // 控件默认长、宽
        float defaultWidth = getWidth();
        float defaultHeight = getHeight();
        mMarginLeft = 0;
        mMarginTop = 0;

        float radius = (defaultWidth < defaultHeight ? defaultWidth : defaultHeight) / 2;

        if (radius <= 0) {
            return;
        }

        drawCroppedRoundBitmap(canvas, bitmap, radius);

        // 增加按下時變暗效果
        if (mIsPressed) {
            canvas.drawCircle(defaultWidth / 2 + mMarginLeft, defaultHeight / 2 + mMarginTop, radius, mFilterPaint);
        }
    }

    /**
     * 获取裁剪后的圆形图片
     *
     * @param r 半径
     */
    public void drawCroppedRoundBitmap(Canvas canvas, Bitmap bmp, float r) {
        int diameter = (int) (r * 2);
        // 为了防止宽高不相等，造成圆形图片变形，因此截取长方形中处于中间位置最大的正方形图片
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        Rect rect = new Rect(0, 0, bmpWidth, bmpHeight);
        if (bmpHeight > bmpWidth) {// 高大于宽
            rect.set(0, (bmpHeight - bmpWidth) / 2, bmpWidth, (bmpHeight - bmpWidth) / 2 + bmpWidth);
        } else if (bmpHeight < bmpWidth) {// 宽大于高
            rect.set((bmpWidth - bmpHeight) / 2, 0, (bmpWidth - bmpHeight) / 2 + bmpHeight, bmpHeight);
        }

        int borderThickness = 0;
        RectF layerRect = new RectF(mMarginLeft + borderThickness, mMarginTop + borderThickness, mMarginLeft + borderThickness + diameter, mMarginTop + borderThickness + diameter);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.saveLayer(layerRect, null, Canvas.ALL_SAVE_FLAG);
        canvas.drawCircle(layerRect.centerX(), layerRect.centerY(), layerRect.width() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bmp, rect, layerRect, paint);
        canvas.restore();
    }
}
