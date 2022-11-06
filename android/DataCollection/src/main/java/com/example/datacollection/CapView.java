package com.example.datacollection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
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
import com.google.gson.Gson;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CapView extends View implements TouchMapCallback, CapImageStreamer.CapImageCallback {
    static {
        OpenCVLoader.initDebug();
    }

    private static final String TAG = CapView.class.getSimpleName();


    // --------------------------------------------------------
    // --------------------------------------------------------
    //                        OPTIONS
    // --------------------------------------------------------
    // --------------------------------------------------------
    private boolean showInformation = true;
    private boolean showText = false;
    private boolean showMatrix = true;
    private boolean showNegativeValues = false;
    private boolean showTouchPoints = true;
    private int viewOffset = 0;


    private Gson gson = new Gson();
    private FileWriter writer = null;

    private short[] capImage;


    private CapImageStreamer capStreamer = new CapImageStreamer(this);
    private TouchDetector touchDetector = new TouchDetector();
    private TouchTracker touchTracker = new TouchTracker();
    private TouchMap touchMap;

    private double capPixelSizeInMm;

    private long time;

    boolean drawText = false;

    // Scanner, Coin, Dungeon, Card

    public CapView(Context context) {
        super(context);
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "CapacitiveMatrices");
        Log.i(TAG, "Save Path: " + root.toString());
        if (!root.exists()) {
            if (!root.mkdirs()) {
                Log.e(TAG, "Error #001: Folder not created");
            }
        }

        String filename = "recording_" + System.currentTimeMillis() + "-HandIK.json";
        File file = new File(root, filename);
        try {
            writer = new FileWriter(file, true);
        } catch (IOException e) {
            Log.e(TAG, "Error #007:" + e.toString());
        }


        if (Build.PRODUCT.equals("gts210vewifixx")) {
            capPixelSizeInMm = 4.022;
        } else {
            throw new UnsupportedOperationException("Your device is not supported. Build.PRODUCT:" + Build.PRODUCT + " Build.ID:" + Build.ID);
        }


    }

    public CapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onCapImage(final CapacitiveImage sample) {
        this.capImage = sample.getCapImg();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = gson.toJson(sample);
                saveToFile(json);
                // Run whatever background code you want here.
            }
        }).start();
        if (capImage != null) {

            byte[] threshCapImage = new byte[capImage.length];
            // thresholding
            Size capSize = CapImage.getCapSize();
            int capw = capSize.getWidth();
            int caph = capSize.getHeight();
            for (int y = 0; y < caph; y++) {
                for (int x = 0; x < capw; x++) {
                    int val = capImage[y * capw + x];
                    int index = (capw - x - 1) * caph + y; // dirty fix
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

            // find min cc area
//            double minArea = Double.MAX_VALUE;
//            for (int i = 0; i < numCC; i++) {
//                double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
//                if (area < minArea) {
//                    minArea = area;
//                }
//            }

            // find min cc leftmost x
            double minX = Double.MAX_VALUE;
            for (int i = 1; i < numCC; i++) {
                double x = stats.get(i, Imgproc.CC_STAT_LEFT)[0];
                if (x < minX) {
                    minX = x;
                }
            }

            // determine which CCs have area too large or are not smallest CC
            Set<Integer> ccBlacklist = new HashSet<>();
            for (int i = 0; i < numCC; i++) {
                double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
                double x = stats.get(i, Imgproc.CC_STAT_LEFT)[0];
                if (x > minX || area > 12) {
                    ccBlacklist.add(i);
                }
            }

            // yeet pixels with blacklisted labels
            for (int y = 0; y < caph; y++) {
                for (int x = 0; x < capw; x++) {
                    int label = (int) labels.get(x, y)[0];
                    if (ccBlacklist.contains(label)) {
                        capImage[y * capw + (capw - x - 1)] = 0;
                    }
                }
            }

            // fix because capacitive screen is weird...
            for (int i = 0; i < capImage.length; i++) {
                if (capImage[i] > 10) {
                    capImage[i] = (short) (Math.sqrt(200 * capImage[i])+50);
                }
            }


            touchTracker.update(touchDetector.findTouchPoints(capImage), CapView.this);
        }
        postInvalidate();
    }

    private Paint capPaint = new Paint();

    private Paint capValTextPaint = new Paint() {{
        setTextSize(8 * getResources().getDisplayMetrics().density);
        setColor(Color.BLACK);
    }};
    private Paint statusTextPaint = new Paint() {{
        setTextSize(12 * getResources().getDisplayMetrics().density);
        setColor(Color.WHITE);
    }};
    private Paint touchPaint = new Paint() {{
        setColor(Color.RED);
        setStrokeWidth(2);
    }};

    private Paint buttonPaint = new Paint() {{
        setColor(Color.GRAY);
        setStyle(Style.FILL);
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
                    int color = Math.max(0, Math.min(val, 255));
                    capPaint.setARGB(255, color, color, color);
                    int tcolor = (color < 127) ? 180 : 0;
                    capValTextPaint.setARGB(255, tcolor, tcolor, tcolor);
                } else {
                    continue;
                }

                // dirty fix for inverted axes
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
     * @param canvas
     * @param touch
     * @param r
     * @param glow Color.TRANSPARENT for no glow effect
     */
    private void drawTouchPoint(Canvas canvas, MotionEvent.PointerCoords touch, double r, int glow) {
        canvas.save();

        //_paintBlur.set(_paintSimple);
        //_paintBlur.setColor(Color.argb(235, 255, 0, 0));

        //_paintSimple.setAntiAlias(true);
        //_paintSimple.setDither(true);
        //_paintSimple.setColor(Color.argb(248, 255, 255, 255));
        //_paintSimple.setStrokeWidth(10f);
        //_paintSimple.setStyle(Paint.Style.STROKE);
        //_paintSimple.setStrokeJoin(Paint.Join.ROUND);
        //_paintSimple.setStrokeCap(Paint.Cap.ROUND);

        // dirty fix for inverted axes
        Size screenSize = CapImage.getScreenSize();
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

        if (false) {
            if (touch.pressure > 0) {
                float pr = touch.pressure * 20;
                canvas.drawOval(-pr, -pr, pr, pr, touchPaint);
            }
            /* TODO: orientation, distance */
            if (touch.touchMajor > 0 && touch.touchMinor > 0) {
                float rx = touch.touchMajor * 20;
                float ry = touch.touchMinor * 20;
                canvas.drawOval(-rx, -ry, rx, ry, touchPaint);
            }
            canvas.rotate(touch.orientation);
        }
        canvas.restore();
    }


    private float[] fingerHSV = new float[]{0, 1, 1};

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

        float statusTextHeight = statusTextPaint.getTextSize();
        int statusTextY = 1;


    }

    private double distance(double x1, double x2, double y1, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
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


    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error #004: " + e.toString());
        }
    }

    private void saveToFile(String data) {
        if (data.length() == 0)
            return;
        try {
            writer.append(data + "\n");
        } catch (Exception e) {
            //Log.e(TAG," Error #002: " + e.toString());
        }
    }
}
