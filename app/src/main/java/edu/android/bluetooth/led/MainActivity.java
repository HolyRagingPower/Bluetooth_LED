package edu.android.bluetooth.led;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener{

    private static final String TAG = "bluetooth.led";

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String STRING_DELIMITER = "\n";
//    private static final char CHAR_DELIMITER = '\n';

    private BluetoothAdapter mBluetoothAdapter;
    private int mPairedDeviceCount = 0;
    private Set<BluetoothDevice> mDevices;
    private BluetoothDevice mRemoteDevice;
    private BluetoothSocket mSocket = null;
    private OutputStream mOutputStream = null;
//    private InputStream mInputStream = null;

//    private Thread mWorkerThread = null;
//    private byte[] readBuffer;
//    private int readBufferPosition;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    // 블루투스가 활성 상태로 변경됨
                    selectDevice();
                }
                else if(resultCode == RESULT_CANCELED){
                    // 블루투스가 비활성 상태임
                    finish();	// 어플리케이션 종료
                }
                break;
        }

    }

    void checkBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            // 장치가 블루투스를 지원하지 않는 경우
            finish();	// 어플리케이션 종료
        }
        else {
            // 장치가 블루투스를 지원하는 경우
            if (!mBluetoothAdapter.isEnabled()) {
                // 블루투스를 지원하지만 비활성 상태인 경우
                // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
                Intent enableBtIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else {
                // 블루투스를 지원하며 활성 상태인 경우
                // 페어링 된 기기 목록을 보여주고 연결할 장치를 선택
                selectDevice();
            }
        }
    }

    void selectDevice(){
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();

        if(mPairedDeviceCount == 0){
            // 페어링 된 장치가 없는 경우
            finish();		// 어플리케이션 종료
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 장치 선택");

        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("취소");		// 취소 항목 추가

        final CharSequence[] items =
                listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int item){
                if(item == mPairedDeviceCount){
                    // 연결할 장치를 선택하지 않고 ‘취소’를 누른 경우
                    finish();
                }
                else{
                    // 연결할 장치를 선택한 경우
                    // 선택한 장치와 연결을 시도함
                    connectToSelectedDevice(items[item].toString());
                }
            }
        });

        builder.setCancelable(false);	// 뒤로 가기 버튼 사용 금지
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
    void beginListenForData() {
        final Handler handler = new Handler();

        readBuffer = new byte[1024];	// 수신 버퍼
        readBufferPosition = 0;		// 버퍼 내 수신 문자 저장 위치

        // 문자열 수신 쓰레드
        mWorkerThread = new Thread(new Runnable(){
            public void run(){
                while(!Thread.currentThread().isInterrupted()){
                    try {
                        int bytesAvailable = mInputStream.available();	// 수신 데이터 확인
                        if(bytesAvailable > 0){		// 데이터가 수신된 경우
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i = 0; i < bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == CHAR_DELIMITER){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0,
                                            encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");
                                    Log.i(TAG, "data from Arduino over the BT: " + data);

                                    readBufferPosition = 0;

                                    handler.post(new Runnable(){
                                        public void run(){
                                            // 수신된 문자열 데이터에 대한 처리 작업
                                        }
                                    });
                                }
                                else{
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException e){
                        e.printStackTrace();
                        // 데이터 수신 중 오류 발생
                        finish();
                    }
                }
            }
        });

        mWorkerThread.start();
    }
    */

    void sendData(String msg){
        msg += STRING_DELIMITER;	// 문자열 종료 표시
        try{
            mOutputStream.write(msg.getBytes());		// 문자열 전송
        }catch(Exception e){
            e.printStackTrace();
            // 문자열 전송 도중 오류가 발생한 경우
            finish();		// 어플리케이션 종료
        }
    }

    BluetoothDevice getDeviceFromBondedList(String name){
        BluetoothDevice selectedDevice = null;

        for (BluetoothDevice device : mDevices) {
            if(name.equals(device.getName())){
                selectedDevice = device;
                break;
            }
        }

        return selectedDevice;
    }

    @Override
    protected void onDestroy() {
        try{
//            mWorkerThread.interrupt();	// 데이터 수신 쓰레드 종료
//            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        super.onDestroy();
    }

    void connectToSelectedDevice(String selectedDeviceName){
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try{
            // 소켓 생성
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            // RFCOMM 채널을 통한 연결
            mSocket.connect();

            // 데이터 송수신을 위한 스트림 얻기
            mOutputStream = mSocket.getOutputStream();
//            mInputStream = mSocket.getInputStream();

            // 데이터 수신 준비
//            beginListenForData();
        }catch(Exception e){
            e.printStackTrace();
            // 블루투스 연결 중 오류 발생
            finish();		// 어플리케이션 종료
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton btn1 = findViewById(R.id.toggleButton1);
        btn1.setOnClickListener(this);
        ToggleButton btn2 = findViewById(R.id.toggleButton2);
        btn2.setOnClickListener(this);
        ToggleButton btn3 = findViewById(R.id.toggleButton3);
        btn3.setOnClickListener(this);
        ToggleButton btn4 = findViewById(R.id.toggleButton4);
        btn4.setOnClickListener(this);
        ToggleButton btn5 = findViewById(R.id.toggleButton5);
        btn5.setOnClickListener(this);

        checkBluetooth();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        String str = new String();

        switch(v.getId()){
            case R.id.toggleButton1:
                str = "1";
                break;
            case R.id.toggleButton2:
                str = "2";
                break;
            case R.id.toggleButton3:
                str = "3";
                break;
            case R.id.toggleButton4:
                str = "4";
                break;
            case R.id.toggleButton5:
                str = "5";
                break;
        }

        sendData(str);
    }
}