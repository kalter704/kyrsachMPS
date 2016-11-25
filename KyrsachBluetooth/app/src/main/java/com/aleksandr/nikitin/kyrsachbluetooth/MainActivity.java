package com.aleksandr.nikitin.kyrsachbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ColorPicker.OnColorChangedListener, ColorPicker.OnColorSelectedListener {
    private static final String TAG = "bluetooth1";

    private StringBuilder sb = new StringBuilder();

    private Button btnOnGreen, btnOnBlue, btnOffAll;
    private Button btnSendPass;
    private TextView tvLeftDis;
    private TextView tvRightDis;
    private TextView tvLog;

    private ColorPicker picker;
    private SVBar svBar;

    private TextView tvRed;
    private TextView tvGreen;
    private TextView tvBlue;

    Handler h;

    private ConnectedThread mConnectedThread;

    private CheckBox chBoxIsConn;

    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private boolean isConnected = false;

    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-адрес Bluetooth модуля
    private static String address = "98:D3:31:FC:43:92";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /*
        tvLeftDis = (TextView) findViewById(R.id.tvLeftDis);
        tvRightDis = (TextView) findViewById(R.id.tvRightDis);
        tvLog = (TextView) findViewById(R.id.tvLog);

        findViewById(R.id.btnUp).setOnClickListener(this);
        findViewById(R.id.btnDown).setOnClickListener(this);
        findViewById(R.id.btnLeft).setOnClickListener(this);
        findViewById(R.id.btnRight).setOnClickListener(this);
        */

        //btnSendPass = (Button) findViewById(R.id.btnSendPass);

        //btnOnGreen = (Button) findViewById(R.id.btnGreenOn);
        //btnOnBlue = (Button) findViewById(R.id.btnBlueOn);
        //btnOffAll = (Button) findViewById(R.id.btnOffAll);

        chBoxIsConn = (CheckBox) findViewById(R.id.chBoxIsConn);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        /*
        btnOnGreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("2");
                //Toast.makeText(getBaseContext(), "Включаем LED", Toast.LENGTH_SHORT).show();
            }
        });
        */

        /*
        btnOnBlue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendData("2");
                //Toast.makeText(getBaseContext(), "Включаем LED", Toast.LENGTH_SHORT).show();
            }
        });
        */

        /*
        btnOffAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("0");
                //Toast.makeText(getBaseContext(), "Выключаем LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnSendPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConnectedThread.write(String.valueOf(((TextView) findViewById(R.id.edPass)).getText()));
            }
        });
        */

        tvRed = (TextView) findViewById(R.id.tvRed);
        tvGreen = (TextView) findViewById(R.id.tvGreen);
        tvBlue = (TextView) findViewById(R.id.tvBlue);

        picker = (ColorPicker) findViewById(R.id.picker);
        svBar = (SVBar) findViewById(R.id.svbar);

        picker.addSVBar(svBar);

        setColorToView();

        //To set the old selected color u can do it like this
        picker.setOldCenterColor(picker.getColor());
        // adds listener to the colorpicker which is implemented
        //in the activity
        picker.setOnColorChangedListener(this);
        picker.setOnColorSelectedListener(this);

        //to turn of showing the old color
        picker.setShowOldCenterColor(false);



        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:                                                   // если приняли сообщение в Handler
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        sb.append(strIncom);                                                // формируем строку
                        int endOfLineIndex = sb.indexOf("\r\n");                            // определяем символы конца строки
                        if (endOfLineIndex > 0) {                                            // если встречаем конец строки,
                            String sbprint = sb.substring(0, endOfLineIndex);               // то извлекаем строку
                            sb.delete(0, sb.length());                                      // и очищаем sb
                            tvLog.setText(sbprint);
                            if(sbprint.startsWith("l")) {
                                tvLeftDis.setText(sbprint.substring(1, sbprint.length()));
                            } else if(sbprint.startsWith("r")) {
                                tvRightDis.setText(sbprint.substring(1, sbprint.length()));
                            }
                        }
                        //Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
                        break;
                }
            };
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - попытка соединения...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Соединяемся...");
        try {
            btSocket.connect();
            isConnected = true;
            Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
        } catch (IOException e) {
            isConnected = false;
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Создание Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        if(isConnected) {
            chBoxIsConn.setChecked(true);
        } else {
            chBoxIsConn.setChecked(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth не поддерживается");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth включен...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onClick(View v) {
        String com = null;
        /*
        switch (v.getId()) {
            case R.id.btnUp:
                com = "r456q";
                break;
            case R.id.btnDown:
                com = "d";
                break;
            case R.id.btnLeft:
                com = "l";
                break;
            case R.id.btnRight:
                com = "r";
                break;
        }
        */
        if(com != null) {
            mConnectedThread.write(com);
        }
    }

    private void showToast(byte b) {
        Toast.makeText(getApplicationContext(), String.valueOf((char)b), Toast.LENGTH_SHORT).show();
    }

    private void setColorToView() {
        int color = picker.getColor();

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        String redString = String.valueOf(red);
        String greenString = String.valueOf(green);
        String blueString = String.valueOf(blue);

        String nowRedString = null;
        String nowGreenString = null;
        String nowBlueString = null;

        if(redString.length() == 2) {
            nowRedString = "r0" + redString + "q";
        } else if(redString.length() == 1) {
            nowRedString = "r00" + redString + "q";
        } else {
            nowRedString = "r" + redString + "q";
        }

        if(greenString.length() == 2) {
            nowGreenString = "g0" + greenString + "q";
        } else if(greenString.length() == 1) {
            nowGreenString = "g00" + greenString + "q";
        } else {
            nowGreenString = "g" + greenString + "q";
        }

        if(blueString.length() == 2) {
            nowBlueString = "b0" + blueString + "q";
        } else if(blueString.length() == 1) {
            nowBlueString = "b00" + blueString + "q";
        } else {
            nowBlueString = "b" + blueString + "q";
        }

        tvRed.setText("Red: " + nowRedString);
        tvGreen.setText("Green: " + nowGreenString);
        tvBlue.setText("Blue: " + nowBlueString);

        if(isConnected) {
            mConnectedThread.write(nowRedString);
            mConnectedThread.write(nowGreenString);
            mConnectedThread.write(nowBlueString);
        }
    }

    @Override
    public void onColorChanged(int color) {
        setColorToView();
    }

    @Override
    public void onColorSelected(int color) {

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Получаем кол-во байт и само собщение в байтовый массив "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Отправляем в очередь сообщений Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Данные для отправки: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                for(int i = 0; i < message.length(); ++i) {
                    mmOutStream.write(msgBuffer[i]);
                    showToast(msgBuffer[i]);
                }
            } catch (IOException e) {
                Log.d(TAG, "...Ошибка отправки данных: " + e.getMessage() + "...");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
