package com.project.pinetree408.sbspeechrecognition;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.github.pwittchen.neurosky.library.NeuroSky;
import com.github.pwittchen.neurosky.library.exception.BluetoothNotEnabledException;
import com.github.pwittchen.neurosky.library.listener.ExtendedDeviceMessageListener;
import com.github.pwittchen.neurosky.library.message.enums.BrainWave;
import com.github.pwittchen.neurosky.library.message.enums.Signal;
import com.github.pwittchen.neurosky.library.message.enums.State;
import com.project.pinetree408.sbspeechrecognition.dummy.BleHeartRateSensor;
import com.project.pinetree408.sbspeechrecognition.dummy.BleSensor;
import com.project.pinetree408.sbspeechrecognition.dummy.BleSensors;
import com.project.pinetree408.sbspeechrecognition.dummy.BleService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import connectivity.bluetooth.BluetoothChatService;
import connectivity.bluetooth.BluetoothHandlerMessage;
import connectivity.bluetooth.DeviceListActivity;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener, SensorEventListener,EmpaDataDelegate, EmpaStatusDelegate ,AdapterView.OnItemSelectedListener, Observer {
    private final int IMU_samplingRate = 20;

    private Spinner spinner1;
    boolean searchBt = true;
    BluetoothAdapter mBluetoothAdapter;
    List<BluetoothDevice> pairedDevices = new ArrayList<>();
    boolean menuBool = false; //display or not the disconnect option
    //sensor
    private long startTime, neuroStart;
    private SensorManager manager;
    private Sensor sensorGyro, sensorAcc;
    int upddated_gyro_at= Integer.MAX_VALUE, upddated_acc_at=Integer.MAX_VALUE, upddated_ppg_at = Integer.MAX_VALUE;
    private TextView gyroX,gyroY, gyroZ, accX, accY, accZ;
    private double[] acc,gyro,ppg;

    CountDownTimer sensorDataSender;

    private TextView viewHR;
    private BleService bleService;
    private BleService libleService;
    private BleHeartRateSensor polarHR;
    private String serviceUuid;
    private String deviceAddress;
    public static final String EXTRAS_DEVICE_ADDRESS = "40:A0:BB:2C";
    public static final String EXTRAS_SENSOR_UUID = "0000180d-0000-1000-8000-00805f9b34fb"; //Heart Rate Service
    private String HR_TAG = "Polar HR";

    private String NEURO_TAG = "Mindwave2";
    private NeuroSky neuroSky;

    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private final int BODY_SENSOR_REQUEST = 5999;
    private final int INTERNET_SENSOR_REQUEST = 5998;

    private static final String EMPATICA_API_KEY = "9ceaa818dd5f4441a4605f3e9de9c4ca"; // TODO insert your API Key here

    private EmpaDeviceManager deviceManager = null;
    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView temperatureLabel;
    private TextView batteryLabel;
    private TextView statusLabel;
    private TextView deviceNameLabel;
    private LinearLayout dataCnt;
    private TextView tvState;
    private TextView tvAttention;
    private TextView tvBlink;
    private TextView tvMeditation;

    float e4_accel_x, e4_accel_y, e4_accel_z, e4_bvp, e4_gsr, e4_ibi, e4_skin_temp, heart_rate, brain_wave_high_alpha, brain_wave_low_alpha, brain_wave_high_beta, brain_wave_low_beta, brain_wave_middle_gamma, brain_wave_low_gamma, brain_wave_theta, brain_wave_delta;
    //Sensors

    Socket socket;
    //String ip = "143.248.199.126";
    String ip = "143.248.53.191";
    int port = 5000;
    String portPath = "speech";
    String portPathSensor = "sensor";
    boolean socketConnectionFlag = false;

    boolean recflag = false;
    MediaRecorder myAudioRecorder = null;


    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;

//    private SpeechService mSpeechService;
//
//    private VoiceRecorder mVoiceRecorder;
//    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {
//
//        @Override
//        public void onVoiceStart() {
//            showStatus(true);
//            if (mSpeechService != null) {
//                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
//            }
//        }
//
//        @Override
//        public void onVoice(byte[] data, int size) {
//            if (mSpeechService != null) {
//                mSpeechService.recognize(data, size);
//            }
//        }
//
//        @Override
//        public void onVoiceEnd() {
//            showStatus(false);
//            if (mSpeechService != null) {
//                mSpeechService.finishRecognizing();
//            }
//        }
//    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Button mTurnButton;

    private boolean turnFlag;

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder binder) {
//            mSpeechService = SpeechService.from(binder);
//            mSpeechService.addListener(mSpeechServiceListener);
//            mStatus.setVisibility(View.VISIBLE);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) { mSpeechService = null; }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar(findViewById(R.id.toolbar));
        mStatus = findViewById(R.id.status);
        mText = findViewById(R.id.text);

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ArrayList<String> results = savedInstanceState == null ? null :
                savedInstanceState.getStringArrayList(STATE_RESULTS);
        mAdapter = new ResultAdapter(results);
        mRecyclerView.setAdapter(mAdapter);



        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Toast.makeText(this,  android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address"), Toast.LENGTH_LONG).show();

        turnFlag = false;
        mTurnButton = findViewById(R.id.turn_button);
        mTurnButton.setOnTouchListener((View v, MotionEvent event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    turnFlag = true;
                    mTurnButton.setText("Your Turn");
                    break;
                case MotionEvent.ACTION_UP:
                    turnFlag = false;
                    mTurnButton.setText("Get Turn");
                    break;
            }
            return true;
        });

        checkPermission();
        //initSensor();
        initE4();

        initNeurosky();
        initHR();

        sensorDataSender = new CountDownTimer(50, 50) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                if (socketConnectionFlag) {
                    socket.emit(portPathSensor,
                            Float.toString(e4_accel_x),
                            Float.toString(e4_accel_y),
                            Float.toString(e4_accel_z),
                            Float.toString(e4_bvp),
                            Float.toString(e4_gsr),
                            Float.toString(e4_ibi),
                            Float.toString(e4_skin_temp),
                            Float.toString(heart_rate),
                            Float.toString(brain_wave_high_alpha),
                            Float.toString(brain_wave_low_alpha),
                            Float.toString(brain_wave_high_beta),
                            Float.toString(brain_wave_low_beta),
                            Float.toString(brain_wave_middle_gamma),
                            Float.toString(brain_wave_low_gamma),
                            Float.toString(brain_wave_theta),
                            Float.toString(brain_wave_delta));
                    Log.d("data", ""+
                            e4_accel_x+", "+
                            e4_accel_y+", "+
                            e4_accel_z+", "+
                            e4_bvp+", "+
                            e4_gsr+", "+
                            e4_ibi+", "+
                            e4_skin_temp+", "+
                            heart_rate+", "+
                            brain_wave_high_alpha+", "+
                            brain_wave_low_alpha+", "+
                            brain_wave_high_beta+", "+
                            brain_wave_low_beta+", "+
                            brain_wave_middle_gamma+", "+
                            brain_wave_low_gamma+", "+
                            brain_wave_theta+", "+
                            brain_wave_delta);

                }
                sensorDataSender.start();
            }
        }.start();

    }

    public void checkPermission(){

        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // If do not grant write external storage permission.
        if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED) {
            // Request user to grant write external storage permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS},
                    BODY_SENSOR_REQUEST);
        }
        if (checkSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.INTERNET},
                    INTERNET_SENSOR_REQUEST);


        }
    }
    public void initSensor(){
        gyroX = (TextView)findViewById(R.id.gyro_x_phone);
        gyroY = (TextView)findViewById(R.id.gyro_y_phone);
        gyroZ = (TextView)findViewById(R.id.gyro_z_phone);
        accX = (TextView)findViewById(R.id.accel_x_phone);
        accY = (TextView)findViewById(R.id.accel_y_phone);
        accZ = (TextView)findViewById(R.id.accel_z_phone);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorAcc = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        List<Sensor> sensorList = manager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor currentSensor : sensorList) {
            Log.d("List sensors", "Name: "+currentSensor.getName() + " /Type_String: " +currentSensor.getStringType()+ " /Type_number: "+currentSensor.getType());
            if(currentSensor.getName().contains("ppg") || currentSensor.getName().contains("heart")){
                int SENSOR_PPG = currentSensor.getType();
            }
        }

        acc = new double[3];
        gyro = new double[3];
        startTime = System.currentTimeMillis();
        registListener();

    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();

            return;
        }else{

            // Check that there's actually something to send
            if (message.length() > 0) {
                // Get the message bytes and tell the BluetoothChatService to write
                message += "\r\n";
                byte[] send = message.getBytes();
                mChatService.write(send);
                Log.i("send to G", message);

                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                //mOutEditText.setText(mOutStringBuffer);
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API

//        bindService(
//                new Intent(this, SpeechService.class),
//                mServiceConnection, BIND_AUTO_CREATE);

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }

        initSocketToServer();
    }

    private void initSocketToServer(){

        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        try {
            socket = IO.socket("http://" + ip + ":" + port + "/mynamespace", opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        socketConnectionFlag = true;
                    }
                });
            }

        }).on("response", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                try {

                    JSONObject response = new JSONObject(args[0].toString());
                    String result = response.get("data").toString();
                    String type = response.get("type").toString();
                    Log.i("Result", result);

                    if(type.compareTo("Result") == 0){

                        sendMessage(result); // To glasses
                    }

                } catch (JSONException e){

                }

            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {}
        });

        socket.connect();
    }

    private void setupChat() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mBtHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mBtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothHandlerMessage.MESSAGE_CONNECT:
                    Intent serverIntent = null;
                    // Launch the DeviceListActivity to see devices and do scan
                    serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    break;
                case BluetoothHandlerMessage.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case BluetoothHandlerMessage.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    mChatService.write(writeBuf);
                    break;
                case BluetoothHandlerMessage.MESSAGE_READ:
                    try {
                        addItemOnList((new String((byte[]) msg.obj, 0, msg.arg1,"UTF-8")),true);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                case BluetoothHandlerMessage.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothHandlerMessage.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), (String)msg.obj,
                            Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothHandlerMessage.MESSAGE_VIBRATE:
                    break;
                case BluetoothHandlerMessage.MESSAGE_END:
                    MainActivity.this.finish();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }
    @Override
    public synchronized void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
        initSensor ();
        ensureDiscoverable();

        if (neuroSky != null ) {
            neuroSky.startMonitoring();
        }
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();

        // Stop Cloud Speech API
//        mSpeechService.removeListener(mSpeechServiceListener);
//        unbindService(mServiceConnection);
//        mSpeechService = null;
//        if (mChatService != null) mChatService.stop();
        socket.disconnect();

        unregistListener();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
//                mSpeechService.recognizeInputStream(getResources().openRawResource(R.raw.audio));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
//        if (mVoiceRecorder != null) {
//            mVoiceRecorder.stop();
//        }
//        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
//        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
//        if (mVoiceRecorder != null) {
//            mVoiceRecorder.stop();
//            mVoiceRecorder = null;
//        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(() -> mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing));
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.BLUETOOTH},
                REQUEST_BLUETOOTH_PERMISSION);
    }

    public void addItemOnList(String text, boolean isFinal){

        if(!TextUtils.isEmpty(text)) {
            runOnUiThread(() -> {
                if (isFinal) {
                    mText.setText(null);
                    mAdapter.addResult(text);
                    mRecyclerView.smoothScrollToPosition(0);
                } else {
                    mText.setText(text);
                }

            });
        }
    }
    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final float confidence, final boolean isFinal) {
                    String date = df.format(Calendar.getInstance().getTime());
                    String log = date + "~" + turnFlag + "~" + text + "~" + confidence;
                    if (isFinal) {
                        writeLog("pilot-jm.txt", log + "\n");
                        Log.d("SpeechRecognizeResult-Final", log);
//                        mVoiceRecorder.dismiss();
                        socket.emit(portPath,
                                text);
                    }
                    if (mText != null){
                        addItemOnList(text,isFinal);
                    }
                }
            };

    public void listBT() {
        Log.d("Main Activity", "Listing BT elements");
        if (searchBt) {
            //Discover bluetooth devices
            final List<String> list = new ArrayList<>();
            list.add("");
            pairedDevices.addAll(mBluetoothAdapter.getBondedDevices());
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    list.add(device.getName() + "\n" + device.getAddress());
                }
            }

            //Populate drop down
            spinner1 = (Spinner) findViewById(R.id.spinner1);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, list);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner1.setOnItemSelectedListener(this);
            spinner1.setAdapter(dataAdapter);

            if (DataHandler.getInstance().getID() != 0 && DataHandler.getInstance().getID() < spinner1.getCount())
                spinner1.setSelection(DataHandler.getInstance().getID());
        }
    }
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                               long arg3) {


        if (arg2 != 0) {
            //Actual work
            DataHandler.getInstance().setID(arg2);
            if (((BluetoothDevice) pairedDevices.toArray()[DataHandler.getInstance().getID() - 1]).getName().contains("H7") && DataHandler.getInstance().getReader() == null) {

                Log.i("Main Activity", "Starting h7");
                DataHandler.getInstance().setH7(new H7ConnectThread((BluetoothDevice) pairedDevices.toArray()[DataHandler.getInstance().getID() - 1], this));
            }
            menuBool = true;

        }

    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void receiveH7Data() {
        //ANALYTIC
        //t.setScreenName("Polar Bluetooth Used");
        //t.send(new HitBuilders.AppViewBuilder().build());

        runOnUiThread(new Runnable() {
            public void run() {
                //menuBool=true;
                heart_rate = DataHandler.getInstance().getLastIntValue();
                updateLabel(viewHR, "" + heart_rate);

            }
        });
    }
    @Override
    public void update(Observable o, Object arg) {
        receiveH7Data();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_result, parent, false));
            text = itemView.findViewById(R.id.text);
        }
    }

    private static class ResultAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final ArrayList<String> mResults = new ArrayList<>();

        ResultAdapter(ArrayList<String> results) {
            if (results != null) {
                mResults.addAll(results);
            }
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.text.setText(mResults.get(position));
        }

        @Override
        public int getItemCount() { return mResults.size(); }

        void addResult(String result) {
            mResults.add(0, result);
            notifyItemInserted(0);
        }

        public ArrayList<String> getResults() { return mResults; }
    }


    private void writeLog(String fileName, String contents) {
        // save to the File
        try {
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + fileName;
            FileOutputStream fos = new FileOutputStream(filePath, true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(contents);
            writer.flush();
            writer.close();
            fos.close();
        } catch (Exception e) {
            e.getMessage();
        }
    }
    @Override
    protected void attachBaseContext(Context base) {
        //https://developer.android.com/studio/build/multidex?hl=ko
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    public void initHR(){

        DataHandler.getInstance().addObserver((Observer) this);
        //verify if bluetooth device are enabled
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (DataHandler.getInstance().newValue) {
            //Verify if bluetooth if activated, if not activate it
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("H& BT")
                            .setMessage("BT off")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mBluetoothAdapter.enable();
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    listBT();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    searchBt = false;
                                }
                            })
                            .show();
                } else {
                    listBT();
                }
            }
            DataHandler.getInstance().setNewValue(false);

        } else {
            listBT();

        }
        viewHR = (TextView)findViewById(R.id.tv_HR);
        final Button connection_btn = findViewById(R.id.btn_HR);
        connection_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());


                if (bleService != null) {
                    final boolean result = bleService.connect(EXTRAS_DEVICE_ADDRESS);
                    Log.d(HR_TAG, "Connect request result=" + result);
                }
            }
        });

        final Button record_btn = findViewById(R.id.recbtn);
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.DEFAULT);

        final Handler handler = new Handler();
        final Runnable startrec = new Runnable() {
            @Override
            public void run() {
                try{
                    myAudioRecorder.stop();
                    myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                    myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.DEFAULT);
                } catch (IllegalStateException ise) {
                    // make something ...
                    System.out.println(2);
                }
                long time = System.currentTimeMillis();
                SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
                String filename = dayTime.format(new Date(time));
                String foldername = "/suggestbot_voice/";
                String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + foldername+ filename + ".3gp";
                myAudioRecorder.setOutputFile(outputFile);
                System.out.println(outputFile);
                int duration_milli = 1000;
                try{
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();
                } catch (IllegalStateException ise) {
                    // make something ...
                } catch (IOException ioe) {
                    // make something
                }
                handler.postDelayed(this, duration_milli);
            }
        };

        record_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recflag) {
                    handler.removeCallbacks(startrec);
                    Toast.makeText(getApplicationContext(), "Audio file saved", Toast.LENGTH_LONG).show();
                    record_btn.setText("Audio Record");
                    recflag = false;
                }
                else{
                    startrec.run();
                    Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                    record_btn.setText("Recording..");
                    recflag = true;
                }
            }
        });
    }

    public void connectionError() {

        Log.w("Main Activity", "H7 Connection error occured");
        if (menuBool) {//did not manually tried to disconnect
            Log.d("Main Activity", "in the app");
            menuBool = false;
            final MainActivity ac = this;
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getBaseContext(), "H7 connection Error", Toast.LENGTH_SHORT).show();
                    //TextView rpm = (TextView) findViewById(R.id.rpm);
                    //rpm.setText("0 BMP");
                    Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
                    if (DataHandler.getInstance().getID() < spinner1.getCount())
                        spinner1.setSelection(DataHandler.getInstance().getID());

                    Log.w("Main Activity", "starting H7 after error");
                    DataHandler.getInstance().setReader(null);
                    DataHandler.getInstance().setH7(new H7ConnectThread((BluetoothDevice) pairedDevices.toArray()[DataHandler.getInstance().getID() - 1], ac));
                }
            });
        }
    }

    public void initE4(){

        // Initialize vars that reference UI components
        statusLabel = (TextView) findViewById(R.id.status_e4);
        dataCnt = (LinearLayout) findViewById(R.id.dataArea);
        accel_xLabel = (TextView) findViewById(R.id.accel_x);
        accel_yLabel = (TextView) findViewById(R.id.accel_y);
        accel_zLabel = (TextView) findViewById(R.id.accel_z);
        bvpLabel = (TextView) findViewById(R.id.bvp);
        edaLabel = (TextView) findViewById(R.id.eda);
        ibiLabel = (TextView) findViewById(R.id.ibi);
        //temperatureLabel = (TextView) findViewById(R.id.temperature);
        //batteryLabel = (TextView) findViewById(R.id.battery);
        deviceNameLabel = (TextView) findViewById(R.id.deviceName);

        final Button disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceManager != null) {
                    deviceManager.disconnect();
                }
            }
        });
        initEmpaticaDeviceManager();

    }
    public void initNeurosky(){

        tvState = (TextView) findViewById(R.id.tv_state);
        tvAttention = (TextView) findViewById(R.id.tv_attention);
        tvMeditation = (TextView) findViewById(R.id.tv_meditation);
        tvBlink = (TextView) findViewById(R.id.tv_blink);
        neuroSky = createNeuroSky();
        final Button connectButton = findViewById(R.id.btn_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    neuroSky.connect();
                } catch (BluetoothNotEnabledException e) {
                    Log.e("Exception", e.getMessage());
                }
            }
        });
        final Button disconnectButton = findViewById(R.id.btn_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                neuroSky.disconnect();
            }
        });
        final Button startButton = findViewById(R.id.btn_start_monitoring);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (neuroSky != null ) {
                    neuroSky.startMonitoring();
                }
            }
        });
        final Button stopButton = findViewById(R.id.btn_stop_monitoring);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (neuroSky != null ) {
                    neuroSky.stopMonitoring();
                }
            }
        });

    }

    @NonNull private NeuroSky createNeuroSky() {
        return new NeuroSky(new ExtendedDeviceMessageListener() {
            @Override public void onStateChange(State state) {
                handleStateChange(state);
            }

            @Override public void onSignalChange(Signal signal) {
                handleSignalChange(signal);
            }

            @Override public void onBrainWavesChange(Set<BrainWave> brainWaves) {
                handleBrainWavesChange(brainWaves);
            }
        });
    }

    private void handleStateChange(final State state) {
        if (neuroSky != null && state.equals(State.CONNECTED)) {
            neuroSky.startMonitoring();
        }

        tvState.setText(state.toString());
    }
    private void handleSignalChange(final Signal signal) {
        switch (signal) {
            case ATTENTION:
                tvAttention.setText(getFormattedMessage("attention: %d", signal));

                break;
            case MEDITATION:
                tvMeditation.setText(getFormattedMessage("meditation: %d", signal));
                break;
            case BLINK:
                tvBlink.setText(getFormattedMessage("blink: %d", signal));
                break;
        }
    }

    private String getFormattedMessage(String messageFormat, Signal signal) {
        return String.format(Locale.getDefault(), messageFormat, signal.getValue());
    }

    private void handleBrainWavesChange(final Set<BrainWave> brainWaves) {
        //long time = System.currentTimeMillis();
        //Log.d("att", ""+(time - neuroStart));
        //neuroStart = time;
        int i = 0;
        for (BrainWave brainWave : brainWaves) {
            Log.d(NEURO_TAG, String.format("%s: %d", brainWave.toString(), brainWave.getValue()));
            switch (i){
                case 0:
                    brain_wave_high_beta = brainWave.getValue();
                    break;
                case 1:
                    brain_wave_low_alpha = brainWave.getValue();
                    break;
                case 2:
                    brain_wave_low_beta = brainWave.getValue();
                    break;
                case 3:
                    brain_wave_delta = brainWave.getValue();
                    break;
                case 4:
                    brain_wave_middle_gamma = brainWave.getValue();
                    break;
                case 5:
                    brain_wave_high_alpha = brainWave.getValue();
                    break;
                case 6:
                    brain_wave_theta = brainWave.getValue();
                    break;
                case 7:
                    brain_wave_low_gamma = brainWave.getValue();
                    break;
            }
            i++;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (permissions.length == 1 && grantResults.length == 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceRecorder();
                } else {
                    showPermissionMessageDialog();
                }
                break;
            case BODY_SENSOR_REQUEST:
                break;
            case INTERNET_SENSOR_REQUEST:
                break;
            case REQUEST_ENABLE_BT:
                break;
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, yay!
                    initEmpaticaDeviceManager();
                } else {
                    // Permission denied, boo!
                    final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    new AlertDialog.Builder(this)
                            .setTitle("Permission required")
                            .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // try again
                                    if (needRationale) {
                                        // the "never ask again" flash is not set, try again with permission request
                                        initEmpaticaDeviceManager();
                                    } else {
                                        // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .setNegativeButton("Exit application", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // without permission exit is the only way
                                    finish();
                                }
                            })
                            .show();
                }
                default:
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private void initEmpaticaDeviceManager() {
        // Android 6 (API level 23) now require ACCESS_COARSE_LOCATION permission to use BLE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {

            if (TextUtils.isEmpty(EMPATICA_API_KEY)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Please insert your API KEY")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without permission exit is the only way
                                finish();
                            }
                        })
                        .show();
                return;
            }

            // Create a new EmpaDeviceManager. E4_communicator is both its data and status delegate.
            deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);

            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
        if (deviceManager != null) {
            deviceManager.stopScanning();
        }
        if (neuroSky != null ) {
            neuroSky.stopMonitoring();
        }
        }catch (NullPointerException e){}
        try{

            unregisterReceiver(gattUpdateReceiver);
        }catch (IllegalArgumentException e){

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceManager != null) {
            deviceManager.cleanUp();
        }
        unbindService(serviceConnection);
        bleService = null;
        DataHandler.getInstance().deleteObserver(this);
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        if (allowed) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice(bluetoothDevice);
                updateLabel(deviceNameLabel, "To: " + deviceName);
            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void didRequestEnableBluetooth() {
        // Request the user to enable Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void didUpdateSensorStatus(@EmpaSensorStatus int status, EmpaSensorType type) {

        didUpdateOnWristStatus(status);
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        // Update the UI
        updateLabel(statusLabel, status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            updateLabel(statusLabel, status.name() + " - Turn on your device");
            // Start scanning
            deviceManager.startScanning();
            // The device manager has established a connection


        } else if (status == EmpaStatus.CONNECTED) {

            // The device manager disconnected from a device
        } else if (status == EmpaStatus.DISCONNECTED) {

            updateLabel(deviceNameLabel, "");

        }
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        updateLabel(accel_xLabel, "" + x);
        updateLabel(accel_yLabel, "" + y);
        updateLabel(accel_zLabel, "" + z);

        e4_accel_x = x;
        e4_accel_y = y;
        e4_accel_z = z;
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        updateLabel(bvpLabel, "" + bvp);
        e4_bvp = bvp;
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        //updateLabel(batteryLabel, String.format("%.0f %%", battery * 100));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        updateLabel(edaLabel, "" + gsr);
        e4_gsr = gsr;
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        updateLabel(ibiLabel, "" + ibi);
        e4_ibi = ibi;
        //heart_rate = 60/ibi;
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        //updateLabel(temperatureLabel, "" + temp);
        e4_skin_temp = temp;
    }

    // Update a label with some text, making sure this is run in the UI thread
    private void updateLabel(final TextView label, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.setText(text);
            }
        });
    }

    @Override
    public void didReceiveTag(double timestamp) {

    }

    @Override
    public void didEstablishConnection() {

        show();
    }

    @Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (status == EmpaSensorStatus.ON_WRIST) {

                    //((TextView) findViewById(R.id.wrist_status_label)).setText("ON WRIST");
                }
                else {

                    //((TextView) findViewById(R.id.wrist_status_label)).setText("NOT ON WRIST");
                }
            }
        });
    }

    void show() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dataCnt.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dataCnt.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //TODO: show toast
                finish();
            } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                final BleSensor<?> sensor = BleSensors.getSensor(serviceUuid);
                bleService.enableSensor(sensor, true);
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                final BleSensor<?> sensor = BleSensors.getSensor(serviceUuid);
                final String text = intent.getStringExtra(BleService.EXTRA_TEXT);
                onDataRecieved(sensor, text);
            }
        }
    };
    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(HR_TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(EXTRAS_DEVICE_ADDRESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
            //TODO: show toast
            finish();
        }
    };
    public void onDataRecieved(BleSensor<?> sensor, String text) {
        if (sensor instanceof BleHeartRateSensor) {
            final BleHeartRateSensor heartSensor = (BleHeartRateSensor) sensor;
            int[] values = heartSensor.getData();

            viewHR.setText(text);
        }
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                long eventTime = System.currentTimeMillis();
//                Log.d("IMU", ""+ (eventTime - startTime));
                startTime = eventTime;
                if (socketConnectionFlag) {
                    socket.emit(portPath,
                            Integer.toString(event.sensor.getType()),
                            event.timestamp,
                            Float.toString(event.values[0]),
                            Float.toString(event.values[1]),
                            Float.toString(event.values[2]));
                }
                accX.setText(""+(int)event.values[0]);
                accY.setText(""+(int)event.values[1]);
                accZ.setText(""+(int)event.values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (socketConnectionFlag) {
                    socket.emit(portPath,
                            Integer.toString(event.sensor.getType()),
                            event.timestamp,
                            Float.toString(event.values[0]),
                            Float.toString(event.values[1]),
                            Float.toString(event.values[2]));
                }
                gyroX.setText(""+(int)event.values[0]);
                gyroY.setText(""+(int)event.values[1]);
                gyroZ.setText(""+(int)event.values[2]);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    public void unregistListener(){
        manager.unregisterListener( this,sensorAcc);
        manager.unregisterListener(this,sensorGyro);
        sensorDataSender.cancel();
    }

    private void registListener(){
        //20ms
        manager.registerListener(this,sensorAcc, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this,sensorGyro, SensorManager.SENSOR_DELAY_GAME);
    }

}