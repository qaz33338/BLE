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

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.SoundPool;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private SoundPool soundPool;
    private int sd1m,sd5m;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private int SCAN_INTERVAL=2000;
    private int STOP_INTERVAL=1000;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private Runnable startScan = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(true);
            mHandler.postDelayed(stopScan, SCAN_INTERVAL);
        }
    };

    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(false);
            mHandler.postDelayed(startScan, STOP_INTERVAL);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 5);
        sd1m = soundPool.load(this, R.raw.one, 1);
        sd5m = soundPool.load(this, R.raw.five, 1);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mHandler.post(startScan);
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
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {// ask for turn on bluetooth
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();//device list
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
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
/*
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {//click device
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());//send name
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());//send address
        if (mScanning) {//stop scan
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);//go to DeviceControlActivity class
    }
*/
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    public class ScanInformation{
        String Name;
        String Address;
        int Rssi;
        public double LastRssi;
        public int NearCount;
        String getName()
        {
            return Name;
        }
        String getAddress()
        {
            return Address;
        }
        int getRssi()
        {
            return Rssi;
        }
        void setRssi(int rssi){Rssi=rssi; }
        public ScanInformation(String name, String address, int rssi)
        {
            Name=name;
            Address=address;
            Rssi=rssi;
            LastRssi=0.0;
            NearCount=0;

        }
        public ScanInformation()
        {

        }

    }
    public double CalBleDistance(int rssi) {
        double exp = ((double) 35 - ((double) rssi + 100.0))
                / (10.0 * (double) 2);
        DecimalFormat df = new DecimalFormat("#.##");// to the second digit
        // after the decimal
        // point
        String s = df.format(Math.pow(10.0, exp));
        return Double.parseDouble(s);
    }




    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private List<ScanInformation> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<ScanInformation>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(ScanInformation NewScan) {
            boolean isExist=false;
            for(int i=0;i<mLeDevices.size();i++)
            {
                if(mLeDevices.get(i).getAddress().equals((NewScan.getAddress())))
                {
                    isExist=true;
                    mLeDevices.get(i).setRssi(NewScan.getRssi());
                    break;
                }
            }

            if(!isExist) {
                /*
                ScanInformation NewScan = new ScanInformation();
                NewScan.Name = deviceName;
                NewScan.Address = deviceAddress;
                NewScan.Rssi = deviceRssi;*/
                mLeDevices.add(NewScan);
            }
        }
/*
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }*/

        public void clear() {
            mLeDevices.clear();
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.MACView);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.SSIDView);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.RSSIView);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ScanInformation temp = mLeDevices.get(i);
            final String name = temp.getName();
            final int rssi = temp.getRssi();
            final String address = temp.getAddress();
            final double Tclose=1,Tfar=5;
            if (name != null && name.length() > 0)
                viewHolder.deviceName.setText(name);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(address);
          /* if(temp.LastRssi<rssi)
            {
                temp.NearCount++;
            }
            else
            {
                temp.NearCount=0;
            }*/
           if(CalBleDistance(rssi)<=Tclose)
            {
                Vibrator myVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                myVibrator.vibrate(500);
                soundPool.play(sd1m, 1.0F, 1.0F, 0, 0, 1.0F);
                viewHolder.deviceRssi.setTextColor(Color.RED);
            }
           else if(CalBleDistance(rssi)<=Tfar &&CalBleDistance(rssi)>Tclose)
            {
                Vibrator myVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                myVibrator.vibrate(1000);
                soundPool.play(sd5m, 1.0F, 1.0F, 0, 0, 1.0F);
                viewHolder.deviceRssi.setTextColor(Color.BLUE);
            }
            else
            {
                viewHolder.deviceRssi.setTextColor(Color.BLACK);
            }
            viewHolder.deviceRssi.setText(rssi+"("+CalBleDistance(rssi)+"m)");
            temp.LastRssi=rssi;

            return view;
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            final ScanInformation NewScan = new ScanInformation(device.getName(), device.getAddress(), rssi);
            mLeDeviceListAdapter.addDevice(NewScan);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //mLeDeviceListAdapter.addDevice(device.getName(), device.getAddress(), Rssi[0]);
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