package com.aleksandr.nikitin.kyrsachbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "bluetooth1";

    private StringBuilder sb = new StringBuilder();

    private Button btnOnGreen, btnOnBlue, btnOffAll;
    private Button btnSendPass;
    private TextView tvLeftDis;
    private TextView tvRightDis;
    private TextView tvLog;

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
    //private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-адрес Bluetooth модуля
    private static String address = "98:D3:31:FC:43:92";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvLeftDis = (TextView) findViewById(R.id.tvLeftDis);
        tvRightDis = (TextView) findViewById(R.id.tvRightDis);
        tvLog = (TextView) findViewById(R.id.tvLog);

        findViewById(R.id.btnUp).setOnClickListener(this);
        findViewById(R.id.btnDown).setOnClickListener(this);
        findViewById(R.id.btnLeft).setOnClickListener(this);
        findViewById(R.id.btnRight).setOnClickListener(this);

        btnSendPass = (Button) findViewById(R.id.btnSendPass);

        btnOnGreen = (Button) findViewById(R.id.btnGreenOn);
        //btnOnBlue = (Button) findViewById(R.id.btnBlueOn);
        btnOffAll = (Button) findViewById(R.id.btnOffAll);

        chBoxIsConn = (CheckBox) findViewById(R.id.chBoxIsConn);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        btnOnGreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //sendData("12");
                mConnectedThread.write("2");
                //Toast.makeText(getBaseContext(), "Включаем LED", Toast.LENGTH_SHORT).show();
            }
        });

        /*
        btnOnBlue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendData("2");
                //Toast.makeText(getBaseContext(), "Включаем LED", Toast.LENGTH_SHORT).show();
            }
        });
        */

        btnOffAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //sendData("0");
                mConnectedThread.write("0");
                //Toast.makeText(getBaseContext(), "Выключаем LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnSendPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendData(String.valueOf(((TextView) findViewById(R.id.edPass)).getText()));
                mConnectedThread.write(String.valueOf(((TextView) findViewById(R.id.edPass)).getText()));
            }
        });

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
                        }
                        //Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
                        break;
                }
            };
        };

    }

    /*
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
            //errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e.getMessage() + ".");
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Создание Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }

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

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }
    */

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

    /*
    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Посылаем данные: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nВ переменной address у вас прописан 00:00:00:00:00:00, вам необходимо прописать реальный MAC-адрес Bluetooth модуля";
            msg = msg +  ".\n\nПроверьте поддержку SPP UUID: " + MY_UUID.toString() + " на Bluetooth модуле, к которому вы подключаетесь.\n\n";

            errorExit("Fatal Error", msg);
        }
    }
    */

    @Override
    public void onClick(View v) {
        String com = null;
        switch (v.getId()) {
            case R.id.btnUp:
                com = "u";
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
        if(com != null) {
            //sendData(com);
            mConnectedThread.write(com);
        }
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
                mmOutStream.write(msgBuffer);
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
