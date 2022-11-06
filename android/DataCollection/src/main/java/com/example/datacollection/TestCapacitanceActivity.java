package com.example.datacollection;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class TestCapacitanceActivity extends AppCompatActivity {
    private TestCapacitanceView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new TestCapacitanceView(this);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
    }

    /* Enable immersive full-screen mode */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /* Handle the volume buttons since the touchscreen might not work */ boolean isVolUp, isVolDown;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    isVolUp = true;
                    if (isVolDown) finish();
                    else view.onVolumeUp();
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolUp = false;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    isVolDown = true;
                    if (isVolUp) finish();
                    else view.onVolumeDown();
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolDown = false;
                }
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}