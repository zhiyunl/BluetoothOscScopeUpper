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
import android.widget.RadioGroup;
import android.widget.TextView;

public class Setting extends AppCompatActivity {

    ImageView setting_view_back, setting_view_openSound;
    TextView setting_text_openSound;
    Intent intent;
    RadioGroup setting_rGroup_theme;
    int themeFlag;
    Boolean soundFlag;
    SharedPreferences share;
    private MediaPlayer mediaPlayer_btnClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setStyle();
        setContentView(R.layout.activity_setting);

        setting_view_back = (ImageView) findViewById(R.id.setting_view_back);
        setting_view_openSound = (ImageView) findViewById(R.id.setting_view_openSound);
        setting_text_openSound = (TextView) findViewById(R.id.setting_text_openSound);
        setting_rGroup_theme = (RadioGroup) findViewById(R.id.setting_rGroup_theme);
        mediaPlayer_btnClick = new MediaPlayer().create(this,R.raw.click);

        setPre();

        intent = new Intent(Setting.this, MainActivity.class);
        setting_view_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                startActivity(intent);
                finish();
            }
        });

        setting_view_openSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                if (!soundFlag) {
                    setting_view_openSound.setImageResource(R.drawable.u7);
                    setting_text_openSound.setText(R.string.closed);
                    soundFlag = !soundFlag;
                } else {
                    setting_view_openSound.setImageResource(R.drawable.u6);
                    setting_text_openSound.setText(R.string.On);
                    soundFlag = !soundFlag;
                }
                saveFlag();    //保存声音设置，其他界面读取次保存值设置声音
            }
        });
        setting_rGroup_theme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if(!soundFlag) mediaPlayer_btnClick.start();
                themeFlag = group.getCheckedRadioButtonId();
                saveRaidoId();   //保存主题设置
                refresh();       //刷新activity使主题设置生效
            }
        });
    }

    //保存声音设置
    public void saveFlag() {

        share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putBoolean("soundFlag", soundFlag);
        editor.commit();
    }


    //保存主题设置
    public void saveRaidoId() {

        share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = share.edit();
        editor.putInt("themeFlag", themeFlag);
        editor.commit();
    }

    //根据保存值设置初始化界面
    public void setPre() {

        share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        soundFlag = share.getBoolean("soundFlag", false);
        themeFlag = share.getInt("themeFlag", R.id.setting_rBtn_BlueTheme_);

        if (soundFlag) {
            setting_view_openSound.setImageResource(R.drawable.u7);
            setting_text_openSound.setText(R.string.closed);
        } else {
            setting_view_openSound.setImageResource(R.drawable.u6);
            setting_text_openSound.setText(R.string.On);
        }

        setting_rGroup_theme.check(themeFlag);
    }

    //根据保存值设置主题
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

    //刷新当前界面
    public void refresh() {
        Intent i = new Intent(Setting.this, Setting.class);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    //重写系统返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0 ) {
            intent = new Intent(Setting.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


}

