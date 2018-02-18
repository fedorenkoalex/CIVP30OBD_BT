package com.obdhondascan.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.obdhondascan.Constants;
import com.obdhondascan.R;
import com.obdhondascan.activities.DeviceDashboardActivity;
import com.obdhondascan.activities.DeviceScanActivity;
import com.obdhondascan.model.OBDDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import io.reactivex.Observable;

/**
 * Created by alexfedorenko on 03.02.2018.
 */

public class OBDBTManager {


    private static final UUID UUID_VALUE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static OBDBTManager mInstance;

    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mBtSocket = null;
    private ConnectedThread mConnectedThread;

    private IReadValue mReadListener;
    private IConnect mConnectionListener;

    private OBDDevice mConnectedDevice = null;

    public static OBDBTManager getInstance() {
        if (mInstance == null) {
            mInstance = new OBDBTManager();
        }
        return mInstance;
    }

    public OBDBTManager() {
        this.mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public void setConnectionListener(IConnect listener) {
        this.mConnectionListener = listener;
    }

    public void setReadListener(IReadValue readListener) {
        Log.d("setReadListener", "" + (readListener != null));
        this.mReadListener = readListener;
    }

    public boolean isConnected() {
        return mConnectedDevice != null;
    }

    public boolean updateDevice(String data) {
        if (mConnectedDevice != null) {
            mConnectedDevice.setResponse(data);
            float injCC = new PreferencesHelper().getFloat(Constants.INJ_CC, 240.0f);
            FuelUtil.calculateFuel(mConnectedDevice, injCC);
            /*try {
                PowerUtil.calculate(mConnectedDevice);
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            return true;
        }
        return false;
    }

    public OBDDevice getConnectedDevice() {
        return mConnectedDevice;
    }

    public List<BluetoothDevice> getAllDevices() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        List<BluetoothDevice> result = new ArrayList<>();
        result.addAll(pairedDevices);
        return result;
    }

    public BluetoothDevice getDeviceToConnect() {
        String targetAddress = new PreferencesHelper().getString(Constants.DEVICE_ADDRESS, "00:21:13:00:C8:94");
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device != null && device.getAddress().equals(targetAddress)) {
                return device;
            }
        }
        return null;
    }

    public Observable<Boolean> write(byte[] value) {
        Observable<Boolean> writeObservable = Observable.fromCallable(() -> {
            if (mConnectedThread != null) {
                boolean result = mConnectedThread.write(value);
                return result;
            } else {
                return false;
            }
        });
        return writeObservable;

    }

    public Observable<Boolean> connect(BluetoothDevice device) {
        Observable<Boolean> connectObservable = Observable.fromCallable(() -> {
            try {
                mBtSocket = device.createRfcommSocketToServiceRecord(UUID_VALUE);
            } catch (IOException e) {
                e.printStackTrace();
                mConnectedDevice = null;
                return false;
            }
            // Establish the Bluetooth socket connection.
            try {
                mBtSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mBtSocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                mConnectedDevice = null;
                return false;
            }
            mConnectedThread = new ConnectedThread(mBtSocket);
            mConnectedThread.start();
            mConnectedThread.write(new byte[]{0});
            mConnectedDevice = new OBDDevice("" + device.getName(), device.getAddress());
            return true;
        });

        return connectObservable;
    }


    public boolean checkBTState() {
        if (mBtAdapter == null) {
            return false;
        } else {
            if (!mBtAdapter.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    public interface IReadValue {
        void receivedData(String data);
    }

    public interface IConnect {
        void onDisconnected();
    }


    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (mConnectionListener != null) {
                    mConnectionListener.onDisconnected();
                }
                mConnectedDevice = null;
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        @Override
        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            StringBuilder result = new StringBuilder();
            // Keep looping to listen for received messages
            while (true) {

                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    if (readMessage.contains("}")) {
                        result.append(readMessage);
                        if (mReadListener != null) {
                            mReadListener.receivedData(result.toString());
                        }
                        result = new StringBuilder();
                    } else {
                        result.append(readMessage);
                    }
                } catch (IOException e) {
                    if (mConnectionListener != null) {
                        mConnectionListener.onDisconnected();
                    }
                    mConnectedDevice = null;
                    break;
                }
            }
        }

        //write method
        public boolean write(byte[] input) {
            try {
                mmOutStream.write(input);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                e.printStackTrace();
                mConnectedDevice = null;
                return false;
            }
            return true;
        }
    }
}
