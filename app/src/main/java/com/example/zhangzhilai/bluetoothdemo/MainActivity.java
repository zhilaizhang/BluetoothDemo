package com.example.zhangzhilai.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.app.Activity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends Activity implements OnClickListener{

    private Context mContext;

    private final int REQUEST_ENALBLE = 1;
    private final String BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private ListView    mBluetoothListview;
    private FrameLayout mLoadingFramelayout;

    private ArrayAdapter mArrayAdapter;

    private Button  mFindDeviceButton;
    private Button  mConnectButton;
    private Button  mDisconnectButton;
    private Button  mCloseButton;
    private Button  mOpenButton;

    private BluetoothUtils   mBluetoothUtils;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice  mDevice;
    private BluetoothSocket  mBluetoothSocket;

//    private ConnectThread  mConnectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initData();
        setListeners();
    }

    public void findViews(){
        mContext = this;
        mBluetoothListview = (ListView)findViewById(R.id.bluetooth_listview);
        mFindDeviceButton = (Button)findViewById(R.id.find_device_btn);
        mConnectButton = (Button)findViewById(R.id.connect_btn);
        mDisconnectButton = (Button)findViewById(R.id.disconnect_btn);
        mOpenButton = (Button)findViewById(R.id.open_btn);
        mCloseButton = (Button)findViewById(R.id.close_btn);
        mLoadingFramelayout = (FrameLayout)findViewById(R.id.loading_framelayout);

    }

    public void initData(){
        mBluetoothUtils = new BluetoothUtils(mContext);
        mBluetoothAdapter = mBluetoothUtils.mBluetoothAdapter;
        mArrayAdapter = new ArrayAdapter(mContext, android.R.layout.simple_expandable_list_item_1);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    public void setListeners(){

        mConnectButton.setOnClickListener(this);
        mFindDeviceButton.setOnClickListener(this);
        mDisconnectButton.setOnClickListener(this);
        mCloseButton.setOnClickListener(this);
        mOpenButton.setOnClickListener(this);

        mBluetoothListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothUtils.endDiscoverBluetooth();

                mArrayAdapter.getItem(position);
                Toast.makeText(mContext,"   position: " + position,Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 开启蓝牙功能
     */
    private void openBluetoothDevice(){
        if(!mBluetoothAdapter.isEnabled()){
            Intent enalbeBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enalbeBluetoothIntent, REQUEST_ENALBLE);
        } else {
            mBluetoothUtils.startDiscoverBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENALBLE && resultCode == RESULT_OK){
            mBluetoothUtils.startDiscoverBluetooth();
            Toast.makeText(mContext,"蓝牙查找" + mBluetoothUtils.startDiscoverBluetooth(),Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_ENALBLE && resultCode == RESULT_CANCELED){
            Toast.makeText(mContext,"蓝牙被关闭",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        CommunicateThread communicateThread = null;
        switch (v.getId()){
            case R.id.find_device_btn:
                if(mBluetoothUtils.mBluetoothAdapter != null){
                    openBluetoothDevice();
                } else {
                    Toast.makeText(mContext,"设备没有蓝牙功能",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.connect_btn:
//                mConnectThread = new ConnectThread(mDevice);
//                mConnectThread.start();
                ConnectAsynctask connectAsyctask = new ConnectAsynctask();
                connectAsyctask.execute(mDevice);
                break;
            case R.id.disconnect_btn:
//                if(mConnectThread != null){
//                    mConnectThread.cancle();
//                }

                break;
            case R.id.open_btn:
                 communicateThread = new CommunicateThread(mBluetoothSocket);
                byte[] openBytes = new byte[5];
                openBytes[0] = (byte)0xA1;
                openBytes[1] = (byte)0xFD;
                openBytes[2] = (byte)0x0B;
                openBytes[3] = (byte)0x02;
                openBytes[4] = (byte)0xDF;
                communicateThread.write(openBytes);
                break;
            case R.id.close_btn:
                communicateThread = new CommunicateThread(mBluetoothSocket);
                byte[] closeBytes = new byte[5];
                closeBytes[0] = (byte)0xA1;
                closeBytes[1] = (byte)0xFD;
                closeBytes[2] = (byte)0x0B;
                closeBytes[3] = (byte)0x01;
                closeBytes[4] = (byte)0xDF;
                communicateThread.write(closeBytes);
                break;
            default:
                break;
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                Toast.makeText(mContext,"蓝牙被发现",Toast.LENGTH_SHORT).show();
                mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mArrayAdapter.add("蓝牙名：" + mDevice.getName() + "\n" + mDevice.getAddress());
                mBluetoothListview.setAdapter(mArrayAdapter);
            }
        }
    };


    public final class ConnectAsynctask extends AsyncTask<BluetoothDevice, Integer, BluetoothSocket>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingFramelayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
            BluetoothSocket bluetoothSocket = null;
            BluetoothDevice bluetoothDevice = params[0];
            try{
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_UUID));
            } catch (IOException e) {
                Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return null;
            }
            return bluetoothSocket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            super.onPostExecute(bluetoothSocket);
            mLoadingFramelayout.setVisibility(View.GONE);
            if(bluetoothSocket == null){
                Toast.makeText(mContext,"连接失败",Toast.LENGTH_SHORT).show();
            } else {
                mBluetoothSocket = bluetoothSocket;
            }
        }
    }

//    public final class CommunicateAsynctask extends AsyncTask<BluetoothSocket, Integer, Boolean>{
//
//        @Override
//        protected Boolean doInBackground(BluetoothSocket... params) {
//            BluetoothSocket bluetoothSocket = null;
//            InputStream     inputStream = null;
//            OutputStream    outputStream = null;
//
//            byte[] buffer = new byte[1024];
//            int bytes;
//
//            bluetoothSocket = params[0];
//
//            try {
//                inputStream = bluetoothSocket.getInputStream();
//                outputStream = bluetoothSocket.getOutputStream();
//
//
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean aBoolean) {
//            super.onPostExecute(aBoolean);
//        }
//    }

    /**
     * 连接蓝牙设备
     */
//    private final class ConnectThread extends Thread{
//        private  BluetoothSocket bluetoothSocket = null;
//        public ConnectThread(BluetoothDevice bluetoothDevice){
//            BluetoothSocket tmp = null;
//            try{
//            tmp = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_UUID));
//            } catch (IOException e) {
//                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_SHORT).show();
//            }
//            bluetoothSocket = tmp;
//        }
//
//        @Override
//        public void run() {
//            //TODO 取消搜索不建议在线程中来实现
//            mBluetoothAdapter.cancelDiscovery();
//            try {
//                bluetoothSocket.connect();
//            } catch (IOException e) {
//                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_SHORT).show();
//                cancle();
//                e.printStackTrace();
//            }
//        }
//
//        public void cancle(){
//            try {
//                bluetoothSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }

    /**
     * 与蓝牙设备进行交流
     */
    private final class CommunicateThread extends Thread{

        BluetoothSocket  bluetoothSocket = null;

        InputStream inputStream = null;

        OutputStream outputStream = null;


        public CommunicateThread(BluetoothSocket bluetoothSocket){
            try{
                bluetoothSocket = bluetoothSocket;

                inputStream = bluetoothSocket.getInputStream();

                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true){
                    try {
                        bytes = inputStream.read(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel(){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private byte[] getHexBytes(String message) {
        int len = message.length() / 2;
        char[] chars = message.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
    }

}
