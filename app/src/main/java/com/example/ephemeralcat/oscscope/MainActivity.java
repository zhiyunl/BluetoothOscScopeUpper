package com.example.ephemeralcat.oscscope;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {


    Button main_btn_scope,main_btn_set,main_btn_about;
    private MediaPlayer mediaPlayer_btnClick;
    Boolean soundFlag;
    int themeFlag;
    SharedPreferences share;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle();
        setContentView(R.layout.activity_main);
        setPre();

        main_btn_scope = (Button) findViewById(R.id.main_btn_scope);
        main_btn_set = (Button) findViewById(R.id.main_btn_set);
        main_btn_about = (Button) findViewById(R.id.main_btn_about);

        final Intent intent1, intent2, intent3;
        intent1 = new Intent(MainActivity.this, Scope.class);
        intent2 = new Intent(MainActivity.this, Setting.class);
        intent3 = new Intent(MainActivity.this, About.class);

        mediaPlayer_btnClick = new MediaPlayer().create(this,R.raw.click);

        main_btn_scope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                startActivity(intent1);
                finish();
            }
        });

        main_btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                startActivity(intent2);
                finish();
            }
        });

        main_btn_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                startActivity(intent3);
                finish();
            }
        });
    }

    public void setStyle() {

        share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        themeFlag = share.getInt("themeFlag",R.id.setting_rBtn_BlueTheme_);
        if(themeFlag == R.id.setting_rBtn_BlueTheme_)
            setTheme(R.style.BlueTheme);
        else if(themeFlag == R.id.setting_rBtn_RedTheme_)
            setTheme(R.style.RedTheme);
        else
            setTheme(R.style.NightTheme);
    }
    public void setPre() {
        share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        soundFlag = share.getBoolean("soundFlag", false);
    }
}
