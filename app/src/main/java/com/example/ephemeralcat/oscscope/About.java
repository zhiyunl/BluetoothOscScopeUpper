package com.example.ephemeralcat.oscscope;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

public class About extends AppCompatActivity {

    ImageView about_view_back;
    boolean soundFlag;
    int themeFlag;
    private MediaPlayer mediaPlayer_btnClick;
    SharedPreferences share;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setStyle();
        setContentView(R.layout.acticity_about);
        setPre();

        mediaPlayer_btnClick = new MediaPlayer().create(this,R.raw.click);
        about_view_back=(ImageView) findViewById(R.id.about_view_back);
        final Intent intent;
        intent = new Intent(About.this,MainActivity.class);
        about_view_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                startActivity(intent);
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

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0 ) {
            Intent intent1 = new Intent(About.this, MainActivity.class);
            startActivity(intent1);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
