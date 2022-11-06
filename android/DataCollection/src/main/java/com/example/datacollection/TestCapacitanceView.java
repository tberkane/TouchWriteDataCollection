package com.example.datacollection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;

import com.example.capimage.CapImage;
import com.example.capimage.CapImageStreamer;
import com.example.capimage.CapacitiveImage;
import com.example.capimage.TouchDetector;
import com.example.capimage.TouchTracker;
import com.example.capimage.TouchTracker.TouchMap;
import com.example.capimage.TouchTracker.TouchMapCallback;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestCapacitanceView extends View implements TouchMapCallback, CapImageStreamer.CapImageCallback {
    static {
        OpenCVLoader.initDebug();
    }

    private static final String TAG = TestCapacitanceView.class.getSimpleName();


    // --------------------------------------------------------
    // --------------------------------------------------------
    //                        OPTIONS
    // --------------------------------------------------------
    // --------------------------------------------------------
    private final boolean showText = false;
    private final boolean showMatrix = true;
    private final boolean showNegativeValues = false;
    private final boolean showTouchPoints = true;
    private int viewOffset = 0;

    private short[] capImage;

    private final CapImageStreamer capStreamer = new CapImageStreamer(this);
    private final TouchDetector touchDetector = new TouchDetector();
    private final TouchTracker touchTracker = new TouchTracker();
    private TouchMap touchMap;
    private final boolean rightHanded;

    public TestCapacitanceView(Context context, boolean rightHanded) {
        super(context);
        this.rightHanded = rightHanded;
    }


    @Override
    public void onCapImage(final CapacitiveImage sample) {
        this.capImage = sample.getCapImg();
        if (capImage != null) {
            byte[] threshCapImage = new byte[capImage.length];
            // thresholding
            Size capSize = CapImage.getCapSize();
            int capw = capSize.getWidth();
            int caph = capSize.getHeight();
            for (int y = 0; y < caph; y++) {
                for (int x = 0; x < capw; x++) {
                    int val = capImage[y * capw + x];
                    int index;
                    if (rightHanded) {
                        index = (capw - x - 1) * caph + y; // dirty fix
                    } else {
                        index = x * caph + (caph - y - 1);
                    }
                    if (val <= 15) {
                        threshCapImage[index] = 0;
                    } else {
                        threshCapImage[index] = 100;
                    }
                }
            }

            Mat img = new Mat(capw, caph, CvType.CV_8S);
            img.put(0, 0, threshCapImage);
            Mat stats = new Mat();
            Mat labels = new Mat();
            int numCC = Imgproc.connectedComponentsWithStats(img, labels, stats, new Mat());

            // find max cc area for no touch = no write
            double maxArea = Double.MIN_VALUE;
            for (int i = 1; i < numCC; i++) {
                double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
                if (area > maxArea) {
                    maxArea = area;
                }
            }

            // find limit cc leftmost x
            double limitX;
            if (rightHanded) {
                double minX = Double.MAX_VALUE;
                for (int i = 1; i < numCC; i++) {
                    double x = stats.get(i, Imgproc.CC_STAT_LEFT)[0];
                    if (x < minX) {
                        minX = x;
                    }
                }
                limitX = minX;
            } else {
                double maxX = Double.MIN_VALUE;
                for (int i = 1; i < numCC; i++) {
                    double x = stats.get(i, Imgproc.CC_STAT_LEFT)[0];
                    if (x > maxX) {
                        maxX = x;
                    }
                }
                limitX = maxX;
            }

            // determine which CCs have area too large or are not smallest CC
            Set<Integer> ccBlacklist = new HashSet<>();
            for (int i = 0; i < numCC; i++) {
                double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
                double x = stats.get(i, Imgproc.CC_STAT_LEFT)[0];
                Log.i(TAG, x+", "+limitX);
                if ((rightHanded && (x < limitX || x < 25)) || (!rightHanded && (x > limitX || x > 25)) || area > 12 || maxArea < 50) {
                    ccBlacklist.add(i);
                }
            }

            // yeet pixels with blacklisted labels
            for (int y = 0; y < caph; y++) {
                for (int x = 0; x < capw; x++) {
                    int label = (int) labels.get(x, y)[0];
                    if (ccBlacklist.contains(label)) {
                        int index;
                        if (rightHanded) {
                            index = y * capw + (capw - x - 1); // dirty fix
                        } else {
                            index = (caph - y - 1) * capw + x;
                        }
                        capImage[index] = 0;

                    }
                }
            }


            // fix because capacitive screen is weird...
            for (int i = 0; i < capImage.length; i++) {
                if (capImage[i] > 10) {
                    capImage[i] = (short) (Math.sqrt(230 * capImage[i]) + 150);
                }
            }

            touchTracker.update(touchDetector.findTouchPoints(capImage), TestCapacitanceView.this);
        }
        postInvalidate();
    }

    private final Paint capPaint = new Paint();

    private final Paint capValTextPaint = new Paint() {{
        setTextSize(8 * getResources().getDisplayMetrics().density);
        setColor(Color.BLACK);
    }};
    private final Paint touchPaint = new Paint() {{
        setColor(Color.RED);
        setStrokeWidth(2);
    }};

    private void drawCapImage(Canvas canvas) {
        if (!showMatrix) {
            return;
        }

        Size screenSize = CapImage.getScreenSize();
        Size capSize = CapImage.getCapSize();
        int capw = capSize.getWidth();
        int caph = capSize.getHeight();
        float pxw = screenSize.getWidth() / (float) capw;
        float pxh = screenSize.getHeight() / (float) caph;
        for (int y = 0; y < caph; y++) {
            for (int x = 0; x < capw; x++) {
                int val = capImage[y * capw + x];
                if (val < 0 & showNegativeValues) {
                    int color = Math.max(0, Math.min(-val * 5, 255));
                    capPaint.setARGB(255, 0, color, color);
                    capValTextPaint.setARGB(255, 0, 0, 0);
                } else if (val > 0) {
                    int color = Math.min(val, 255);
                    capPaint.setARGB(255, color, color, color);
                    int tcolor = (color < 127) ? 180 : 0;
                    capValTextPaint.setARGB(255, tcolor, tcolor, tcolor);
                } else {
                    continue;
                }

                // dirty fix for inverted axes
                if (rightHanded) {
                    canvas.drawRect((caph - y - 1) * pxh, x * pxw, (caph - y) * pxh, (x + 1) * pxw, capPaint);
                } else
                    canvas.drawRect(y * pxh, (capw - x - 1) * pxw, (y + 1) * pxh, (capw - x) * pxw, capPaint);
//                canvas.drawRect(x * pxw, y * pxh, (x + 1) * pxw, (y + 1) * pxh, capPaint);
                if (showText) {
//                    canvas.drawText(Integer.toString(val), x * pxw, (y + 1) * pxh, capValTextPaint);
                    canvas.drawText(Integer.toString(val), (y + 1) * pxh, (capw - x) * pxw, capValTextPaint);
                }
            }
        }
    }

    private void drawTouchPoint(Canvas canvas, MotionEvent.PointerCoords touch) {
        drawTouchPoint(canvas, touch, 10, Color.TRANSPARENT);
    }

    Paint _paintBlur = new Paint();


    /***
     *
     * @param glow Color.TRANSPARENT for no glow effect
     */
    private void drawTouchPoint(Canvas canvas, MotionEvent.PointerCoords touch, double r, int glow) {
        canvas.save();
        // dirty fix for inverted axes
        Size screenSize = CapImage.getScreenSize();
        if (rightHanded)
            canvas.translate(screenSize.getHeight() - touch.y, touch.x);
        else
            canvas.translate(touch.y, screenSize.getWidth() - touch.x);
//        canvas.translate(touch.x, touch.y);
        touchPaint.setStyle(Paint.Style.STROKE);

        if (glow != Color.TRANSPARENT) {
            _paintBlur.setStyle(Paint.Style.FILL);
            _paintBlur.setStrokeWidth(10f);
            _paintBlur.setDither(true);
            _paintBlur.setAntiAlias(true);
            RadialGradient radialGradient = new RadialGradient(0, 0, (float) r * 2, glow, Color.TRANSPARENT, Shader.TileMode.CLAMP);
            _paintBlur.setShader(radialGradient);
            canvas.drawOval((float) -r, (float) -r, (float) r, (float) r, _paintBlur);
        } else {
            canvas.drawOval((float) -r, (float) -r, (float) r, (float) r, touchPaint);
        }

        canvas.restore();
    }


    private final float[] fingerHSV = new float[]{0, 1, 1};

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRGB(0, 0, 0);

        canvas.save();
        canvas.translate(0, -viewOffset * CapImage.getScreenSize().getHeight() / (float) CapImage.getCapSize().getHeight());
        canvas.scale(1f, 1f);
        if (capImage != null) {
            drawCapImage(canvas);
        }


        if (touchMap != null & showTouchPoints) {
            for (Map.Entry<MotionEvent.PointerProperties, MotionEvent.PointerCoords> ent : touchMap.entrySet()) {
                fingerHSV[0] = (ent.getKey().id * 100) % 360; // hue
                touchPaint.setColor(Color.HSVToColor(fingerHSV));
                drawTouchPoint(canvas, ent.getValue());
            }
        }
        canvas.restore();
    }

    public void onResume() {
        capStreamer.start();
    }

    public void onPause() {
        capStreamer.stop();
    }

    public void onVolumeUp() {
        viewOffset++;
        invalidate();
    }

    public void onVolumeDown() {
        viewOffset--;
        invalidate();
    }

    @Override
    public void onNewTouchMap(TouchMap newTouchMap) {
        touchMap = newTouchMap;
    }

}
