package com.example.datacollection;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.capimage.CapImage;
import com.example.capimage.CapImageStreamer;
import com.example.capimage.CapacitiveImage;
import com.example.capimage.TouchDetector;
import com.example.capimage.TouchTracker;
import com.google.gson.Gson;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataCollectionView extends View implements TouchTracker.TouchMapCallback, CapImageStreamer.CapImageCallback {
    static {
        OpenCVLoader.initDebug();
    }

    private static final String TAG = DataCollectionView.class.getSimpleName();
    private final File root;

    private List<String> letters;

    private short[] capImage;

    private CapImageStreamer capStreamer = new CapImageStreamer(this);
    private TouchDetector touchDetector = new TouchDetector();
    private TouchTracker touchTracker = new TouchTracker();
    private TouchTracker.TouchMap prevTouchMap;
    private TouchTracker.TouchMap touchMap;

    private float motionTouchEventX;
    private float motionTouchEventY;
    private Path path;
    private float currentX;
    private float currentY;
    private final int touchTolerance;
    private Canvas extraCanvas;
    private Bitmap extraBitmap;
    private final Paint paint;
    private final int drawColor;
    private final int backgroundColor;

    private Paint letterPaint;
    private float xPos;
    private float yPos;
    private String letter;

    private Map<String, Map<Long, Map<String, short[]>>> capImages;

    private boolean recordCapImages;
    private Gson gson;


    public DataCollectionView(Context context) {
        super(context);

        root = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "CapacitanceData");
        Log.i(TAG, "Save Path: " + root);
        if (!root.exists()) {
            if (!root.mkdirs()) {
                Log.e(TAG, "Error: Folder not created");
            }
        }


        this.letters = new ArrayList<>(Arrays.asList("A", "B", "C"));
        Collections.shuffle(this.letters);

        this.path = new Path();
        this.touchTolerance = ViewConfiguration.get(context).getScaledTouchSlop();
        this.paint = new Paint();
        this.drawColor = ResourcesCompat.getColor(this.getResources(), R.color.black, null);
        this.backgroundColor = ResourcesCompat.getColor(this.getResources(), R.color.white, null);
        this.paint.setColor(this.drawColor);
        this.paint.setAntiAlias(true);
        this.paint.setDither(true);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeWidth(12.0F);

        this.letterPaint = new Paint();
        this.letterPaint.setTextAlign(Paint.Align.CENTER);
        this.letterPaint.setTextSize(1000F);//sizes: 1000, normal size, in between, as big as possible
        this.letterPaint.setColor(ContextCompat.getColor(this.getContext(), com.google.android.material.R.color.material_grey_100));

        this.letter = letters.remove(letters.size() - 1);

        this.capImages = new HashMap<>();
        this.capImages.put(letter, new HashMap<>());

        this.recordCapImages = true;
        gson = new Gson();

    }

    @Override
    public void onCapImage(final CapacitiveImage sample) {
        this.capImage = sample.getCapImg();
        if (capImage != null && recordCapImages) {

            capImages.get(letter).put(sample.getTimeStamp(), Map.of(
                    "CAP_IMG", capImage,
                    "PEN_POS", new short[]{(short) motionTouchEventX, (short) motionTouchEventY}
            ));

            ////TODO optimize this fct (see handik for secondary threads)

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

            // find max cc area for no touch = no write
            double maxArea = Double.MIN_VALUE;
            for (int i = 1; i < numCC; i++) {
                double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
                if (area > maxArea) {
                    maxArea = area;
                }
            }

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
                if (x > minX || x > 25 || area > 12 || maxArea < 50) {
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
                    capImage[i] = (short) (Math.sqrt(200 * capImage[i]) + 50);
                }
            }

            touchTracker.update(touchDetector.findTouchPoints(capImage), DataCollectionView.this);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (touchMap != null && !touchMap.isEmpty()) {
            // right handed user
            float stylusX = Float.MAX_VALUE;
            float stylusY = Float.MAX_VALUE;
            for (Map.Entry<MotionEvent.PointerProperties, MotionEvent.PointerCoords> ent : touchMap.entrySet()) {
                MotionEvent.PointerCoords touchCoords = ent.getValue();
                // for right handed user, stylus position is position of leftmost screen touch
                if (touchCoords.y < stylusY) {
                    stylusX = touchCoords.x;
                    stylusY = touchCoords.y;
                }
            }
            // dirty fix for inverted axes
//                Log.i(TAG, stylusX + "," + stylusY);
            Size screenSize = CapImage.getScreenSize();
            this.motionTouchEventX = stylusY;
            this.motionTouchEventY = screenSize.getWidth() - stylusX;

            if (prevTouchMap == null || prevTouchMap.isEmpty()) {
                this.touchStart();
            } else {
                this.touchMove();
            }
        } else {
            this.touchUp();
        }

        canvas.drawBitmap(extraBitmap, 0f, 0f, null);
    }

    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (this.extraBitmap != null) {
            this.extraBitmap.recycle();
        }
        this.extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.extraCanvas = new Canvas(extraBitmap);
        this.extraCanvas.drawColor(backgroundColor);

        this.xPos = width / 3f;
        this.yPos = (height / 2f - (letterPaint.descent() + letterPaint.ascent()) / 2f);
        extraCanvas.drawText(letter, xPos, yPos, letterPaint);
    }

    private void touchStart() {
        this.path.reset();
        this.path.moveTo(this.motionTouchEventX, this.motionTouchEventY);
        this.currentX = this.motionTouchEventX;
        this.currentY = this.motionTouchEventY;
    }

    private void touchMove() {
        float dx = Math.abs(this.motionTouchEventX - this.currentX);
        float dy = Math.abs(this.motionTouchEventY - this.currentY);
        if (dx >= this.touchTolerance || dy >= this.touchTolerance) {
            this.path.quadTo(this.currentX, this.currentY, (this.motionTouchEventX + this.currentX) / 2f, (this.motionTouchEventY + this.currentY) / 2f);
            this.currentX = this.motionTouchEventX;
            this.currentY = this.motionTouchEventY;
            this.extraCanvas.drawPath(this.path, this.paint);
        }

        this.invalidate();
    }

    private void touchUp() {
        this.path.reset();
    }


    @Override
    public void onNewTouchMap(TouchTracker.TouchMap newTouchMap) {
        prevTouchMap = touchMap;
        touchMap = newTouchMap;
    }

    public void onResume() {
        capStreamer.start();
    }

    public void onPause() {
        capStreamer.stop();
    }

    public void startNextLetter(AppCompatActivity a) {
//        Map<Long, short[]> capImagesForLetterCopy = capImagesForLetter.entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.copyOf(e.getValue(), e.getValue().length)));
        recordCapImages = true;
        if (letters.isEmpty()) {
            recordCapImages = false;
            String json = gson.toJson(capImages);


            String filename = "recording_id" + 1 + ".json";
            File file = new File(root, filename);
            FileWriter writer = null;
            try {
                writer = new FileWriter(file, false);
                writer.write(json);
            } catch (IOException e) {
                Log.e(TAG, "Error:" + e);
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            Intent intent = new Intent(a, EndActivity.class);
            startActivity(a, intent, null);
        } else {
            this.letter = letters.remove(letters.size() - 1);
            this.capImages.put(letter, new HashMap<>());
            this.extraCanvas.drawColor(backgroundColor);
            extraCanvas.drawText(letter, xPos, yPos, letterPaint);
        }
    }
}

