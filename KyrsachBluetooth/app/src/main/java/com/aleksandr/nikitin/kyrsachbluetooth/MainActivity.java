package com.aleksandr.nikitin.kyrsachbluetooth;

import android.app.Dialog;
import android.app.TimePickerDialog;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ColorPicker.OnColorChangedListener, ColorPicker.OnColorSelectedListener {
    private static final String TAG = "bluetooth1";

    private StringBuilder sb = new StringBuilder();

    private Button btnOnGreen, btnOnBlue, btnOffAll;
    private Button btnSendPass;
    private TextView tvLeftDis;
    private TextView tvRightDis;
    private TextView tvLog;

    private int DIALOG_TIME = 1;
    private int myHour = Integer.valueOf((new SimpleDateFormat("HH")).format(System.currentTimeMillis()));
    private int myMinute = Integer.valueOf((new SimpleDateFormat("mm")).format(System.currentTimeMillis()));

    private ColorPicker picker;
    private SVBar svBar;

    private TextView tvRed;
    private TextView tvGreen;
    private TextView tvBlue;

    private EditText etTime;
    private Button btnSetTime;

    private TextView tvState;

    private TextView tvLight;
    private Switch swLight;

    private Handler h;

    private ConnectedThread mConnectedThread;

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

        //etTime = (EditText) findViewById(R.id.etTime);
        btnSetTime = (Button) findViewById(R.id.btnSetTime);
        tvState = (TextView) findViewById(R.id.tvState);
        tvLight = (TextView) findViewById(R.id.tvLight);
        swLight = (Switch) findViewById(R.id.swLight);

        btnSetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myHour = Integer.valueOf((new SimpleDateFormat("HH")).format(System.currentTimeMillis()));
                myMinute = Integer.valueOf((new SimpleDateFormat("mm")).format(System.currentTimeMillis()));
                showDialog(DIALOG_TIME);
                /*
                String tempStr = etTime.getText().toString();
                String time = null;
                switch (tempStr.length()) {
                    case 1:
                        time = "000";
                        break;
                    case 2:
                        time = "00";
                        break;
                    case 3:
                        time = "0";
                        break;
                    case 4:
                        time = "";
                        break;
                    default:
                        return;
                }
                time += tempStr + 't';
                if(isConnected) {
                    mConnectedThread.write(time);
                }
                */
            }
        });

        swLight.setChecked(false);
        swLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvLight.setText("Светильник ВКЛ");
                    findViewById(R.id.rlColor).setVisibility(View.VISIBLE);
                    setColorToView();
                } else {
                    tvLight.setText("Светильник ВЫКЛ");
                    findViewById(R.id.rlColor).setVisibility(View.INVISIBLE);
                    if (isConnected) {
                        mConnectedThread.write("r000q");
                        mConnectedThread.write("g000q");
                        mConnectedThread.write("b000q");
                    }
                }
            }
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

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
                            //tvLog.setText(sbprint);
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
            tvState.setText("Соединение установлено");
            tvState.setTextColor(Color.GREEN);
        } else {
            tvState.setText("Соединение НЕ установлено");
            tvState.setTextColor(Color.RED);
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


    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_TIME) {
            myHour = Integer.valueOf((new SimpleDateFormat("HH")).format(System.currentTimeMillis()));
            myMinute = Integer.valueOf((new SimpleDateFormat("mm")).format(System.currentTimeMillis()));
            TimePickerDialog tpd = new TimePickerDialog(this, myCallBack, myHour, myMinute, true);
            return tpd;
        }
        return super.onCreateDialog(id);
    }

    TimePickerDialog.OnTimeSetListener myCallBack = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Toast.makeText(getApplicationContext(), String.valueOf(hourOfDay) + ":" + String.valueOf(minute), Toast.LENGTH_SHORT).show();

            long date = System.currentTimeMillis();

            SimpleDateFormat sdfHour = new SimpleDateFormat("HH");
            SimpleDateFormat sdfMinute = new SimpleDateFormat("mm");
            int h = Integer.valueOf(sdfHour.format(date));
            int m = Integer.valueOf(sdfMinute.format(date));
            Toast.makeText(getApplicationContext(), String.valueOf(h) + ":" + String.valueOf(m), Toast.LENGTH_SHORT).show();

            int rrr = minute - m;
            Log.d("TTime", "rrr = " + String.valueOf(rrr));
            String tempTime = String.valueOf(rrr);
            String time = null;
            switch (tempTime.length()) {
                case 1:
                    time = "000";
                    break;
                case 2:
                    time = "00";
                    break;
                case 3:
                    time = "0";
                    break;
                case 4:
                    time = "";
                    break;
                default:
                    return;
            }
            time += tempTime + 't';
            if(isConnected) {
                Log.d("TTime", time);
                mConnectedThread.write(time);
            }

        }
    };
}
