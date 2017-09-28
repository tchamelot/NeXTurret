/*
 * Copyright (c) 2010 Jacek Fedorynski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is derived from:
 * 
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChat.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package org.tchamelot.NeXTRemote;

/*
 * TODO:
 * 
 * tilt controls
 */

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class NXTRemoteControl extends Activity implements SensorEventListener{
    
    private boolean NO_BT = false; 
    
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_SETTINGS = 3;
    
    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;
    
    public static final String TOAST = "toast";
    
    private static final int MODE_BUTTONS = 1;
    private static final int MODE_TEST = 5;
    
    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private NXTTalker mNXTTalker;
    
    private int mState = NXTTalker.STATE_NONE;
    private int mSavedState = NXTTalker.STATE_NONE;
    private boolean mNewLaunch = true;
    private String mDeviceAddress = null;
    private TextView mStateDisplay;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private Menu mMenu;

    private TextView mName;
    private TextView mAccel_view = null;
    private SensorManager mSManager;
    private Sensor mAccelerometer;
    
    private int mPower = 80;
    private int mControlsMode = MODE_BUTTONS;

    private long last_call_sensor_listener = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i("NXT", "NXTRemoteControl.onCreate()");
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        
        if (savedInstanceState != null) {
            mNewLaunch = false;
            mDeviceAddress = savedInstanceState.getString("device_address");
            if (mDeviceAddress != null) {
                mSavedState = NXTTalker.STATE_CONNECTED;
            }
            
            if (savedInstanceState.containsKey("power")) {
                mPower = savedInstanceState.getInt("power");
            }
            if (savedInstanceState.containsKey("controls_mode")) {
                mControlsMode = savedInstanceState.getInt("controls_mode");
            }
        }
        
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "NXT Remote Control");
        
        if (!NO_BT) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        
        setupUI();
        
        mNXTTalker = new NXTTalker(mHandler);

        mSManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String text;
        byte[] cmd = new byte[3];
        float x, y, z;

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            if(System.currentTimeMillis() - last_call_sensor_listener >= 50) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                text = "X : " + x + "\nY : " + y + "\nZ : " + z;

                //if(mAccel_view != null && mControlsMode == MODE_TEST)
                {
                    //mAccel_view.setText(text);
                    cmd[2] = 1;
                    cmd[0] = (byte)(-x/9.81 * 100);
                    cmd[1] = (byte)(y/9.81 * 100);

                    mNXTTalker.send_mailbox(cmd, 0);
                    last_call_sensor_listener = System.currentTimeMillis();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class TurretButtonOnTouchListener implements OnTouchListener{

        private int mmDirection;
        private final byte[] mCmd = new byte[3];

        public TurretButtonOnTouchListener(int direction) {
            mmDirection = direction;

            mCmd[0] = 1;    //Horizontal position
            mCmd[1] = 1;    //Vertical position
            mCmd[2] = 1;    //No shoot

        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            if(action == MotionEvent.ACTION_DOWN) {
                mCmd[2] = 1;

                switch (mmDirection) {
                    case 0: //UP
                        mCmd[1] += 2;
                        if(mCmd[1] >= 100)
                            mCmd[1] = 100;
                        break;
                    case 1: //DOWN
                        mCmd[1] -= 2;
                        if(mCmd[1] <= -100)
                            mCmd[1] = -100;
                        break;
                    case 2: //RIGHT
                        mCmd[0] += 2;
                        if(mCmd[0] >= 100)
                            mCmd[0] = 100;
                        break;
                    case 3: //LEFT
                        mCmd[0] -= 2;
                        if(mCmd[0] <= -100)
                            mCmd[0] = -100;
                        break;
                    case 4: //Shoot
                        mCmd[2] = 2;
                    default:
                        //Do nothing
                }

                if(mCmd[0] == 0)
                    mCmd[0] = 1;
                if(mCmd[1] == 0)
                    mCmd[1] = 1;

                mNXTTalker.send_mailbox(mCmd, 0);
            }


            return true;
        }
    }

    private void updateMenu(int disabled) {
        if (mMenu != null) {
            mMenu.findItem(R.id.menuitem_buttons).setEnabled(disabled != R.id.menuitem_buttons).setVisible(disabled != R.id.menuitem_buttons);
            mMenu.findItem(R.id.menuitem_test).setEnabled(disabled != R.id.menuitem_test).setVisible(disabled != R.id.menuitem_test);
        }
    }
    
    private void setupUI() {
        if (mControlsMode == MODE_BUTTONS) {
            setContentView(R.layout.main);

            updateMenu(R.id.menuitem_buttons);
            
            ImageButton buttonUp = (ImageButton) findViewById(R.id.button_up);
            buttonUp.setOnTouchListener(new TurretButtonOnTouchListener(0));
            ImageButton buttonLeft = (ImageButton) findViewById(R.id.button_left);
            buttonLeft.setOnTouchListener(new TurretButtonOnTouchListener(3));

            ImageButton buttonDown = (ImageButton) findViewById(R.id.button_down);
            buttonDown.setOnTouchListener(new TurretButtonOnTouchListener(1));

            ImageButton buttonRight = (ImageButton) findViewById(R.id.button_right);
            buttonRight.setOnTouchListener(new TurretButtonOnTouchListener(2));

            Button shoot = (Button) findViewById(R.id.shoot);
            shoot.setOnTouchListener(new TurretButtonOnTouchListener(4));

            mAccel_view = null;

        } else if(mControlsMode == MODE_TEST) {
            setContentView(R.layout.main_test);

            updateMenu(R.id.menuitem_test);

            mName = (TextView) findViewById(R.id.name_textView);

            Button name_button = (Button) findViewById(R.id.name_button);
            name_button.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View v) {
                   //String name;
                   byte[] test = {0x43, 0x6F, 0x75, 0x63, 0x6F, 0x75, 0x00};

                   //name = mNXTTalker.get_name();

                   //mName.setText(name);
                   mNXTTalker.send_mailbox(test, 0);
               }
           });

            mAccel_view = (TextView) findViewById(R.id.accel_view);

        }
        
        mStateDisplay = (TextView) findViewById(R.id.state_display);

        mConnectButton = (Button) findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NO_BT) {
                    findBrick();
                } else {
                    mState = NXTTalker.STATE_CONNECTED;
                    displayState();
                }
            }
        });
        
        mDisconnectButton = (Button) findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mNXTTalker.stop();
            }
        });

        displayState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i("NXT", "NXTRemoteControl.onStart()");
        if (!NO_BT) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                if (mSavedState == NXTTalker.STATE_CONNECTED) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mNXTTalker.connect(device);
                } else {
                    if (mNewLaunch) {
                        mNewLaunch = false;
                        findBrick();
                    }
                }
            }
        }
    }

    private void findBrick() {
        Intent intent = new Intent(this, ChooseDeviceActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }
  
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                findBrick();
            } else {
                Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
                finish();
            }
            break;
        case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                //Toast.makeText(this, address, Toast.LENGTH_LONG).show();
                mDeviceAddress = address;
                mNXTTalker.connect(device);
            }
            break;
        case REQUEST_SETTINGS:
            //XXX?
            break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.i("NXT", "NXTRemoteControl.onSaveInstanceState()");
        if (mState == NXTTalker.STATE_CONNECTED) {
            outState.putString("device_address", mDeviceAddress);
        }
        //outState.putBoolean("reverse", mReverse);
        outState.putInt("power", mPower);
        outState.putInt("controls_mode", mControlsMode);
    }
    
    private void displayState() {
        String stateText = null;
        int color = 0;
        switch (mState){ 
        case NXTTalker.STATE_NONE:
            stateText = "Not connected";
            color = 0xffff0000;
            mConnectButton.setVisibility(View.VISIBLE);
            mDisconnectButton.setVisibility(View.GONE);
            setProgressBarIndeterminateVisibility(false);
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            break;
        case NXTTalker.STATE_CONNECTING:
            stateText = "Connecting...";
            color = 0xffffff00;
            mConnectButton.setVisibility(View.GONE);
            mDisconnectButton.setVisibility(View.GONE);
            setProgressBarIndeterminateVisibility(true);
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            break;
        case NXTTalker.STATE_CONNECTED:
            stateText = "Connected";
            color = 0xff00ff00;
            mConnectButton.setVisibility(View.GONE);
            mDisconnectButton.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(false);
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
            break;
        }
        mStateDisplay.setText(stateText);
        mStateDisplay.setTextColor(color);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_STATE_CHANGE:
                mState = msg.arg1;
                displayState();
                break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        //Log.i("NXT", "NXTRemoteControl.onStop()");
        mSavedState = mState;
        mNXTTalker.stop();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menuitem_buttons:
            mControlsMode = MODE_BUTTONS;
            setupUI();
            break;
        case R.id.menuitem_test:
            mControlsMode = MODE_TEST;
            setupUI();
            break;

        default:
            return false;    
        }
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        mSManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSManager.unregisterListener(this, mAccelerometer);
    }
}
