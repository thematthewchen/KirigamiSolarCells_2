package com.example.kirigamisolarcells_2;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.util.Set;
import java.util.ArrayList;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class MainActivity extends AppCompatActivity {

    Button btnStart, btnEnd, btnSend, btnClear;//Button variables for controls
    private BluetoothAdapter myBluetooth = null;    //BluetoothAdapter variable to control bluetooth
    private Set<BluetoothDevice> pairedDevices;                      //Set variable for list of connected devices
    ConnectThread connection;

    public MainActivity(){
        //devicelist = (ListView)findViewById(R.id.listView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStart = (Button)findViewById(R.id.buttonStart);
        btnEnd = (Button)findViewById(R.id.buttonStop);
        btnSend = (Button)findViewById(R.id.buttonSend);
        btnClear = (Button)findViewById(R.id.buttonClear);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                connect(device);
            }
        }
    };

    //Function allows ListView to be clicked
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
            //Intent i = new Intent(DeviceList.this, ledControl.class);
            //Change the activity.
            //i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            //startActivity(i);
        }
    };

    //Querying paired devices
    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();
        String deviceName;
        String deviceHardwareAddress;

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                connect(device);//connect the device
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        /*final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked*/

    }

    public void connect(BluetoothDevice device){
        String deviceName = device.getName();
        String deviceHardwareAddress = device.getAddress();
        connection = new ConnectThread(device);
        // Cancel discovery because it otherwise slows down the connection.
        myBluetooth.cancelDiscovery();
        connection.run();
    }

    //When 'Start' Button called
    public void onClickStart(View view) {
        if(myBluetooth == null)
        {
            //Show a message that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            finish();
        }
        else
        {
            if (!myBluetooth.isEnabled())
            {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);//REQUEST_ENABLE_BT = 1
            }
        }
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                pairedDevicesList(); //method that will be called
            }
        });
        //making the device discoverable
        /*Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);//add extra time
        startActivity(discoverableIntent);*/
    }

    //When 'End' Button called
    public void onClickEnd(View view) {
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                connection.cancel(); //method that will be called
            }
        });
    }

    //When 'Send' Button called
    public void onClickSend(View view) {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //connection.cancel(); //method that will be called
            }
        });
    }

    //When 'Clear' Button called
    public void onClickClear(View view) {
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //connection.cancel(); //method that will be called
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
}


