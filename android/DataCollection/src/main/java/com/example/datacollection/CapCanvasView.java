package com.example.datacollection;


import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.example.capimage.CapImageStreamer;
import com.example.capimage.CapacitiveImage;
import com.example.capimage.TouchDetector;
import com.example.capimage.TouchTracker;

import java.util.Map;

public class CapCanvasView extends MyCanvasView implements TouchTracker.TouchMapCallback, CapImageStreamer.CapImageCallback {
    private static final String TAG = CapCanvasView.class.getSimpleName();

    private short[] capImage;

    private CapImageStreamer capStreamer = new CapImageStreamer(this);
    private TouchDetector touchDetector = new TouchDetector();
    private TouchTracker touchTracker = new TouchTracker();
    private TouchTracker.TouchMap touchMap;

    public CapCanvasView(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onCapImage(final CapacitiveImage sample) {
        this.capImage = sample.getCapImg();
        if (capImage != null) {
            touchTracker.update(touchDetector.findTouchPoints(capImage), CapCanvasView.this);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i(TAG, "a");
        super.onDraw(canvas);
        if (touchMap != null && !touchMap.isEmpty()) {
            Log.i(TAG, "b");
            // right handed user
            float stylusX = Float.MAX_VALUE;
            float stylusY = Float.MAX_VALUE;
            for (Map.Entry<MotionEvent.PointerProperties, MotionEvent.PointerCoords> ent : touchMap.entrySet()) {
                MotionEvent.PointerCoords touchCoords=ent.getValue();
                // for right handed user, stylus position is position of leftmost screen touch
                if (touchCoords.y < stylusY) {
                    stylusX=touchCoords.x;
                    stylusY=touchCoords.y;
                }
            }
            if (stylusY<1350) { // simple heuristic for now
                Log.i(TAG, "c");
                Log.i(TAG, stylusX + "," + stylusY);
                setMotionTouchEventX(stylusX);
                setMotionTouchEventY(stylusY);
            }
        }
        //TODO... dispatch to 3 other fcts
    }

    public void onResume() {
        capStreamer.start();
    }

    public void onPause() {
        capStreamer.stop();
    }


    @Override
    public void onNewTouchMap(TouchTracker.TouchMap newTouchMap) {
        touchMap = newTouchMap;
    }
}
