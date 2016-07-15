package tevonial.aocr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Connor on 7/5/2016.
 */
public abstract class DrawingView extends View {
    public int width;
    public  int height;
    private Bitmap mBitmap;
    private Path mPath;
    Activity activity;
    private Paint circlePaint;
    private Path circlePath;
    private Paint mPaint;

    abstract void onTouchUp(int[] pixels);

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getMeasuredHeight();
        int width = getMeasuredWidth();

        int dim = (height < width) ? height : width;
        setMeasuredDimension(dim, dim);
    }

    public DrawingView(Activity c) {
        super(c);
        setBackground(getResources().getDrawable(R.drawable.border));
        activity =c;
        mPath = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(100);

        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w; height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawPath( mPath,  mPaint);
        canvas.drawPath( circlePath,  circlePaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    public void reset() {
        mPath.reset();
        invalidate();
    }

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;

            circlePath.reset();
            circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        circlePath.reset();
        // commit the path to our offscreen
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        (new Canvas(mBitmap)).drawPath(mPath, mPaint);
        // kill this so we don't double draw

        Bitmap scale = Bitmap.createScaledBitmap(mBitmap, 28, 28, false);

        int[] pixels = new int[scale.getHeight() * scale.getWidth()];
        scale.getPixels(pixels, 0, scale.getWidth(), 0, 0, scale.getWidth(), scale.getHeight());
        pixels = centerPixels(pixels, 28, 28);

        onTouchUp(centerPixels(pixels, 28, 28));

        //scale.setPixels(pixels, 0, scale.getWidth(), 0, 0, scale.getWidth(), scale.getHeight());
        //MainActivity.pre.setImageBitmap(scale);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }


    private int[] centerPixels(int[] pixels, int w, int h) {
        if (pixels.length != w*h) return null;

        int center_x = w / 2, center_y = h / 2;

        int x = 0, y = 0;
        int total = 0; int pixel;

        ArrayList<ArrayList<Integer>> matrix = new ArrayList<>();

        for (int row=0; row<h; row++) {
            matrix.add(new ArrayList<Integer>());
            for (int col=0; col<w; col++) {
                pixel = pixels[(row * w) + col];
                matrix.get(row).add(pixel);
                if (pixel != 0) {
                    x += col;
                    y += row;
                    total++;
                }
            }
        }

        if (total > 0) {
            x /= total;
            y /= total;
            x = center_x - x;
            y = center_y - y;

            Collections.rotate(matrix, y);
            for (int r = 0; r < matrix.size(); r++) {
                ArrayList<Integer> row = matrix.get(r);
                Collections.rotate(row, x);
                for (int i = 0; i < w; i++) {
                    pixels[(r * w) + i] = row.get(i);
                }
            }
        }

        return pixels;
    }

}