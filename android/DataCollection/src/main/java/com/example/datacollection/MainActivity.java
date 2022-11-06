package com.example.datacollection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button startButton;
    private EditText participantIdEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        startButton = findViewById(R.id.startButton);
//        startButton.setEnabled(false); //TODO

        participantIdEditText = findViewById(R.id.participantIdEditText);
        participantIdEditText.addTextChangedListener(new TextWatcher() { // only enable start button if ID has been set

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                startButton.setEnabled(s.toString().trim().length() != 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    public void startDataCollection(View view) {
        Intent intent = new Intent(this, DataCollectionActivity.class);
        intent.putExtra("participantId", participantIdEditText.getText().toString());
        startActivity(intent);
    }

    public void testCapacitance(View view) {
        Intent intent = new Intent(this, TestCapacitanceActivity.class);
        startActivity(intent);
    }

}