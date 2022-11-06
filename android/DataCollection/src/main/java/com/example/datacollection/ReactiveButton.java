package com.example.datacollection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReactiveButton extends androidx.appcompat.widget.AppCompatButton {


    public ReactiveButton(@NonNull Context context) {
        super(context);
    }

    public ReactiveButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ReactiveButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) setPressed(true);
        return super.onTouchEvent(event);
    }
}
