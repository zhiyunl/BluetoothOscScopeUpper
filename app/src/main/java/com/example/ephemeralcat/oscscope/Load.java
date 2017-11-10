package com.example.ephemeralcat.oscscope;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;


public class Load extends AppCompatActivity {

    private static final int LOAD_DISPLAY_TIME = 1500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setStyle();
        setContentView(R.layout.activity_load);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                //Go to main activity, and finish load activity
                Intent mainIntent = new Intent(Load.this, MainActivity.class);
                Load.this.startActivity(mainIntent);
                Load.this.finish();
            }
        }, LOAD_DISPLAY_TIME);
    }

    public void setStyle() {

        SharedPreferences share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        int themeFlag = share.getInt("themeFlag",R.id.setting_rBtn_BlueTheme_);
        if(themeFlag == R.id.setting_rBtn_BlueTheme_)
            setTheme(R.style.BlueTheme);
        else if(themeFlag == R.id.setting_rBtn_RedTheme_)
            setTheme(R.style.RedTheme);
        else
            setTheme(R.style.NightTheme);
    }

    }



