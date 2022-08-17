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
import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * For given BLE devices, this Activity provides the user interface to record advertise and
 * display data statistics.
 * The Activity communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class RecordingActivity extends ListActivity {
    private final static String TAG = RecordingActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private ArrayList<String> mDevicesAddress;
    private String mSessionName;
    private String mRecFileName;
    private Uri mRecFileNameUri;
    private FileOutputStream mFileOutputStream;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private LeScanService mLeScanService;
    private Handler mHandler;
    private ScanCallback mFilteredScanCallback;
    private long mTimeOfLastAdvReceived;
    private long mNofReceivedAdv;
    private ScanResultAdapter mScanResultAdapter;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adv_recording);
        mHandler = new Handler();

        // Get the devices address as provided by DeviceScanActivity
        final Intent intent = getIntent();
        mDevicesAddress =
                (ArrayList<String>) intent.getSerializableExtra("devicesAddress");

        getActionBar().setTitle("Recording ADV");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Bind LE Scan Service
        Intent leScanServiceIntent = new Intent(this, LeScanService.class);
        getApplicationContext().bindService(leScanServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Initialize list view adapter
        mScanResultAdapter = new ScanResultAdapter(getApplicationContext(), getLayoutInflater());
        setListAdapter(mScanResultAdapter);

        // Pop up asking the user to enter a session name
        final EditText sessionName = new EditText(this);
        sessionName.setHint("IndoorMeetingRoomChur");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Session name");
        builder.setMessage("Enter the session name");
        builder.setView(sessionName);
        builder.setPositiveButton("Start session", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mSessionName = sessionName.getText().toString();
                startRecSession(mSessionName);
            }
        });
        builder.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeFile();
//        mBluetoothLeService = null;
    }

    private String getTimestampAsString() {
        long time = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        return DateFormat.format("yyyy-MM-dd-HH-mm-ss", cal).toString();
    }

    private void startRecSession(String sessionName) {
        // Create and open log file
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
                    // Open file for editing
                    mParcelFileDescriptor = getApplicationContext().getContentResolver().
                            openFileDescriptor(mRecFileNameUri, "w");
                    mFileOutputStream = new FileOutputStream(mParcelFileDescriptor.getFileDescriptor());
                    writeToFile("Session started at " + getTimestampAsString() + "\n\n");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // Start LE scan
                scanAndRecord(true);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeFile() {
        try {
            // Let the document provider know you're done by closing the stream.
            mFileOutputStream.close();
            mParcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scanAndRecord(final boolean enable) {
        if (enable) {
            if (mLeScanService != null) {
                if (mFilteredScanCallback == null) {
                    Log.d(TAG, "scanAndRecord() -> startScan");
                    mNofReceivedAdv = 0;
                    mTimeOfLastAdvReceived = 0;
                    mFilteredScanCallback = new CustomScanCallback();
                    mLeScanService.startFilteredScan(mDevicesAddress, mFilteredScanCallback);
                }
            }
        } else {
            if (mLeScanService != null) {
                Log.d(TAG, "scanAndRecord() -> stopScan");
                mLeScanService.stopFilteredScan(mFilteredScanCallback);
                mFilteredScanCallback = null;
            }
        }
        invalidateOptionsMenu();
    }

     // ScanCallback for filtered scan
    private class CustomScanCallback extends ScanCallback {

        // Convert a byte array into a hex-string
         private String toHexadecimal(byte[] digest) {
             String hash = "";
             for(byte aux : digest) {
                 int b = aux & 0xff;
                 if (Integer.toHexString(b).length() == 1) hash += "0";
                 hash += Integer.toHexString(b);
             }
             return hash;
         }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults");

            // This one seems to be never called

            for (ScanResult result : results) {
                //mAdapter.add(result);
            }
            //mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult");
            BluetoothDevice device = result.getDevice();

            String rssiStr = String.valueOf(result.getRssi());
            byte[] advData = result.getScanRecord().getBytes();
            String advDataHexStr = toHexadecimal(advData);

            long time = System.currentTimeMillis();
//            ScanRecord scanRec = result.getScanRecord();
//            byte data[] = scanRec.getBytes();
            String str = device.getAddress() + ";" + String.valueOf(time) +
                    ";" + advDataHexStr + ";" + rssiStr + "\n";
            writeToFile(str);

            // Adapter takes care of updating the UI - among other things
            mScanResultAdapter.add(result);
            mScanResultAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed");
//            Toast.makeText(getActivity(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
//                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_scan).setVisible(false); // Do not display SCAN during this activity
        if ((mLeScanService != null) && mLeScanService.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_record).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_record).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_record:
                scanAndRecord(true);
                break;
            case R.id.menu_stop:
                scanAndRecord(false);
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return true;
    }
}
