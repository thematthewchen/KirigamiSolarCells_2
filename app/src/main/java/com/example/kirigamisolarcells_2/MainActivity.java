package com.example.kirigamisolarcells_2;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.solver.widgets.Rectangle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity{

    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    TextView txtLat;
    String lat;
    String provider;
    protected String latitude,longitude;
    protected boolean gps_enabled,network_enabled;
    Button btnSend, locationbutton, pitchbutton;//Button variables for controls
    private BluetoothAdapter myBluetooth = null;    //BluetoothAdapter variable to control bluetooth
    private Set<BluetoothDevice> pairedDevices;                      //Set variable for list of connected devices
    ConnectThread connection;
    ConnectedThread mConnectedThread;
    BluetoothDevice mDevice;
    private EditText userinput;
    private FusedLocationProviderClient mFusedLocationClient;
    String mainText;
    //private Rect rect;
    //private Paint paint;

    public MainActivity(){
        //rect = new Rect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        btnSend = (Button)findViewById(R.id.buttonSend);
        locationbutton = (Button)findViewById(R.id.khalid);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        if(myBluetooth == null)//if bluetooth not available for android device
        {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            finish();
        }
        if (!myBluetooth.isEnabled())
        {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);//REQUEST_ENABLE_BT = 1
        }
        else{
            Toast.makeText(getApplicationContext(), "Bluetooth Enabled", Toast.LENGTH_LONG).show();
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //
        SensorManager sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        final float[] mValuesMagnet      = new float[3];
        final float[] mValuesAccel       = new float[3];
        final float[] mValuesOrientation = new float[3];
        final float[] mRotationMatrix    = new float[9];

        pitchbutton = (Button) findViewById(R.id.pitch);
        final TextView txt1 = (TextView) findViewById(R.id.gpstext);
        final SensorEventListener mEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                // Handle the events for which we registered
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        System.arraycopy(event.values, 0, mValuesAccel, 0, 3);
                        break;

                    case Sensor.TYPE_MAGNETIC_FIELD:
                        System.arraycopy(event.values, 0, mValuesMagnet, 0, 3);
                        break;
                }
            };
        };

        // You have set the event lisetner up, now just need to register this with the
        // sensor manager along with the sensor wanted.
        //setListeners(sensorManager, mEventListener);
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        pitchbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                SensorManager.getRotationMatrix(mRotationMatrix, null, mValuesAccel, mValuesMagnet);
                SensorManager.getOrientation(mRotationMatrix, mValuesOrientation);
                final CharSequence test;
                test = "results: " + mValuesOrientation[0] +" "+mValuesOrientation[1]+ " "+ mValuesOrientation[2];
                txt1.setText(test);
            }
        });
        locationbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                sendLocation();
            }
        });
        pairedDevicesList();
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //connect(device, "test");
            }
        }
    };

    //Querying paired devices
    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();
        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                mDevice = device;
                connection = new ConnectThread(mDevice);
                if(connection.isConnected()){
                    connection.start();
                    break;
                }
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }
    }

    //When 'Send' Button called
    public void onClickSend(View view) {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String status;
                userinput = (EditText) findViewById(R.id.inputText);
                status = userinput.getText().toString() + "\0";
                mConnectedThread.write(status.getBytes());
            }
        });
    }

    //When 'Location' Button called
    public void onClickLocation(View view) {

    }

    public void sendLocation(){
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                            }
                            final double latitude = location.getLatitude();
                            final double longitude = location.getLongitude();
                            String status;
                            status = latitude + "#" + longitude + "\0";
                            mConnectedThread.write(status.getBytes());
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, "Turn on Your Location To Orient the Solar Panels", Toast.LENGTH_SHORT).show();
        }


    }
    /*float[] mGravity;
    float[] mGeomagnetic;
    double azimut;
    double pitch;
    double roll;
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                pitch = orientation[1];
                roll = orientation[2];
            }
        }
    }*/



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
    /*
    *
    *
    *
    *
    * Separate Thread for Connecting
    *
    *
    *
    *
     */
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private ConnectedThread manage;
        //private boolean isconnected = false;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice. This code below show how to do it and handle the case that the UUID from the device is not found and trying a default UUID.

            // Default UUID
            UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            Log.d("UUIDNull", " UUID from device is null, Using Default UUID, Device name: " + device.getName());
            try {
                tmp = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            mmSocket = tmp;
            Log.e("CreateSocket2", "connected to temp");
        }

        public boolean isConnected(){
            return mmSocket != null;
        }

        // Connect to the remote device through the socket.
        public void run() {
            myBluetooth.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("CloseSocket", "Could not close the client socket", closeException);
                }
                //isconnected = false;
                return;
            }
            //isconnected = true;
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket, input);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("SocketClose", "Could not close the client socket", e);
            }
        }
    }

    /*
    *
    *
    *
    * Once connected, calls this class
    *
    *
    *
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            //char[] chars= new char[1024];
            int numBytes; // bytes returned from read()
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    // Read from the InputStream.
                    buffer[bytes] = (byte)mmInStream.read();
                    if ((buffer[bytes] == '\n'))
                    {
                        //String s1 = Arrays.toString(buffer);
                        String readMessage = new String(buffer, 0, bytes);
                        String finalMessage = "";
                        String letter = "";
                        //Hardcoding reading the byte array input
                        boolean next = true;
                        //boolean read = false;
                        int count = 0;//temporary count variable for each increase
                        int maxnext = 2;//the max that count will go
                        for(int i = 0; i < readMessage.length(); ++i){
                            if(next){
                                if(readMessage.charAt(i) == '1'){
                                    maxnext = 3;//read a 3 digit number
                                }
                                else{
                                    maxnext = 2;//read a 2 digit number
                                }
                                next = false;
                                count = 0;
                            }
                            ++count;
                            letter += readMessage.charAt(i);
                            if(count == maxnext){
                                finalMessage += (char)Integer.parseInt(letter);
                                next = true;
                                letter = "";
                                //read = true;
                            }
                        }
                        //if(read){
                            Message readMsg = mHandler.obtainMessage(0, bytes, -1, finalMessage);
                            readMsg.sendToTarget();
                        //}
                        bytes=0;
                    }
                    else {
                        bytes++;
                    }
                    //mHandler.obtainMessage(0, numBytes, -1, buffer).sendToTarget();
                    /*String readMessage = new String(buffer, 0, numBytes);
                    if(readMessage.charAt((int)readMessage.length() - 1) == '#'){
                        // Send the obtained bytes to the UI activity.
                        Message readMsg = mHandler.obtainMessage(1, numBytes, -1, readMessage);
                        readMsg.sendToTarget();
                    }
                    else{
                        // Send the obtained bytes to the UI activity.
                        Message readMsg = mHandler.obtainMessage(0, numBytes, -1, readMessage);
                        readMsg.sendToTarget();
                    }*/
                    /*
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                     */
                } catch (IOException e) {
                    Log.d("read stream", "Input stream was disconnected", e);
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("write", "Cannot Write", e);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /*
            String writeMessage;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;
            byte[] hold = new byte[end - begin];
            char[] temp = new char[end - begin];
            for(int i = begin; i < end; i++){
                temp[i] = (char)writeBuf[i];
            }

                writeMessage = new String(temp);
            /*catch (UnsupportedEncodingException e) {
                throw new AssertionError("UTF-8 is unknown");
            }*/
            //writeMessage = writeMessage.substring(begin, end);*/
           switch(msg.what){
                case 0:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    //String readMessage = new String(writeBuf, 0, msg.arg1);
                    String readMessage = (String) msg.obj;
                    if(readMessage.length() > 0){
                        Toast.makeText(MainActivity.this, readMessage, Toast.LENGTH_SHORT).show();
                        ((TextView) findViewById(R.id.receivedText)).setText(readMessage);
                        //Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_LONG).show();
                    }
                    /*if (msg.arg1 > 0) {
                        mainText += readMessage;
                    }*/
                /*case 1:
                    String readMessage2 = (String) msg.obj;
                    if (msg.arg1 > 0) {
                        mainText += readMessage2.substring(0, (int)readMessage2.length() - 1);
                    }
                    ((TextView) findViewById(R.id.receivedText)).setText(mainText);
                    Toast.makeText(getApplicationContext(), mainText, Toast.LENGTH_LONG).show();*/
            }
        }
    };
}


