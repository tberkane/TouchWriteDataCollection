package com.example.datacollection;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;


public class DataCollectionActivity extends AppCompatActivity {

    CapCanvasView myCanvasView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myCanvasView = new CapCanvasView(this);
        myCanvasView.setSystemUiVisibility(SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.activity_data_collection);

        ConstraintLayout myLayout = findViewById(R.id.constraintLayout);

        myCanvasView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT));

        myLayout.addView(myCanvasView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myCanvasView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myCanvasView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}