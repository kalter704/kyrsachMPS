package com.aleksandr.nikitin.testcolorpicker;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements ColorPicker.OnColorChangedListener, ColorPicker.OnColorSelectedListener{

    private ColorPicker picker;
    private SVBar svBar;
    private Button btnSetColor;
    private RelativeLayout viewForPaint;

    private TextView tvRed;
    private TextView tvGreen;
    private TextView tvBlue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvRed = (TextView) findViewById(R.id.tvRed);
        tvGreen = (TextView) findViewById(R.id.tvGreen);
        tvBlue = (TextView) findViewById(R.id.tvBlue);

        viewForPaint = (RelativeLayout) findViewById(R.id.viewForPaint);
        btnSetColor = (Button) findViewById(R.id.btnSetColor);
        picker = (ColorPicker) findViewById(R.id.picker);
        svBar = (SVBar) findViewById(R.id.svbar);

        picker.addSVBar(svBar);

        setColorToView();

        //To set the old selected color u can do it like this
        picker.setOldCenterColor(picker.getColor());
        // adds listener to the colorpicker which is implemented
        //in the activity
        picker.setOnColorChangedListener(this);
        picker.setOnColorSelectedListener(this);

        //to turn of showing the old color
        picker.setShowOldCenterColor(false);

        btnSetColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setColorToView();
            }
        });

    }

    @Override
    public void onColorChanged(int color) {
        //Toast.makeText(this, "Changed " + String.valueOf(color), Toast.LENGTH_SHORT).show();
        setColorToView();
    }

    @Override
    public void onColorSelected(int color) {
        //Toast.makeText(this, "Selected " + String.valueOf(color), Toast.LENGTH_SHORT).show();
    }

    private void setColorToView() {
        int color = picker.getColor();
        viewForPaint.setBackgroundColor(color);

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        tvRed.setText("Red: " + String.valueOf(red));
        tvGreen.setText("Green: " + String.valueOf(green));
        tvBlue.setText("Blue: " + String.valueOf(blue));
    }

}
