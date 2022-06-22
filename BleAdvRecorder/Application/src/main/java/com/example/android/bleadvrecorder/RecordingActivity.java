/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bleadvrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * For given BLE devices, this Activity provides the user interface to record advertise and
 * display data statistics,
 * The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class RecordingActivity extends Activity {
    private final static String TAG = RecordingActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mSessionName;
    private String mRecFileName;
    private Uri mRecFileNameUri;
    private FileOutputStream mFileOutputStream;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private LeScanService mLeScanService;
    private Handler mHandler;
//    private ExpandableListView mGattServicesList;
//    private BluetoothLeService mBluetoothLeService;
//    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
//            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
//    private boolean mConnected = false;
//    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mLeScanService = ((LeScanService.ThisBinder) service).getService();
            if (!mLeScanService.initialize()) {
                //Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mLeScanService = null;
        }
    };

//    // Code to manage Service lifecycle.
//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//            if (!mBluetoothLeService.initialize()) {
//                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
//            }
//            // Automatically connects to the device upon successful start-up initialization.
//            mBluetoothLeService.connect(mDeviceAddress);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            mBluetoothLeService = null;
//        }
//    };

//    // Handles various events fired by the Service.
//    // ACTION_GATT_CONNECTED: connected to a GATT server.
//    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
//    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
//    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
//    //                        or notification operations.
//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
//            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
//            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//            }
//        }
//    };

//    // If a given GATT characteristic is selected, check for supported features.  This sample
//    // demonstrates 'Read' and 'Notify' features.  See
//    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
//    // list of supported characteristic features.
//    private final ExpandableListView.OnChildClickListener servicesListClickListner =
//            new ExpandableListView.OnChildClickListener() {
//                @Override
//                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
//                                            int childPosition, long id) {
//                    if (mGattCharacteristics != null) {
//                        final BluetoothGattCharacteristic characteristic =
//                                mGattCharacteristics.get(groupPosition).get(childPosition);
//                        final int charaProp = characteristic.getProperties();
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // If there is an active notification on a characteristic, clear
//                            // it first so it doesn't update the data field on the user interface.
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            mBluetoothLeService.readCharacteristic(characteristic);
//                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            mNotifyCharacteristic = characteristic;
//                            mBluetoothLeService.setCharacteristicNotification(
//                                    characteristic, true);
//                        }
//                        return true;
//                    }
//                    return false;
//                }
//    };

//    private void clearUI() {
//        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
//        mDataField.setText(R.string.no_data);
//    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.gatt_services_characteristics);
//
//        final Intent intent = getIntent();
//        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
//        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
//
//        // Sets up UI references.
//        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        mConnectionState = (TextView) findViewById(R.id.connection_state);
//        mDataField = (TextView) findViewById(R.id.data_value);
//
//        getActionBar().setTitle(mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adv_recording);
        mHandler = new Handler();

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.rec_device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        mConnectionState = (TextView) findViewById(R.id.connection_state);
//        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Bind LE Scan Service
        Intent leScanServiceIntent = new Intent(this, LeScanService.class);
        getApplicationContext().bindService(leScanServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Pop up asking the user to enter a session name
        final EditText sessionName = new EditText(this);
        sessionName.setHint("IndoorMeetingRoomChur");
        new AlertDialog.Builder(this)
                .setTitle("Session name")
                .setMessage("Enter the session name")
                .setView(sessionName)
                .setPositiveButton("Start session", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mSessionName = sessionName.getText().toString();
                        startRecSession(mSessionName);
                    }
                })
                .show();
    }
    @Override
    protected void onResume() {
        super.onResume();
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService != null) {
//            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//            Log.d(TAG, "Connect request result=" + result);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeFile();
//        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
    }

    private String getTimestampAsString() {
        long time = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        return DateFormat.format("yyyy-MM-dd-hh-mm-ss", cal).toString();
    }

    private void startRecSession(String sessionName) {
        // Create and open log file
        String recFileName = getTimestampAsString() + "-" + sessionName;
        //File recFile = new File(getExternalFilesDir(null), recFileName);
        createFile(sessionName);

    }

    // Request code for creating the file where ADV will be recorded
    private static final int CREATE_REC_FILE = 1;

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == CREATE_REC_FILE
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            mRecFileNameUri = null;
            if (resultData != null) {
                mRecFileNameUri = resultData.getData();
                // Perform operations on the document using its URI.

                try {
                    // Open file or editing
                    mParcelFileDescriptor = getApplicationContext().getContentResolver().
                            openFileDescriptor(mRecFileNameUri, "w");
                    mFileOutputStream = new FileOutputStream(mParcelFileDescriptor.getFileDescriptor());
                    writeToFile("Session started at " + System.currentTimeMillis() + "\n");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Start LE scan
                scanLeDevice(true);
            }
        }
    }

    private void createFile(String sessionName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/txt");
        mRecFileName = getTimestampAsString() + "-" + sessionName + ".txt";
        intent.putExtra(Intent.EXTRA_TITLE, mRecFileName);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_REC_FILE);

    }

    private void writeToFile(String str) {
        try {
            mFileOutputStream.write(str.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void closeFile() {
        try {
            // Let the document provider know you're done by closing the stream.
            mFileOutputStream.close();
            mParcelFileDescriptor.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (mLeScanService != null) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mLeScanService.stopScan(mLeScanCallback);
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        invalidateOptionsMenu();
                    }
                }, 10000);
                Log.d(TAG, "scanLeDevice() -> startScan");
                mLeScanService.startScan(mLeScanCallback);
            }
        } else {
            if (mLeScanService != null) {
                Log.d(TAG, "scanLeDevice() -> stopScan");
                mLeScanService.stopScan(mLeScanCallback);
            }
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                String str = device.getAddress() + " " + String.valueOf(rssi) + "dBm\n";
                                writeToFile(str);
//                            mLeDeviceListAdapter.addDevice(device, String.valueOf(rssi));
//                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if ((mLeScanService != null) && mLeScanService.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                //mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch(item.getItemId()) {
//            case R.id.menu_connect:
//                mBluetoothLeService.connect(mDeviceAddress);
//                return true;
//            case R.id.menu_disconnect:
//                mBluetoothLeService.disconnect();
//                return true;
//            case android.R.id.home:
//                onBackPressed();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void updateConnectionState(final int resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mConnectionState.setText(resourceId);
//            }
//        });
//    }
//
//    private void displayData(String data) {
//        if (data != null) {
//            mDataField.setText(data);
//        }
//    }
//
//    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
//    // In this sample, we populate the data structure that is bound to the ExpandableListView
//    // on the UI.
//    private void displayGattServices(List<BluetoothGattService> gattServices) {
//        if (gattServices == null) return;
//        String uuid = null;
//        String unknownServiceString = getResources().getString(R.string.unknown_service);
//        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
//        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
//                = new ArrayList<ArrayList<HashMap<String, String>>>();
//        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
//
//        // Loops through available GATT Services.
//        for (BluetoothGattService gattService : gattServices) {
//            HashMap<String, String> currentServiceData = new HashMap<String, String>();
//            uuid = gattService.getUuid().toString();
//            currentServiceData.put(
//                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
//            currentServiceData.put(LIST_UUID, uuid);
//            gattServiceData.add(currentServiceData);
//
//            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
//                    new ArrayList<HashMap<String, String>>();
//            List<BluetoothGattCharacteristic> gattCharacteristics =
//                    gattService.getCharacteristics();
//            ArrayList<BluetoothGattCharacteristic> charas =
//                    new ArrayList<BluetoothGattCharacteristic>();
//
//            // Loops through available Characteristics.
//            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                charas.add(gattCharacteristic);
//                HashMap<String, String> currentCharaData = new HashMap<String, String>();
//                uuid = gattCharacteristic.getUuid().toString();
//                currentCharaData.put(
//                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
//                currentCharaData.put(LIST_UUID, uuid);
//                gattCharacteristicGroupData.add(currentCharaData);
//            }
//            mGattCharacteristics.add(charas);
//            gattCharacteristicData.add(gattCharacteristicGroupData);
//        }
//
//        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
//                this,
//                gattServiceData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[] {LIST_NAME, LIST_UUID},
//                new int[] { android.R.id.text1, android.R.id.text2 },
//                gattCharacteristicData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[] {LIST_NAME, LIST_UUID},
//                new int[] { android.R.id.text1, android.R.id.text2 }
//        );
//        mGattServicesList.setAdapter(gattServiceAdapter);
//    }

//    private static IntentFilter makeGattUpdateIntentFilter() {
//        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        return intentFilter;
//    }
}
