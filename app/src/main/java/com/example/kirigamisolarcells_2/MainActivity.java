package com.example.kirigamisolarcells_2;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    //For Getting Current Location
    String latitude;
    String longitude;
    protected LocationManager locationManager;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;

    //Button variables for controls
    Button btnSend, locationbutton, pitchbutton;

    //For Bluetooth Connection
    private BluetoothAdapter myBluetooth = null;    //BluetoothAdapter variable to control bluetooth
    private Set<BluetoothDevice> pairedDevices;                      //Set variable for list of connected devices
    ConnectThread connection;
    ConnectedThread mConnectedThread;
    BluetoothDevice mDevice;
    private EditText userinput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //to get rid of title on MainActivity
        setContentView(R.layout.activity_main);

        /*
        Buttons
         */
        btnSend = (Button)findViewById(R.id.buttonSend);
        locationbutton = (Button)findViewById(R.id.khalid);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        /*
        Bluetooth
         */
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
        pairedDevicesList();

        /*
        Getting pitch of Phone (when placed on roof)
         */
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

        // listener needs to be registered with the
        // sensor manager along with the sensor wanted.
        setListners(sensorManager, mEventListener);

        //When the pitchbutton is pushed
        pitchbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                SensorManager.getRotationMatrix(mRotationMatrix, null, mValuesAccel, mValuesMagnet);
                SensorManager.getOrientation(mRotationMatrix, mValuesOrientation);
                String test;
                        //= "angle: " + "90" + "\0";
                test = "angle: " +mValuesOrientation[1] * (-180.0/3.141415926535);
                test = test.substring(0, 13) + "\0";
                mConnectedThread.write(test.getBytes());
                ((TextView) findViewById(R.id.receivedText)).setText(test);
            }
        });

        /*
        Location
         */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        checkLocation();
    }
    // Register the event listener and sensor type.
    public void setListners(SensorManager sensorManager, SensorEventListener mEventListener)
    {
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
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

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        startLocationUpdates();

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {
            latitude = String.valueOf(mLocation.getLatitude());
            longitude = String.valueOf(mLocation.getLongitude());
            String status = "location: " + latitude + "#" + longitude + "\0";
            ((TextView) findViewById(R.id.gpstext)).setText(status);
            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  }, MY_PERMISSION_ACCESS_COARSE_LOCATION);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("connectsus", "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("connectfail", "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
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
        locationbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //-83.73 long 42.28 lat
                String status = "location: " + "-083" + "#" + "042" + "\0";
                mConnectedThread.write(status.getBytes());
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        //txtLat = (TextView) findViewById(R.id.receivedText);
        //txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean checkLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

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
    * Separate Thread for Connecting Bluetooth
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
    * Once bluetooth connected, calls this class
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
            }
        }
    };
}


