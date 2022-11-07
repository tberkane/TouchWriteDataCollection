package com.example.datacollection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button startButton;
    private EditText participantIdEditText, editTextNumSamples;
    private Switch handSwitch;
    private CheckBox checkBoxSmall, checkBoxMedium, checkBoxLarge, checkBoxFreeHand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        handSwitch = findViewById(R.id.switch1);

        editTextNumSamples = findViewById(R.id.editTextNumSamples);
        checkBoxSmall = findViewById(R.id.checkBoxSmall);
        checkBoxMedium = findViewById(R.id.checkBoxMedium);
        checkBoxLarge = findViewById(R.id.checkBoxLarge);
        checkBoxFreeHand = findViewById(R.id.checkBoxFreeHand);

        startButton = findViewById(R.id.startButton);
        startButton.setEnabled(false);

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
        intent.putExtra("rightHanded", handSwitch.isChecked());
        intent.putExtra("numSamples", Integer.valueOf(editTextNumSamples.getText().toString()));
        intent.putExtra("smallDigits", checkBoxSmall.isChecked());
        intent.putExtra("mediumDigits", checkBoxMedium.isChecked());
        intent.putExtra("largeDigits", checkBoxLarge.isChecked());
        intent.putExtra("freeHand", checkBoxFreeHand.isChecked());
        startActivity(intent);
    }

    public void testCapacitance(View view) {
        Intent intent = new Intent(this, TestCapacitanceActivity.class);
        intent.putExtra("rightHanded", handSwitch.isChecked());
        startActivity(intent);
    }

}