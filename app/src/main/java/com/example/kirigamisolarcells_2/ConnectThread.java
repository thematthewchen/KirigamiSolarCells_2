package com.example.kirigamisolarcells_2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by thema on 10/2/2017.
 */

public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice. This code below show how to do it and handle the case that the UUID from the device is not found and trying a default UUID.

        // Default UUID
        UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            // Use the UUID of the device that discovered // TODO Maybe need extra device object
            if (mmDevice != null)
            {
                Log.i("NameDevice", "Device Name: " + mmDevice.getName());
                Log.i("UUIDDevice", "Device UUID: " + mmDevice.getUuids()[0].getUuid());
                tmp = device.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());

            }
            else Log.d("DeviceNull", "Device is null.");
        }
        catch (NullPointerException e)
        {
            Log.d("UUIDNull", " UUID from device is null, Using Default UUID, Device name: " + device.getName());
            try {
                tmp = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        catch (IOException e) {
            Log.e("CreateSocket", "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
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
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        //manageMyConnectedSocket(mmSocket);
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