package com.example.datacollection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Activity for collecting capacitance data from the user
 */
public class DataCollectionActivity extends AppCompatActivity {
    private static final String TAG = DataCollectionActivity.class.getSimpleName();


    private DataCollectionView view;
    private String participantId;
    private boolean rightHanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        this.participantId = intent.getStringExtra("participantId");
        this.rightHanded = intent.getBooleanExtra("rightHanded", true);

        int numSamples = intent.getIntExtra("numSamples", 5);
        boolean smallDigits = intent.getBooleanExtra("smallDigits", true);
        boolean mediumDigits = intent.getBooleanExtra("mediumDigits", true);
        boolean largeDigits = intent.getBooleanExtra("largeDigits", true);
        boolean freeHand = intent.getBooleanExtra("freeHand", true);

        List<String> digits = new ArrayList<>(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));

        List<String> sizeDigits = new ArrayList<>();
        if (smallDigits)
            for (String d : digits)
                sizeDigits.add(d + "s");
        if (mediumDigits)
            for (String d : digits)
                sizeDigits.add(d + "m");
        if (largeDigits)
            for (String d : digits)
                sizeDigits.add(d + "l");


        List<String> lettersToWrite = new ArrayList<>();
        for (String d : sizeDigits) {
            lettersToWrite.add(d + "g");
        }
        List<String> lettersToWrite2 = new ArrayList<>();
        for (String d : lettersToWrite) {
            for (int i = 0; i < numSamples; i++) {
                lettersToWrite2.add(d + i);
            }
        }
        Collections.shuffle(lettersToWrite2);

        if (freeHand) {
            List<String> freeHandDigits = new ArrayList<>();
            for (String d : sizeDigits) {
                freeHandDigits.add(d + "f");
            }
            List<String> freeHandDigits2 = new ArrayList<>();
            for (String d : freeHandDigits) {
                for (int i = 0; i < numSamples; i++) {
                    freeHandDigits2.add(d + i);
                }
            }
            Collections.shuffle(freeHandDigits2);
            lettersToWrite2.addAll(freeHandDigits2);
        }

        Log.i(TAG, lettersToWrite2.toString());
        if (rightHanded)
            setContentView(R.layout.activity_data_collection_right_handed);
        else
            setContentView(R.layout.activity_data_collection_left_handed);

        TextView tvInstruction = findViewById(R.id.textViewInstruction);

        view = new DataCollectionView(this, rightHanded, lettersToWrite2, tvInstruction);

//        ReactiveButton nextButton = findViewById(R.id.nextButton);
//        nextButton.setOnClickListener(this::startNextLetter);
//
//        ReactiveButton resetButton = findViewById(R.id.resetButton);
//        resetButton.setOnClickListener(this::resetLetter);

        ConstraintLayout myLayout = findViewById(R.id.constraintLayout);
        ConstraintSet set = new ConstraintSet();
        view.setId(View.generateViewId());
        myLayout.addView(view, 0);
        set.clone(myLayout);
        set.connect(view.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        set.connect(view.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        set.connect(view.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        set.connect(view.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        set.constrainHeight(view.getId(), ConstraintLayout.LayoutParams.MATCH_PARENT);
        set.constrainWidth(view.getId(), ConstraintLayout.LayoutParams.MATCH_PARENT);
        set.applyTo(myLayout);

        isStoragePermissionGranted();

    }

    /* Enable immersive full-screen mode */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /* Handle the volume buttons since the touchscreen might not work */
    boolean isVolUp, isVolDown;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        Log.i(TAG, String.valueOf(keyCode));
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    isVolUp = true;
                    if (isVolDown)
                        finish();
                    else
                        resetLetter(null);
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolUp = false;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    isVolDown = true;
                    if (isVolUp)
                        finish();
                    else
                        startNextLetter(null);
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolDown = false;
                }
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Tell the view to go to the next letter
     */
    public void startNextLetter(View view) {
        this.view.startNextLetter(this, participantId);
    }

    /**
     * Restart the current letter from scratch
     */
    public void resetLetter(View view) {
        this.view.resetLetter();
    }

}