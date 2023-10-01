package com.example.smartkidbuckleapp;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.navigation.ui.AppBarConfiguration;

import com.example.smartkidbuckleapp.databinding.ActivityMainBinding;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_ENABLE_BT = 1;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mBtLeScanner;
    private BleScanCallback scanCallBack;
    //
    String DEVICE_NAME = "StartKidBuckle_device";
    String SERVICE_UUID = "19B10000-E8F2-537E-4F6C-D104768A1214";
    String CHARACTERISTIC_UUID = "0fc10cb8-0518-40dd-b5c3-c4637815de40";
    private int SCAN_PERIOD = 10000;//
    private  Handler handler;
    boolean scanning = false;
    private boolean BUCKLE_CONNECTED = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        handler = new Handler();


        //BLEがサポートされてなければ終了
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
        mBtLeScanner = mBtAdapter.getBluetoothLeScanner();
        scanCallBack = new BleScanCallback();

        //BTが無効の場合許可を求める
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // 許可を要求
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101
            );
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    102
            );
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    103
            );
        }



    }

    public void startScan(View view){


        if(!scanning){


            List<ScanFilter> filterList = new ArrayList<>();
            ScanFilter filer = new ScanFilter.Builder().setDeviceName(DEVICE_NAME).build();
            filterList.add(filer);

            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

            mBtLeScanner.startScan(filterList,settings,scanCallBack);
            handler.postDelayed(()->{
                //mBtLeScanner.stopScan(scanCallBack);
                stopScan();
            }, SCAN_PERIOD);
            scanning = true;

            Button bt = (Button)findViewById(R.id.scanButton);
            bt.setText("Scanning");
            bt.setEnabled(false);
        }



    };

    private void log(String txt){
        ((TextView)findViewById(R.id.textView)).setText(txt);
    }
    private  void stopScan(){
        Button bt = (Button)findViewById(R.id.scanButton);
        bt.setText("Scan");
        bt.setEnabled(true);
        scanning = false;
    }

    private class BleScanCallback extends android.bluetooth.le.ScanCallback{
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(result != null && result.getDevice() != null){
                BluetoothDevice device = result.getDevice();

                if (device.getName().equals(DEVICE_NAME)) {
                    log("find device");
                    connectToDevice(device);
                }
            }
        }
    };
    private void connectToDevice(BluetoothDevice device) {
        BluetoothGatt gatt = device.connectGatt(this, false, gattCallback,BluetoothDevice.TRANSPORT_LE);
    }
    private  BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                log("device connected");
                gatt.discoverServices();
            }
            if(newState == BluetoothProfile.STATE_DISCONNECTED){
                log("device disconnected");

                if(BUCKLE_CONNECTED){
                    //TODO:アラートと出す

                }
            }
        }
        @Override

        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                //set notify
                if(gatt==null){return;}

                BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
                BluetoothGattCharacteristic chara = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));

                BluetoothGattDescriptor descriptor = chara.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

                gatt.setCharacteristicNotification(chara, true);

                // characteristic のnotification 有効化する
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

                log("set chara notification");


            }

        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            if(status == BluetoothGatt.GATT_SUCCESS){




            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //データを受信した際の処理
            super.onCharacteristicChanged(gatt, characteristic);
            int data = characteristic.getIntValue(FORMAT_UINT8, 0);
            if(data == 0){
                BUCKLE_CONNECTED = false;
                ((TextView)findViewById(R.id.buckleStatus)).setText("open");
            }
            else if(data == 1){
                BUCKLE_CONNECTED = true;
                ((TextView)findViewById(R.id.buckleStatus)).setText("close");

            }else{
                ((TextView)findViewById(R.id.buckleStatus)).setText("data");
            }
        }
    };


}