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
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChatService.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package org.tchamelot.NeXTRemote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NXTTalker {

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    
    private int mState;
    private Handler mHandler;
    private BluetoothAdapter mAdapter;

    //Bluetooth atribute
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;
    
    private ConnectThread mConnectThread;
    //private ConnectedThread mConnectedThread;
    
    public NXTTalker(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        mState = state;
        if (mHandler != null) {
            mHandler.obtainMessage(NXTRemoteControl.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        } else {
            //XXX
        }
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    public synchronized void setHandler(Handler handler) {
        mHandler = handler;
    }
    
    private void toast(String text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(NXTRemoteControl.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(NXTRemoteControl.TOAST, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } else {
            //XXX
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        //Log.i("NXT", "NXTTalker.connect()");
        
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if(mState == STATE_CONNECTED)
            cancel_connection();

        
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        //Closing connection thread
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Check if NXT is not already connected
        if(mState == STATE_CONNECTED)
            cancel_connection();

        //Setting up the connection
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;

        
        /*if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();*/
        
        //toast("Connected to " + device.getName());
        
        setState(STATE_CONNECTED);
    }
    
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mState == STATE_CONNECTED)
            cancel_connection();

        setState(STATE_NONE);
    }
    
    private void connectionFailed() {
        setState(STATE_NONE);
        //toast("Connection failed");
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int offset, int length) {
        char[] hexChars = new char[length * 2];
        for ( int j = offset; j < length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String get_name() {
        byte[] data = {0x02, 0x00, 0x01, (byte) 0x9B};
        //byte[] data = {0x00, 0x03, 0x01, (byte) 0xB8, 0x03, (byte) 0xE8};
        byte[] answer;
        //byte[] raw_name;
        String name;

        write(data);

        answer = read(35);

        //name = bytesToHex(answer, 5, 14);
        //raw_name = Arrays.copyOfRange(answer, 5, 17);
        name = new String(Arrays.copyOfRange(answer, 5, 17));

        return name;
    }
    
    public void write(byte[] out) {
        //ConnectedThread r;

        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            //r = mConnectedThread;
        }

        //toast(bytesToHex(out, 0, out.length));

        try {
            mOutStream.write(out);
        } catch (IOException e) {
            e.printStackTrace();
            // XXX?
        }
        //r.write(out);
    }

    public byte[] read(int size) {
        //ConnectedThread r;

        byte[] buffer = new byte[size];
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return null;
            }
            //r = mConnectedThread;
        }


        try {
            mInStream.read(buffer, 0, size);
        } catch(IOException e) {
            e.printStackTrace();
            // XXX?
        }
        return buffer;
    }

    public void send_mailbox(byte[] data, int num) {
        int length = data.length;
        byte[] header = {0x00, 0x00, (byte)0x80, 0x09, 0x00, 0x00};
        byte[] ender = {0};
        byte[] cmd = new byte[length + 7];

        header[0] =  (byte)((length + 5) & 0xFF);
        header[1] =  (byte)(((length + 5) >> 8) & 0xFF);

        header[4] = (byte)num;
        header[5] = (byte)(length + 1);

        System.arraycopy(header, 0, cmd, 0, header.length);
        System.arraycopy(data, 0, cmd, header.length, data.length);
        System.arraycopy(ender, 0, cmd, header.length + data.length, ender.length);

        write(cmd);
    }

    public void cancel_connection() {
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSocket = null;
        mInStream = null;
        mOutStream = null;
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }

        public void run() {
            setName("ConnectThread");
            mAdapter.cancelDiscovery();

            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    // This is a workaround that reportedly helps on some older devices like HTC Desire, where using
                    // the standard createRfcommSocketToServiceRecord() method always causes connect() to fail.
                    Method method = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                    mmSocket = (BluetoothSocket) method.invoke(mmDevice, Integer.valueOf(1));
                    mmSocket.connect();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    connectionFailed();
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    return;
                }
            }

            synchronized (NXTTalker.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
