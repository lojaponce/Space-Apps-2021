package com.example.mitienda;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivityControl extends Activity {
    private static final String TAG = "bluetooth2";

    Button btnOn, btnOff, btnOnMotAg, btnOffMotAg,BtnAceptar,btnHumedadOn;
    TextView txtArduino;
    Handler h;
    EditText EdtTextOut;

    final int RECEIVE_MESSAGE = 1;    //1    // Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "00:19:08:35:31:EE";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        btnOn = (Button) findViewById(R.id.btnOn);                  // button LED ON
        btnOff = (Button) findViewById(R.id.btnOff);                // button LED OFF
        btnOnMotAg = (Button) findViewById(R.id.btnOnMotAg);                // button LED OFF
        btnOffMotAg = (Button) findViewById(R.id.btnOffMotAg);                // button LED OFF
        BtnAceptar = (Button) findViewById(R.id.BtnAceptar);                // button LED OFF
        btnHumedadOn = (Button) findViewById(R.id.btnHumedadOn);                // button LED OFF
        EdtTextOut = (EditText) findViewById(R.id.EdtTextOut);                // button LED OFF

        txtArduino = (TextView) findViewById(R.id.tvtMensaje);      // for display the received data             from the Arduino
        //cargarDatosDni();
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:                                   // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1); // create string from bytes array
                        txtArduino.setText("Data from Arduino: " + strIncom);
                        Toast.makeText(getBaseContext(),"Se recibió este msms: " +strIncom, Toast.LENGTH_LONG).show();
                        // append string
                        break;
                }
            };
        };


        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //  btnOn.setEnabled(false);
                mConnectedThread.write("1");    // Send "1" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //  btnOff.setEnabled(false);
                mConnectedThread.write("0");    // Send "0" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });
        btnOnMotAg.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //  btnOff.setEnabled(false);
                mConnectedThread.write("3");    // Send "0" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });
        btnOffMotAg.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //  btnOff.setEnabled(false);
                mConnectedThread.write("8");    // Send "0" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });
        btnHumedadOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //  btnOff.setEnabled(false);
                mConnectedThread.write("10");    // Send "0" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });
        //AAAAAAAAAAAAAAAAAAAAAAAA
        BtnAceptar.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //  btnOff.setEnabled(false);
                String ed= EdtTextOut.getText().toString();
                mConnectedThread.write(ed);    // Send "0" via Bluetooth
                //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDatosDni() {
        Bundle datos=this.getIntent().getExtras();
        address=datos.getString("etAddress");
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
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
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);

            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}
