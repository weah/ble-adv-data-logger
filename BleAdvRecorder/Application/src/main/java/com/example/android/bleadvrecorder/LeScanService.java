package com.example.android.bleadvrecorder;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LeScanService extends Service {
    private final static String TAG = LeScanService.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean isScanning;


    public class ThisBinder extends Binder {
        LeScanService getService() {
            return LeScanService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new ThisBinder();


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean initialize() {
        isScanning = false;
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

//    /**
//     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
//     */
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private List<ScanFilter> buildScanFilters() {
//        List<ScanFilter> scanFilters = new ArrayList<>();
//
//        ScanFilter.Builder builder = new ScanFilter.Builder();
//        // Comment out the below line to see all BLE devices around you
//        builder.setServiceUuid(Constants.Service_UUID);
//        //builder.setDeviceAddress();
//        scanFilters.add(builder.build());
//
//        return scanFilters;
//    }
//
//    /**
//     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
//     */
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private ScanSettings buildScanSettings() {
//        ScanSettings.Builder builder = new ScanSettings.Builder();
//        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
//        return builder.build();
//    }

    public void startScan(BluetoothAdapter.LeScanCallback scanCallback) {
        mBluetoothAdapter.startLeScan(scanCallback);
        isScanning = true;
    }

//    public void startFilteredScan(ScanCallback scanCallback) {
//        mBluetoothLeScanner.startScan(scanCallback);
//        isScanning = true;
//    }

    public void stopScan(BluetoothAdapter.LeScanCallback scanCallback) {
        mBluetoothAdapter.stopLeScan(scanCallback);
        isScanning = false;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public boolean isBtEnabled() {
        return mBluetoothAdapter.isEnabled();
    }
}
