package com.example.ephemeralcat.oscscope;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.lang.Math.*;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import java.math.*;
import java.util.Date;
import java.util.Locale;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Scope extends AppCompatActivity {
    ///
    ImageView scope_view_back;
    Boolean soundFlag;
    int themeFlag;
    SharedPreferences share;
    private MediaPlayer mediaPlayer_btnClick;


    //////////////Bluetooth
    // Debugging
    private static final String TAG = "BTScope";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_MEASURE=6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBTService = null;

    //////////// Scope
    public int hstep=1;
    public int vstep=1;
    boolean set=false;
    private int trig_valInt;
    private double trig_val;
    TextView trig,Vp_max,Vp_min;
    SurfaceView scope_view;
    LinearLayout scope_father,scope_grand,scope_grandG;
    SurfaceHolder scope_holder;
    Button trig_up, trig_dn, prt_sc, btn_run, btn_stop;
    Spinner spin_A,spin_T,spin_O,spin_C;
    /////data
    private boolean online=false;
    private static final int MAX_SAMPLES = 1024;
    byte waveByte[][]=new byte[MAX_SAMPLES][5];
    int waveData[] = new int[MAX_SAMPLES];
    String waveString[] =new String[MAX_SAMPLES];
    int dataIndex=0,byteIndex=0,strIndex=0,v_cnt=0;
    private double max_val=0,min_val=0;
    private int max_valInt=0,min_valInt=0;
    /////power
    protected PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        //setContentView(R.layout.acitivity_scpoe);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        // Set up the custom title
        //mTitle = (TextView) findViewById(R.id.title_left_text);
        //mTitle.setText(R.string.app_name);
        //mTitle = (TextView) findViewById(R.id.title_right_text);
        setStyle();
        setContentView(R.layout.acitivity_scpoe);
        setPre();
        createSDCardDir();
        mediaPlayer_btnClick = new MediaPlayer().create(this,R.raw.click);
        scope_view_back=(ImageView)findViewById(R.id.scope_view_back);

        final Intent intent;
        intent = new Intent(Scope.this,MainActivity.class);
        scope_view_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!soundFlag) mediaPlayer_btnClick.start();
                startActivity(intent);
                finish();
            }
        });

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Prevent phone from sleeping
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK| PowerManager.ON_AFTER_RELEASE, "My Tag");
        this.mWakeLock.acquire();
    }

    @Override
    public void onStart() {

        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        if (!set) {
            setupScope();
            set=true;
        }
        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the session
        } else {
            if (mBTService == null) setupBT();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBTService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBTService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBTService.start();
            }
        }
    }

    private void setupBT() {
        //Bluetooth
        Log.d(TAG, "setupBT()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mBTService = new BluetoothService(this, mHandler);

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        // release screen being on
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services
        if (mBTService != null) mBTService.stop();

        // release screen being on
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMe(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothRfcommClient to write
            byte[] send = message.getBytes();
            mBTService.write(send);
        }
        System.out.printf("send %d!\n",message.length());
    }
    boolean rec_done=false;
    boolean draw_done=true;
    boolean start_rec=true;
//    char zero='0';
    byte temp[]=new byte[5];
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        public int byteArrayToInt(byte[] b, int offset) {
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int shift = (4 - 1 - i) * 8;
                value += (b[i + offset] & 0x000000FF) << shift;
            }
            return value;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            //mTitle.setText(R.string.title_connected_to);
                            //mTitle.append(mConnectedDeviceName);
                            online=true;
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            //mTitle.setText(R.string.title_connecting);
                            online=false;
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //mTitle.setText(R.string.title_not_connected);
                            online=false;
                            Toast.makeText(getApplicationContext(), R.string.offline,Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    //int data_length = msg.arg1;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    if (true){
                        for (int j=0;j<msg.arg1;j++){
                            temp[v_cnt]=readBuf[j];
                            v_cnt++;
                            if(v_cnt==5) {
                                v_cnt=0;
                                waveString[strIndex++] = new String(temp, 0, 5);
                                if (strIndex>=MAX_SAMPLES) {
                                    strIndex=0;
                                    //start_rec=false;
                                    v_cnt=0;
                                    setData();
                                    break;
                                }
//                                System.out.println("receive "+waveString[strIndex-1]);
                                //System.out.println("receive "+((char)waveByte[byteIndex][0]-zero)+((char)waveByte[byteIndex][1]-zero)+((char)waveByte[byteIndex][2]-zero)+((char)waveByte[byteIndex][3]-zero)+((char)waveByte[byteIndex][4]-zero));
                            }
                        }
                    }
//                    if (draw_done){
//                        rec_done=false;
//                        for (int j=0;j<data_length;j++){
//                            temp[v_cnt]=readBuf[j];
//                            v_cnt++;
//                            if(v_cnt==5) {
//                                v_cnt=0;
//                                waveString[strIndex++] = new String(temp, 0, 5);
//                                if (strIndex>=1024) {
//                                    rec_done=true;
//                                    v_cnt=0;
//                                    strIndex=0;
//                                    break;
//                                }
////                                System.out.println("receive "+waveString[strIndex-1]);
//                                //System.out.println("receive "+((char)waveByte[byteIndex][0]-zero)+((char)waveByte[byteIndex][1]-zero)+((char)waveByte[byteIndex][2]-zero)+((char)waveByte[byteIndex][3]-zero)+((char)waveByte[byteIndex][4]-zero));
//                            }
//                        }
//                    }
//                    if(draw_done){
//                        rec_done=false;T
//                        //System.out.printf("Strat receive %d!\n",msg.arg1);
//                        //int data=0;
//                        for(x=0; x<msg.arg1; x++){
////                            for (int i=0;i<5;i++){
////                                raw = UByte(readBuf[i]);
////                                data =data*10+raw;
////                            }
//                            //raw = UByte(readBuf[x]);
//                            if( byteIndex<(5) ){ // valid data
//                                waveByte[byteIndex]=readBuf[x];
//                                temp = new String(readBuf[x],);
//
//                                //System.out.printf("receive done %d!\n",waveData[dataIndex]);
//                                byteIndex++;
//                            }
//                            else {
//                                //waveByte[byteIndex]=readBuf[x];
//                                rec_done=true;
//                                System.out.printf("receive done %d!\n",byteIndex);
//                                byteIndex=0;
//                                break;
//                            }
//                        }
//                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), getString(R.string.connectedTo)
                            + mConnectedDeviceName, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_MEASURE:
                    Vp_max.setText(String.valueOf(max_val) + "V");
                    Vp_min.setText(String.valueOf(min_val) + "V");
                    break;
            }
        }
//        private int UByte(byte b){
//            if(b<0) // if negative
//                return (int)( (b&0x7F) + 128 );
//            else
//                return (int)b;
//        }
    };
    private int UByte(byte b){
        if(b<0) // if negative
            return (int)( (b&0x7F) + 128 );
        else
            return (int)b;
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mBTService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBT();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
                    //finish();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                //setStop();
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

    ThreadDraw threadDraw;
    private void setupScope () {
        //Scope
        Log.d(TAG, "setupScope()");

        //Button
        trig_up = (Button) findViewById(R.id.trig_up);
        trig_dn = (Button) findViewById(R.id.trig_dn);
        prt_sc = (Button) findViewById(R.id.prt_sc);
        btn_run = (Button) findViewById(R.id.btn_run);
        btn_stop = (Button) findViewById(R.id.btn_stop);

        //监听Button
        MyItemClickListener myItemClickListener = new MyItemClickListener();
        trig_up.setOnClickListener(myItemClickListener);
        trig_dn.setOnClickListener(myItemClickListener);
        btn_run.setOnClickListener(myItemClickListener);
        btn_stop.setOnClickListener(myItemClickListener);
        prt_sc.setOnClickListener(myItemClickListener);

        //显示offset电压的值
        trig = (TextView) findViewById(R.id.trig);

        trig_valInt = 0;// 显示
        trig_val = (double) trig_valInt / 100;
        trig.setText(getResources().getString(R.string.trig_vol) + Double.toString(trig_val) + "V");

        //显示触发电压的值
        Vp_max = (TextView) findViewById(R.id.Vp_max);
        Vp_min = (TextView) findViewById(R.id.Vp_min);
        max_valInt = min_valInt=0;// 显示
        max_val =min_val= (double) max_valInt*3.3 / 65535;
        Vp_max.setText(String.valueOf(max_val) + "V");
        Vp_min.setText(String.valueOf(min_val) + "V");
        // 初始化Spinner控件
        spin_A = (Spinner)findViewById(R.id.spin_A);
        spin_T = (Spinner)findViewById(R.id.spin_T);
        spin_O = (Spinner)findViewById(R.id.spin_O);
        spin_C = (Spinner)findViewById(R.id.spin_C);
        // 建立数据源
        String[] items_A = getResources().getStringArray(R.array.A);
        String[] items_T = getResources().getStringArray(R.array.T);
        String[] items_O = getResources().getStringArray(R.array.O);
        String[] items_C = getResources().getStringArray(R.array.C);
        // 建立Adapter并且绑定数据源
        ArrayAdapter<String> adapter_A=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items_A);
        ArrayAdapter<String> adapter_T=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items_T);
        ArrayAdapter<String> adapter_O=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items_O);
        ArrayAdapter<String> adapter_C=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, items_C);

        adapter_A.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_T.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_O.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_C.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //绑定 Adapter到控件
        spin_A .setAdapter(adapter_A);
        spin_T .setAdapter(adapter_T);
        spin_O .setAdapter(adapter_O);
        spin_C .setAdapter(adapter_C);

        MyItemSelectListener itemSelectListener=new MyItemSelectListener();
        spin_A.setOnItemSelectedListener(itemSelectListener);
        spin_T.setOnItemSelectedListener(itemSelectListener);
        spin_O.setOnItemSelectedListener(itemSelectListener);
        spin_C.setOnItemSelectedListener(itemSelectListener);

        //SurfaceView
        scope_view = (SurfaceView) findViewById(R.id.scope_view);
        Toast.makeText(Scope.this, R.string.landscape_note, Toast.LENGTH_SHORT).show();
        scope_holder = scope_view.getHolder();
        scope_holder.addCallback(new WaveDraw());
        scope_father =(LinearLayout)findViewById(R.id.scope_father);
        scope_grand = (LinearLayout)findViewById(R.id.scop_grand);
        scope_grandG = (LinearLayout)findViewById(R.id.scop_grandG);
        ///initialize
        for (int i=0;i<waveString.length;i++){
            waveString[i] = "0";
        }
        setData();
    }

    public class WaveDraw implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            //在surface的大小发生改变时激发
            System.out.println("surfaceChanged");
        }
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            //threadDraw=new ThreadDraw();
            //_run=true;
//            threadDraw.start();
            System.out.println("surfaceCreated");
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            //销毁时激发，一般在这里将画图的线程停止、释放。
            boolean retry = true;
            System.out.println("surfaceDestroyed==");
            setStop();
//            while (retry){
//                try{
//                    threadDraw.join();
//                    retry = false;
//                }catch(InterruptedException e){
//                    e.printStackTrace();
//                }
//            }
        }
    }
    boolean state;
    public boolean _run=false;
    boolean reset=true;
    public class ThreadDraw extends Thread {

        int xscale ;
        int yscale ;
        int voffset;
        int hoffset;
        int cnt;
        private Paint waveColor = new Paint();
        private Paint grid_paint = new Paint();
        private Paint cross_paint = new Paint();
        private Paint outline_paint = new Paint();
        public ThreadDraw(){
            xscale = scope_view.getWidth();
            yscale = scope_view.getHeight();
            voffset=yscale/2;
            hoffset=xscale/2;
            cnt=waveData.length;
//            waveColor.setColor(Color.rgb((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
            grid_paint.setColor(Color.rgb(100, 100, 100));
            waveColor.setColor(Color.YELLOW);
            cross_paint.setColor(Color.rgb(70, 100, 70));
            outline_paint.setColor(Color.GREEN);
        }

        @Override
        public void run(){
            Canvas c=null;
            System.out.println("thDraw Created!");
            while(_run){
                try {
                    System.out.println("start draw!");
                    c = scope_holder.lockCanvas(null);
                    if(!start_rec||!online) {
//                        setData();
                        mHandler.obtainMessage(Scope.MESSAGE_MEASURE, -1, -1, -1)
                                .sendToTarget();
                    }
                    if (reset) {
                        resetWave(c);
                    }
                    synchronized (scope_holder) {
                        doDraw(c);
                    }
                    if (online)
                        Thread.sleep(1);//睡眠时间为1秒
                    else Thread.sleep(10);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        scope_holder.unlockCanvasAndPost(c);
//                        draw_done=true;
//                        start_rec=true;
                        System.out.printf("Draw done !\n");
                        if (online){
//                            start_rec=true;
//                            sendMe("+UART ON");
                        }
                    }
                }
            }
            System.out.println("thDraw finished!");
        }
        private void doDraw(Canvas c){

            //1.这里就是核心了， 得到画布 ，然后在你的画布上画出要显示的内容

            //3.开画
            waveColor.setColor(Color.rgb((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255)));
            //plot data
            for(int i=0; i<(xscale-1); i++){
                if (online){
                    c.drawLine(i, voffset-trig_valInt- (waveData[i*hstep % cnt]-65535/2) * yscale / 65536 / vstep,
                            i+1,  voffset-trig_valInt-(waveData[(i+1)*hstep % cnt]-65535/2) * yscale / 65536 / vstep, waveColor);
                }
                else {
                    c.drawLine(i, -trig_valInt+ voffset-(sine1024[i*hstep % cnt]-65535/2) * yscale / 65536 / vstep ,
                            i+1, -trig_valInt+ voffset-(sine1024[(i+1)*hstep % cnt]-65535/2) * yscale / 65536 / vstep, waveColor);
                }
            }
        }
        public void resetWave(Canvas c){
            Paint p = new Paint();
            //clear screen
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            c.drawPaint(new Paint());
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            //2.scale
            c.drawLine(0, voffset+1, xscale+1, voffset+1, cross_paint);
            c.drawLine(hoffset, 0, hoffset, yscale, cross_paint);
            // draw grids
            for(int vertical = 1; vertical<10; vertical++){
                c.drawLine(
                        vertical*(xscale/10)+1, 1,
                        vertical*(xscale/10)+1, yscale+1,
                        grid_paint);
            }
            for(int horizontal = 1; horizontal<10; horizontal++){
                c.drawLine(
                        1, horizontal*(yscale/10)+1,
                        xscale+1, horizontal*(yscale/10)+1,
                        grid_paint);
            }
            // draw outline
            c.drawLine(0, 0, (xscale+1), 0, outline_paint);	// top
            c.drawLine((xscale+1), 0, (xscale+1), (yscale+1), outline_paint); //right
            c.drawLine(0, (yscale+1), (xscale+1), (yscale+1), outline_paint); // bottom
            c.drawLine(0, 0, 0, (yscale+1), outline_paint); //left
        }
    }

    public void setData(){
        setStop();
        if (online){
            System.out.println("waitting");
//                        while(!rec_done) ;
//                        draw_done=false;
           // while(start_rec) state=false;

            //System.out.println("Byte Data:!");
            max_valInt=min_valInt=0;
            waveData[0]=Integer.parseInt(waveString[0]);
            waveData[1]=Integer.parseInt(waveString[1]);
            for (int i=2;i<waveData.length;i=i+1){
//                            System.out.printf("%d",UByte(waveByte[i]));
                waveData[i]=Integer.parseInt(waveString[i]);
                if (Math.abs(waveData[i]-waveData[i-1])>1000) {
                    if(waveData[i]-waveData[i-2]>1000) waveData[i]=2*waveData[i-1]+waveData[i-2];
                }
                if (max_valInt<waveData[i]) max_valInt=waveData[i];
                if (min_valInt>waveData[i]) min_valInt=waveData[i];
            }
//            if (state==false){
//                v_cnt=0;
//                strIndex=0;
//                sendMe("+UART ON");
//                start_rec=state=true;
//            }
            v_cnt=0;
            strIndex=0;
            sendMe("+UART ON");
            start_rec=state=true;
            max_val=max_valInt*3.3/65535;
            min_val=min_valInt*3.3/65535;
        }
        else {
            System.out.println("Offline Data:!");
            max_valInt=min_valInt=0;
            for (int i=0;i<sine1024.length;i=i+1){
//                            System.out.printf("%d",UByte(waveByte[i]));
                if (max_valInt<sine1024[i]) max_valInt=sine1024[i];
                if (min_valInt>sine1024[i]) min_valInt=sine1024[i];
            }
            max_val=max_valInt*3.3/65535;
            min_val=min_valInt*3.3/65535;
        }
        if(set)
            setRun();
    }
    public void setRun(){
        _run = true;
        reset=true;
        threadDraw=new ThreadDraw();
        System.out.println("Start Running!");
        threadDraw.start();
    }
    public void setStop(){
        if(_run){
            _run=false;
        }
        else System.out.println("Not Running!");
    }
    public class MyItemSelectListener implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            //String[] A = getResources().getStringArray(R.array.A);
            //Toast.makeText(Scope.this, "你点击的是:"+items_A[pos], Toast.LENGTH_SHORT).show();
            if(!soundFlag) mediaPlayer_btnClick.start();
            //setStop();
            reset = true;
            switch (parent.getId()){
                case R.id.spin_A:
                    switch(pos){
                        case 0: vstep=1;break;
                        case 1: vstep=2;break;
                        case 2: vstep=5;break;
                        case 3: vstep=10;break;
                        default: break;
                    }
                    break;
                case R.id.spin_T:
                    switch(pos){
                        case 0: hstep=2;break;
                        case 1: hstep=4;break;
                        case 2: hstep=10;break;
                        case 3: hstep=20;break;
                        case 4: hstep=40;break;
                        default:break;
                    }
                    break;
                case R.id.spin_O:
                    switch(pos){
                        case 0:
                            break;
                        case 1:
                            break;
                        default: break;
                    }
                    break;
                case R.id.spin_C:
                    switch (pos){
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        default:break;
                    }
                    break;
                default:break;
            }
            //setRun();
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }
    }

    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
//            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";
            String mPath = Environment.getExternalStorageDirectory().toString() + "/OscScope/" + "screenshoot"+".jpg";
            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }

    }
    private void takeScreenshot(View view){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        String fname = Environment.getExternalStorageDirectory() + File.separator+"OscScope"+ File.separator +sdf.format(new Date()) + ".png";
        View v = view.getRootView();
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache();
        Bitmap bitmap = v.getDrawingCache();
        if(bitmap != null) {
            try{
                FileOutputStream out = new FileOutputStream(fname);
                bitmap.compress(Bitmap.CompressFormat.PNG,100, out);
                Toast.makeText(Scope.this,getString(R.string.savedImageTo)+fname,Toast.LENGTH_LONG).show();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(Scope.this, R.string.Failed,Toast.LENGTH_SHORT).show();
        }
    }
    private void takeScreenshotAll(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        String fname = Environment.getExternalStorageDirectory() + File.separator+"OscScope"+ File.separator +sdf.format(new Date()) + ".png";
        View v=getWindow().getDecorView().getRootView();
        int left=scope_father.getLeft()+scope_grand.getLeft()+scope_grandG.getLeft();
        int top=scope_father.getTop()+scope_grand.getTop()+scope_grandG.getTop()+110;
        Bitmap bitmap1 = Bitmap.createBitmap(scope_view.getWidth(),
                scope_view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bitCanvas = new Canvas(bitmap1);
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache();
        Bitmap bitmap = v.getDrawingCache();
        if (threadDraw!=null){
            threadDraw.doDraw(bitCanvas);//在自己创建的canvas上画
        }
        else {
            threadDraw=new ThreadDraw();
            threadDraw.doDraw(bitCanvas);//在自己创建的canvas上画
            threadDraw=null;
        }
        bitmap =Scope.combineBitmap(bitmap,bitmap1,left,top);
        if(bitmap != null) {
            try{
                FileOutputStream out = new FileOutputStream(fname);
                bitmap.compress(Bitmap.CompressFormat.PNG,100, out);
                Toast.makeText(Scope.this,getString(R.string.savedImageTo)+fname,Toast.LENGTH_LONG).show();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(Scope.this, R.string.Failed,Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 合并两张bitmap为一张
     * @param background
     * @param foreground
     * @return Bitmap
     */
    public static Bitmap combineBitmap(Bitmap background, Bitmap foreground,int x,int y) {
        if (background == null) {
            return null;
        }
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
//        int fgWidth = foreground.getWidth();
//        int fgHeight = foreground.getHeight();
        Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(background, 0, 0, null);
//        canvas.drawBitmap(foreground, (bgWidth - fgWidth) / 2,
//                (bgHeight - fgHeight) / 2, null);
        canvas.drawBitmap(foreground,x,y,null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        return newmap;
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    public class MyItemClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            if(!soundFlag) mediaPlayer_btnClick.start();
            switch (view.getId()){
                case R.id.trig_up:
                    trig_valInt += 5;
                    trig_val = (double) trig_valInt / 100;
                    trig.setText(getResources().getString(R.string.trig_vol) + Double.toString(trig_val) + "V");
                    break;
                case R.id.trig_dn:
                    trig_valInt -= 5;
                    trig_val = (double) trig_valInt / 100;
                    trig.setText(getResources().getString(R.string.trig_vol) + Double.toString(trig_val) + "V");
                    break;
                case R.id.btn_run:
                    setRun();
                    if (online){
                        sendMe("+UART ON");
                    }
                    btn_run.setClickable(false);
                    btn_stop.setClickable(true);
                    Toast.makeText(Scope.this, R.string.onDraw_note,Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btn_stop:
                    setStop();
                    if (online){
                        sendMe("+UART OFF");
                    }
                    btn_run.setClickable(true);
                    btn_stop.setClickable(false);
                    Toast.makeText(Scope.this, R.string.pause_note,Toast.LENGTH_SHORT).show();
                    break;
                case R.id.prt_sc:
                    takeScreenshotAll();
                    break;
                default:break;
            }
        }
    }

    public void createSDCardDir(){
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            // 创建一个文件夹对象，赋值为外部存储器的目录
            File sdcardDir =Environment.getExternalStorageDirectory();
            //得到一个路径，内容是sdcard的文件夹路径和名字
            String path=sdcardDir.getPath()+"/OscScope";
            File path1 = new File(path);
            if (!path1.exists()) {
                //若不存在，创建目录，可以在应用启动的时候创建
                path1.mkdirs();
                System.out.println("paht ok,path:"+path);
            }
        }
        else{
            System.out.println("failed to created path!");
            return;
        }

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

    //根据保存值设置初始化界面
    public void setPre() {

        share = getSharedPreferences("p", Activity.MODE_PRIVATE);
        soundFlag = share.getBoolean("soundFlag", false);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0 ) {
            Intent intent1 = new Intent(Scope.this, MainActivity.class);
            startActivity(intent1);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    static int sine1024[] = { 0x7FFF,0x80C8,0x8191,0x825A,0x8323,0x83EC,0x84B5,0x857E,0x8647,0x8710,0x87D8,0x88A1,0x896A,0x8A32,0x8AFA,0x8BC3
            ,0x8C8B,0x8D53,0x8E1B,0x8EE2,0x8FAA,0x9072,0x9139,0x9200,0x92C7,0x938E,0x9454,0x951B,0x95E1,0x96A7,0x976D,0x9832
            ,0x98F8,0x99BD,0x9A82,0x9B46,0x9C0A,0x9CCE,0x9D92,0x9E56,0x9F19,0x9FDC,0xA09E,0xA161,0xA223,0xA2E4,0xA3A5,0xA466
            ,0xA527,0xA5E7,0xA6A7,0xA766,0xA826,0xA8E4,0xA9A3,0xAA61,0xAB1E,0xABDB,0xAC98,0xAD54,0xAE10,0xAECB,0xAF86,0xB041
            ,0xB0FB,0xB1B4,0xB26D,0xB326,0xB3DE,0xB495,0xB54C,0xB603,0xB6B9,0xB76E,0xB823,0xB8D8,0xB98C,0xBA3F,0xBAF2,0xBBA4
            ,0xBC55,0xBD07,0xBDB7,0xBE67,0xBF16,0xBFC5,0xC073,0xC120,0xC1CD,0xC279,0xC324,0xC3CF,0xC47A,0xC523,0xC5CC,0xC674
            ,0xC71C,0xC7C2,0xC869,0xC90E,0xC9B3,0xCA57,0xCAFA,0xCB9D,0xCC3F,0xCCE0,0xCD80,0xCE20,0xCEBF,0xCF5D,0xCFFA,0xD097
            ,0xD132,0xD1CE,0xD268,0xD301,0xD39A,0xD432,0xD4C9,0xD55F,0xD5F4,0xD689,0xD71D,0xD7AF,0xD842,0xD8D3,0xD963,0xD9F3
            ,0xDA81,0xDB0F,0xDB9C,0xDC28,0xDCB3,0xDD3D,0xDDC6,0xDE4F,0xDED6,0xDF5D,0xDFE2,0xE067,0xE0EB,0xE16E,0xE1F0,0xE271
            ,0xE2F1,0xE370,0xE3EE,0xE46B,0xE4E7,0xE562,0xE5DD,0xE656,0xE6CE,0xE745,0xE7BC,0xE831,0xE8A5,0xE918,0xE98B,0xE9FC
            ,0xEA6C,0xEADB,0xEB4A,0xEBB7,0xEC23,0xEC8E,0xECF8,0xED61,0xEDC9,0xEE2F,0xEE95,0xEEFA,0xEF5E,0xEFC0,0xF022,0xF082
            ,0xF0E1,0xF140,0xF19D,0xF1F9,0xF254,0xF2AE,0xF306,0xF35E,0xF3B4,0xF40A,0xF45E,0xF4B1,0xF503,0xF554,0xF5A4,0xF5F3
            ,0xF640,0xF68D,0xF6D8,0xF722,0xF76B,0xF7B3,0xF7F9,0xF83F,0xF883,0xF8C6,0xF908,0xF949,0xF989,0xF9C7,0xFA04,0xFA41
            ,0xFA7C,0xFAB5,0xFAEE,0xFB25,0xFB5C,0xFB91,0xFBC4,0xFBF7,0xFC29,0xFC59,0xFC88,0xFCB6,0xFCE2,0xFD0E,0xFD38,0xFD61
            ,0xFD89,0xFDB0,0xFDD5,0xFDF9,0xFE1C,0xFE3E,0xFE5E,0xFE7E,0xFE9C,0xFEB9,0xFED4,0xFEEF,0xFF08,0xFF20,0xFF37,0xFF4C
            ,0xFF61,0xFF74,0xFF86,0xFF96,0xFFA6,0xFFB4,0xFFC1,0xFFCD,0xFFD7,0xFFE0,0xFFE8,0xFFEF,0xFFF5,0xFFF9,0xFFFC,0xFFFE
            ,0xFFFE,0xFFFE,0xFFFC,0xFFF9,0xFFF5,0xFFEF,0xFFE8,0xFFE0,0xFFD7,0xFFCD,0xFFC1,0xFFB4,0xFFA6,0xFF96,0xFF86,0xFF74
            ,0xFF61,0xFF4C,0xFF37,0xFF20,0xFF08,0xFEEF,0xFED4,0xFEB9,0xFE9C,0xFE7E,0xFE5E,0xFE3E,0xFE1C,0xFDF9,0xFDD5,0xFDAF
            ,0xFD89,0xFD61,0xFD38,0xFD0E,0xFCE2,0xFCB6,0xFC88,0xFC59,0xFC28,0xFBF7,0xFBC4,0xFB91,0xFB5C,0xFB25,0xFAEE,0xFAB5
            ,0xFA7C,0xFA41,0xFA04,0xF9C7,0xF989,0xF949,0xF908,0xF8C6,0xF883,0xF83F,0xF7F9,0xF7B3,0xF76B,0xF722,0xF6D8,0xF68D
            ,0xF640,0xF5F3,0xF5A4,0xF554,0xF503,0xF4B1,0xF45E,0xF40A,0xF3B4,0xF35E,0xF306,0xF2AE,0xF254,0xF1F9,0xF19D,0xF140
            ,0xF0E1,0xF082,0xF022,0xEFC0,0xEF5E,0xEEFA,0xEE95,0xEE2F,0xEDC9,0xED61,0xECF8,0xEC8E,0xEC23,0xEBB7,0xEB49,0xEADB
            ,0xEA6C,0xE9FC,0xE98B,0xE918,0xE8A5,0xE831,0xE7BC,0xE745,0xE6CE,0xE656,0xE5DC,0xE562,0xE4E7,0xE46B,0xE3EE,0xE370
            ,0xE2F1,0xE270,0xE1F0,0xE16E,0xE0EB,0xE067,0xDFE2,0xDF5D,0xDED6,0xDE4F,0xDDC6,0xDD3D,0xDCB3,0xDC28,0xDB9C,0xDB0F
            ,0xDA81,0xD9F2,0xD963,0xD8D3,0xD841,0xD7AF,0xD71C,0xD689,0xD5F4,0xD55F,0xD4C9,0xD432,0xD39A,0xD301,0xD268,0xD1CD
            ,0xD132,0xD097,0xCFFA,0xCF5D,0xCEBE,0xCE20,0xCD80,0xCCE0,0xCC3E,0xCB9D,0xCAFA,0xCA57,0xC9B3,0xC90E,0xC868,0xC7C2
            ,0xC71C,0xC674,0xC5CC,0xC523,0xC479,0xC3CF,0xC324,0xC279,0xC1CD,0xC120,0xC073,0xBFC5,0xBF16,0xBE67,0xBDB7,0xBD06
            ,0xBC55,0xBBA4,0xBAF2,0xBA3F,0xB98B,0xB8D8,0xB823,0xB76E,0xB6B9,0xB603,0xB54C,0xB495,0xB3DE,0xB326,0xB26D,0xB1B4
            ,0xB0FA,0xB040,0xAF86,0xAECB,0xAE10,0xAD54,0xAC98,0xABDB,0xAB1E,0xAA60,0xA9A2,0xA8E4,0xA825,0xA766,0xA6A7,0xA5E7
            ,0xA527,0xA466,0xA3A5,0xA2E4,0xA222,0xA160,0xA09E,0x9FDC,0x9F19,0x9E55,0x9D92,0x9CCE,0x9C0A,0x9B46,0x9A81,0x99BC
            ,0x98F7,0x9832,0x976D,0x96A7,0x95E1,0x951B,0x9454,0x938E,0x92C7,0x9200,0x9139,0x9071,0x8FAA,0x8EE2,0x8E1A,0x8D53
            ,0x8C8B,0x8BC2,0x8AFA,0x8A32,0x8969,0x88A1,0x87D8,0x870F,0x8647,0x857E,0x84B5,0x83EC,0x8323,0x825A,0x8191,0x80C8
            ,0x7FFF,0x7F36,0x7E6D,0x7DA4,0x7CDB,0x7C12,0x7B49,0x7A80,0x79B7,0x78EE,0x7825,0x775D,0x7694,0x75CC,0x7503,0x743B
            ,0x7373,0x72AB,0x71E3,0x711B,0x7054,0x6F8C,0x6EC5,0x6DFE,0x6D37,0x6C70,0x6BA9,0x6AE3,0x6A1D,0x6957,0x6891,0x67CB
            ,0x6706,0x6641,0x657C,0x64B8,0x63F3,0x632F,0x626C,0x61A8,0x60E5,0x6022,0x5F5F,0x5E9D,0x5DDB,0x5D1A,0x5C58,0x5B97
            ,0x5AD7,0x5A17,0x5957,0x5897,0x57D8,0x5719,0x565B,0x559D,0x54E0,0x5423,0x5366,0x52AA,0x51EE,0x5133,0x5078,0x4FBD
            ,0x4F03,0x4E4A,0x4D91,0x4CD8,0x4C20,0x4B68,0x4AB1,0x49FB,0x4945,0x488F,0x47DA,0x4726,0x4672,0x45BF,0x450C,0x445A
            ,0x43A8,0x42F7,0x4247,0x4197,0x40E8,0x4039,0x3F8B,0x3EDE,0x3E31,0x3D85,0x3CD9,0x3C2E,0x3B84,0x3ADB,0x3A32,0x398A
            ,0x38E2,0x383B,0x3795,0x36F0,0x364B,0x35A7,0x3504,0x3461,0x33BF,0x331E,0x327E,0x31DE,0x313F,0x30A1,0x3004,0x2F67
            ,0x2ECB,0x2E30,0x2D96,0x2CFD,0x2C64,0x2BCC,0x2B35,0x2A9F,0x2A09,0x2975,0x28E1,0x284E,0x27BC,0x272B,0x269B,0x260B
            ,0x257D,0x24EF,0x2462,0x23D6,0x234B,0x22C1,0x2238,0x21AF,0x2128,0x20A1,0x201B,0x1F97,0x1F13,0x1E90,0x1E0E,0x1D8D
            ,0x1D0D,0x1C8E,0x1C10,0x1B93,0x1B17,0x1A9B,0x1A21,0x19A8,0x1930,0x18B8,0x1842,0x17CD,0x1759,0x16E5,0x1673,0x1602
            ,0x1592,0x1522,0x14B4,0x1447,0x13DB,0x1370,0x1306,0x129D,0x1235,0x11CE,0x1169,0x1104,0x10A0,0x103E,0x0FDC,0x0F7C
            ,0x0F1C,0x0EBE,0x0E61,0x0E05,0x0DAA,0x0D50,0x0CF8,0x0CA0,0x0C49,0x0BF4,0x0BA0,0x0B4D,0x0AFA,0x0AAA,0x0A5A,0x0A0B
            ,0x09BE,0x0971,0x0926,0x08DC,0x0893,0x084B,0x0805,0x07BF,0x077B,0x0738,0x06F6,0x06B5,0x0675,0x0637,0x05F9,0x05BD
            ,0x0582,0x0549,0x0510,0x04D9,0x04A2,0x046D,0x043A,0x0407,0x03D5,0x03A5,0x0376,0x0348,0x031C,0x02F0,0x02C6,0x029D
            ,0x0275,0x024E,0x0229,0x0205,0x01E2,0x01C0,0x01A0,0x0180,0x0162,0x0145,0x012A,0x010F,0x00F6,0x00DE,0x00C7,0x00B2
            ,0x009D,0x008A,0x0078,0x0068,0x0058,0x004A,0x003D,0x0031,0x0027,0x001E,0x0016,0x000F,0x0009,0x0005,0x0002,0x0000
            ,0x0000,0x0000,0x0002,0x0005,0x0009,0x000F,0x0016,0x001E,0x0027,0x0031,0x003D,0x004A,0x0058,0x0068,0x0078,0x008A
            ,0x009D,0x00B2,0x00C7,0x00DE,0x00F6,0x010F,0x012A,0x0145,0x0162,0x0180,0x01A0,0x01C0,0x01E2,0x0205,0x0229,0x024F
            ,0x0275,0x029D,0x02C6,0x02F0,0x031C,0x0348,0x0376,0x03A5,0x03D6,0x0407,0x043A,0x046E,0x04A3,0x04D9,0x0510,0x0549
            ,0x0583,0x05BE,0x05FA,0x0637,0x0675,0x06B5,0x06F6,0x0738,0x077B,0x07BF,0x0805,0x084C,0x0893,0x08DC,0x0926,0x0972
            ,0x09BE,0x0A0B,0x0A5A,0x0AAA,0x0AFB,0x0B4D,0x0BA0,0x0BF4,0x0C4A,0x0CA0,0x0CF8,0x0D51,0x0DAA,0x0E05,0x0E61,0x0EBF
            ,0x0F1D,0x0F7C,0x0FDD,0x103E,0x10A1,0x1104,0x1169,0x11CF,0x1236,0x129D,0x1306,0x1370,0x13DB,0x1448,0x14B5,0x1523
            ,0x1592,0x1602,0x1674,0x16E6,0x1759,0x17CD,0x1843,0x18B9,0x1930,0x19A8,0x1A22,0x1A9C,0x1B17,0x1B93,0x1C10,0x1C8F
            ,0x1D0E,0x1D8E,0x1E0F,0x1E91,0x1F13,0x1F97,0x201C,0x20A2,0x2128,0x21B0,0x2238,0x22C1,0x234C,0x23D7,0x2463,0x24EF
            ,0x257D,0x260C,0x269B,0x272C,0x27BD,0x284F,0x28E2,0x2975,0x2A0A,0x2A9F,0x2B36,0x2BCD,0x2C65,0x2CFD,0x2D97,0x2E31
            ,0x2ECC,0x2F68,0x3004,0x30A2,0x3140,0x31DF,0x327E,0x331F,0x33C0,0x3462,0x3504,0x35A8,0x364C,0x36F0,0x3796,0x383C
            ,0x38E3,0x398A,0x3A32,0x3ADB,0x3B85,0x3C2F,0x3CDA,0x3D85,0x3E32,0x3EDE,0x3F8C,0x403A,0x40E8,0x4198,0x4247,0x42F8
            ,0x43A9,0x445B,0x450D,0x45BF,0x4673,0x4727,0x47DB,0x4890,0x4946,0x49FC,0x4AB2,0x4B69,0x4C21,0x4CD9,0x4D91,0x4E4A
            ,0x4F04,0x4FBE,0x5078,0x5133,0x51EF,0x52AA,0x5367,0x5423,0x54E0,0x559E,0x565C,0x571A,0x57D9,0x5898,0x5957,0x5A17
            ,0x5AD8,0x5B98,0x5C59,0x5D1A,0x5DDC,0x5E9E,0x5F60,0x6023,0x60E6,0x61A9,0x626C,0x6330,0x63F4,0x64B8,0x657D,0x6642
            ,0x6707,0x67CC,0x6892,0x6958,0x6A1E,0x6AE4,0x6BAA,0x6C71,0x6D38,0x6DFE,0x6EC6,0x6F8D,0x7054,0x711C,0x71E4,0x72AC
            ,0x7374,0x743C,0x7504,0x75CD,0x7695,0x775E,0x7826,0x78EF,0x79B8,0x7A81,0x7B49,0x7C12,0x7CDB,0x7DA4,0x7E6D,0x7F36};

}
