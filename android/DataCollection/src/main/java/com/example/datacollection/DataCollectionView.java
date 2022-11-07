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
import android.widget.TextView;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * View for collecting capacitance data from the user
 */
public class DataCollectionView extends View implements TouchTracker.TouchMapCallback, CapImageStreamer.CapImageCallback {
    static {
        OpenCVLoader.initDebug();
    }

    private static final String TAG = DataCollectionView.class.getSimpleName();

    // cap data and touches
    private final CapImageStreamer capStreamer = new CapImageStreamer(this);
    private final TouchDetector touchDetector = new TouchDetector();
    private final TouchTracker touchTracker = new TouchTracker();
    private TouchTracker.TouchMap prevTouchMap;
    private TouchTracker.TouchMap touchMap;

    // cap image recording
    private boolean recordCapImages;
    private final Map<String, Map<Long, Map<String, short[]>>> recordedCapImages;
    private final Gson gson;
    private final File saveDirectory;

    // letters
    private final List<String> lettersToWrite;
    private String currentLetterToWrite;

    // drawing
    private Canvas extraCanvas;
    private Bitmap extraBitmap;
    private final int backgroundColor;
    private final Path path;
    private final Paint paint;
    private final Paint letterPaint;
    private float letterXPos;
    private float letterYPos;

    // touch positions
    private float motionTouchEventX;
    private float motionTouchEventY;
    private float currentX;
    private float currentY;
    private final int touchTolerance;
    private final boolean rightHanded;
    private final TextView tvInstruction;

    public DataCollectionView(Context context, boolean rightHanded, List<String> lettersToWrite, TextView tvInstruction) {
        super(context);

        this.tvInstruction = tvInstruction;
        this.rightHanded = rightHanded;
        this.lettersToWrite = lettersToWrite;
        this.currentLetterToWrite = lettersToWrite.remove(lettersToWrite.size() - 1);

        this.recordCapImages = true;
        this.recordedCapImages = new HashMap<>();
        this.recordedCapImages.put(currentLetterToWrite, new HashMap<>());
        this.gson = new Gson();
        // initialize directory to save cap data and create it if needed
        this.saveDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "CapacitanceData");
        Log.i(TAG, "Save Path: " + saveDirectory);
        if (!saveDirectory.exists()) {
            if (!saveDirectory.mkdirs()) {
                Log.e(TAG, "Error: Folder not created");
            }
        }

        this.touchTolerance = ViewConfiguration.get(context).getScaledTouchSlop();

        this.path = new Path();
        this.backgroundColor = ResourcesCompat.getColor(this.getResources(), R.color.white, null);

        this.paint = new Paint();
        int drawColor = ResourcesCompat.getColor(this.getResources(), R.color.black, null);
        this.paint.setColor(drawColor);
        this.paint.setAntiAlias(true);
        this.paint.setDither(true);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeWidth(12.0F);

        this.letterPaint = new Paint();
        this.letterPaint.setTextAlign(Paint.Align.CENTER);
        this.letterPaint.setTextSize(1000F);
        this.letterPaint.setColor(ContextCompat.getColor(this.getContext(), com.google.android.material.R.color.material_grey_100));
    }

    @Override
    public void onCapImage(final CapacitiveImage sample) {
        short[] capImage = sample.getCapImg();
        if (capImage != null && recordCapImages) {
            recordedCapImages.get(currentLetterToWrite).put(sample.getTimeStamp(), Map.of("CAP_IMG", Arrays.copyOf(capImage, capImage.length), "PEN_POS", new short[]{(short) motionTouchEventX, (short) motionTouchEventY}));

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
//            for (int i = 0; i < capImage.length; i++) {
//                if (capImage[i] > 10) {
//                    capImage[i] = (short) (Math.sqrt(230 * capImage[i]) + 150);
//                }
//            }

            touchTracker.update(touchDetector.findTouchPoints(capImage), DataCollectionView.this);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (touchMap != null && !touchMap.isEmpty()) {
            if (rightHanded) {
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
                Size screenSize = CapImage.getScreenSize();
                this.motionTouchEventX = screenSize.getHeight() - stylusY;
                this.motionTouchEventY = stylusX;
            } else {
                float stylusX = Float.MIN_VALUE;
                float stylusY = Float.MIN_VALUE;
                for (Map.Entry<MotionEvent.PointerProperties, MotionEvent.PointerCoords> ent : touchMap.entrySet()) {
                    MotionEvent.PointerCoords touchCoords = ent.getValue();
                    // for left handed user, stylus position is position of rightmost screen touch
                    if (touchCoords.y > stylusY) {
                        stylusX = touchCoords.x;
                        stylusY = touchCoords.y;
                    }
                }
                // dirty fix for inverted axes
                Size screenSize = CapImage.getScreenSize();
                this.motionTouchEventX = stylusY;
                this.motionTouchEventY = screenSize.getWidth() - stylusX;
            }

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
        if (rightHanded)
            this.letterXPos = width / 3f;
        else
            this.letterXPos = 2f * width / 3f;
        this.letterYPos = (height / 2f - (letterPaint.descent() + letterPaint.ascent()) / 2f);
        resetCanvas();
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

    // clear canvas and draw new outline
    private void resetCanvas() {
        Log.i(TAG, currentLetterToWrite);
        this.extraCanvas.drawColor(backgroundColor);
        switch (currentLetterToWrite.charAt(1)) {
            case 's':
                this.letterPaint.setTextSize(200F);
                break;
            case 'm':
                this.letterPaint.setTextSize(500F);
                break;
            case 'l':
                this.letterPaint.setTextSize(1000F);
                break;
        }
        if (currentLetterToWrite.charAt(2) == 'g') {
            tvInstruction.setText("Write a " + currentLetterToWrite.charAt(0) + " following the outline below.");
            extraCanvas.drawText(String.valueOf(currentLetterToWrite.charAt(0)), letterXPos, letterYPos, letterPaint);
        } else {
            tvInstruction.setText("Write a " + currentLetterToWrite.charAt(0) + " in the box below.");
            extraCanvas.drawRect(letterXPos - letterPaint.getTextSize() / 4f, letterYPos, letterXPos + letterPaint.getTextSize() / 4f, letterYPos - letterPaint.getTextSize() / 1.4f, letterPaint);
        }
    }

    public void startNextLetter(AppCompatActivity a, String participantId) {
        if (lettersToWrite.isEmpty()) {
            recordCapImages = false;

            // convert data to json and save to fs
            String jsonData = gson.toJson(recordedCapImages);
            String filename = "recording_id" + participantId + "_" + (rightHanded ? "right" : "left") + "Handed.json";
            File file = new File(saveDirectory, filename);
            FileWriter writer = null;
            try {
                writer = new FileWriter(file, false);
                writer.write(jsonData);
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
            // get next letter
            this.currentLetterToWrite = lettersToWrite.remove(lettersToWrite.size() - 1);
            this.recordedCapImages.put(currentLetterToWrite, new HashMap<>());
            resetCanvas();
        }
    }

    public void resetLetter() {
        resetCanvas();
        this.recordedCapImages.get(currentLetterToWrite).clear();
    }
}

