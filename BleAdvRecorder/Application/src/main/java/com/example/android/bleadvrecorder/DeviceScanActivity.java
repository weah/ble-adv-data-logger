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
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.manuelpeinado.multichoiceadapter.MultiChoiceBaseAdapter;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private static final String TAG = DeviceScanActivity.class.getSimpleName();


    private LeDeviceListAdapter mLeDeviceListAdapter;
    private LeScanService mLeScanService;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mLeScanService = ((LeScanService.ThisBinder) service).getService();
            if (!mLeScanService.initialize()) {
                //Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            scanLeDevice(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mLeScanService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        Intent leScanServiceIntent = new Intent(this, LeScanService.class);
        getApplicationContext().bindService(leScanServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(savedInstanceState);
        setListAdapter(mLeDeviceListAdapter);
        //mLeDeviceListAdapter.setOnItemClickListener(itemClickListener);
//        mLeDeviceListAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//                Log.d(TAG, "onListItemClick() id = " + String.valueOf(id));
//            }
//        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_record:
                startRecActivity();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mLeScanService != null) {
            if (!mLeScanService.isBtEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        //scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "onListItemClick() id = " + String.valueOf(id));
        mLeDeviceListAdapter.setAdapterView(l);
        if (!mLeDeviceListAdapter.isChecked(position)) {
            mLeDeviceListAdapter.setItemChecked(position, true);
        } else {
            mLeDeviceListAdapter.setItemChecked(position, false);
        }
        //l.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        //l.setItemsCanFocus(false);
        //l.setSelector(R.color.design_default_color_primary_dark);
//
//        if (l.isItemChecked(position)) {
//            v.setSelected(false);
//            mLeDeviceListAdapter.selectedDevice(position, false);
//        } else {
//            //l.setItemChecked(position, true);
//            v.setSelected(true);
//            mLeDeviceListAdapter.selectedDevice(position, true);
//        }
        Log.d(TAG, "onListItemClick() # of selected devices = "
                + String.valueOf(mLeDeviceListAdapter.getCheckedItemCount()));

//        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
//        if (device == null) return;
//
//        final Intent intent = new Intent(this, RecordingActivity.class);
//        intent.putExtra(RecordingActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(RecordingActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//        if (mScanning) {
//            mLeScanService.stopScan(mLeScanCallback);
//            mScanning = false;
//        }
//        startActivity(intent);
    }

    private void startRecActivity() {
        Log.d(TAG, "startRecActivity()");
        // Check that one or two devices are selected
        int nofSel = mLeDeviceListAdapter.getNofSelectedDevices();
        if (nofSel < 1 || nofSel > 2) {
            Toast.makeText(this, R.string.error_devices_selection, Toast.LENGTH_SHORT).show();
            return;
        }

        // Start activity


        final Intent intent = new Intent(this, RecordingActivity.class);
        ArrayList<String> devicesAddress = mLeDeviceListAdapter.getSelectedDeviceAddress();
        intent.putExtra("devicesAddress", devicesAddress);
//        intent.putExtra(RecordingActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(RecordingActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mLeScanService.stopScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (mLeScanService != null) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mLeScanService.stopScan(mLeScanCallback);
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                mLeScanService.startScan(mLeScanCallback);
            }
        } else {
            if (mLeScanService != null) {
                mScanning = false;
                mLeScanService.stopScan(mLeScanCallback);
            }
        }
        invalidateOptionsMenu();
    }

    public class LeDeviceListAdapter extends MultiChoiceBaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<BluetoothDevice> mSelectedLeDevices;
        private ArrayList<String> mLeDeviceRssi;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Bundle savedInstanceState) {
            super(savedInstanceState);
            mLeDevices = new ArrayList<BluetoothDevice>();
            mSelectedLeDevices = new ArrayList<BluetoothDevice>();
            mLeDeviceRssi = new ArrayList<String>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device, String rssi) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                mLeDeviceRssi.add(rssi);
            }
        }

        public void clear() {
            mLeDevices.clear();
            mSelectedLeDevices.clear();
        }
        public void selectedDevice(int position, boolean select) {
            if (select) {
                if (!mSelectedLeDevices.contains(mLeDevices.get(position))) {
                    mSelectedLeDevices.add(mLeDevices.get(position));
                }
            } else {
                if (mSelectedLeDevices.contains(mLeDevices.get(position))) {
                    mSelectedLeDevices.remove(mLeDevices.get(position));
                }
            }
        }

        public int getNofSelectedDevices() {
            return mSelectedLeDevices.size();
        }

        public ArrayList<String> getSelectedDeviceAddress() {
            ArrayList<String> addr = new ArrayList<>();
            for(int i=0; i<mSelectedLeDevices.size(); i++) {
                addr.add(mSelectedLeDevices.get(i).getAddress());
            }
            return addr;
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public ArrayList<BluetoothDevice> getDevices(ArrayList positions) {
            ArrayList<BluetoothDevice> d = null;
            for(int i=0; i<positions.size(); i++) {
                d.add(mLeDevices.get((Integer) positions.get(i)));
            }
            return d;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//            MenuInflater inflater = mode.getMenuInflater();
//            inflater.inflate(R.menu.my_action_mode, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//            if (item.getItemId() == R.id.menu_share) {
//                Toast.makeText(getContext(), "Share", Toast.LENGTH_SHORT).show();
//                return true;
//            }
            return false;
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        protected View getViewImpl(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            final String deviceRssi = mLeDeviceRssi.get(i);
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRssi.setText(deviceRssi + " dBm");

            return view;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

    }
//
//    // Adapter for holding devices found through scanning.
//    private class LeDeviceListAdapter extends BaseAdapter {
//        private ArrayList<BluetoothDevice> mLeDevices;
//        private ArrayList<BluetoothDevice> mSelectedLeDevices;
//        private ArrayList<String> mLeDeviceRssi;
//        private LayoutInflater mInflator;
//
//        public LeDeviceListAdapter() {
//            super();
//            mLeDevices = new ArrayList<BluetoothDevice>();
//            mSelectedLeDevices = new ArrayList<BluetoothDevice>();
//            mLeDeviceRssi = new ArrayList<String>();
//            mInflator = DeviceScanActivity.this.getLayoutInflater();
//        }
//
//        public void addDevice(BluetoothDevice device, String rssi) {
//            if(!mLeDevices.contains(device)) {
//                mLeDevices.add(device);
//                mLeDeviceRssi.add(rssi);
//            }
//        }
//
//        public void selectedDevice(int position, boolean select) {
//            if (select) {
//                if (!mSelectedLeDevices.contains(mLeDevices.get(position))) {
//                    mSelectedLeDevices.add(mLeDevices.get(position));
//                }
//            } else {
//                if (mSelectedLeDevices.contains(mLeDevices.get(position))) {
//                    mSelectedLeDevices.remove(mLeDevices.get(position));
//                }
//            }
//        }
//
//        public int getNofSelectedDevices() {
//            return mSelectedLeDevices.size();
//        }
//
//        public ArrayList<String> getSelectedDeviceAddress() {
//            ArrayList<String> addr = new ArrayList<>();
//            for(int i=0; i<mSelectedLeDevices.size(); i++) {
//                addr.add(mSelectedLeDevices.get(i).getAddress());
//            }
//            return addr;
//        }
//
//        public BluetoothDevice getDevice(int position) {
//            return mLeDevices.get(position);
//        }
//
//        public ArrayList<BluetoothDevice> getDevices(ArrayList positions) {
//            ArrayList<BluetoothDevice> d = null;
//            for(int i=0; i<positions.size(); i++) {
//                d.add(mLeDevices.get((Integer) positions.get(i)));
//            }
//            return d;
//        }
//
//
//        public void clear() {
//            mLeDevices.clear();
//            mSelectedLeDevices.clear();
//        }
//
//        @Override
//        public int getCount() {
//            return mLeDevices.size();
//        }
//
//        @Override
//        public Object getItem(int i) {
//            return mLeDevices.get(i);
//        }
//
//        @Override
//        public long getItemId(int i) {
//            return i;
//        }
//
//        @Override
//        public View getView(int i, View view, ViewGroup viewGroup) {
//            ViewHolder viewHolder;
//            // General ListView optimization code.
//            if (view == null) {
//                view = mInflator.inflate(R.layout.listitem_device, null);
//                viewHolder = new ViewHolder();
//                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
//                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
//                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.device_rssi);
//                view.setTag(viewHolder);
//            } else {
//                viewHolder = (ViewHolder) view.getTag();
//            }
//
//            BluetoothDevice device = mLeDevices.get(i);
//            final String deviceName = device.getName();
//            final String deviceRssi = mLeDeviceRssi.get(i);
//            if (deviceName != null && deviceName.length() > 0)
//                viewHolder.deviceName.setText(deviceName);
//            else
//                viewHolder.deviceName.setText(R.string.unknown_device);
//            viewHolder.deviceAddress.setText(device.getAddress());
//            viewHolder.deviceRssi.setText(deviceRssi + " dBm");
//
//            return view;
//        }
//    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mLeDeviceListAdapter.addDevice(device, String.valueOf(rssi));
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }
}
