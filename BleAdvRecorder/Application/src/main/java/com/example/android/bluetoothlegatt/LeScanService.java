package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class LeScanService extends Service {
    private final static String TAG = LeScanService.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
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


    public boolean initialize() {
        isScanning = false;
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }


    public void startScan(BluetoothAdapter.LeScanCallback scanCallback) {
        mBluetoothAdapter.startLeScan(scanCallback);
        isScanning = true;
    }

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
