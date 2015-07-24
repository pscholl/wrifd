package es.uni_freiburg.de.wrifd;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import java.nio.ByteBuffer;
import java.util.UUID;

public class WrifdDevice {
    public static final String RFID_DETECTED = "RFID_DETECTED";
    public static final String RFID_DETECTED_UID_EXTRA = "RFID_DETECTED_UID_EXTRA";
    public static final String RFID_STATE_CHANGE = "RFID_STATE_CHANGE";
    public static final String RFID_STATE_CHANGE_EXTRA = "RFID_STATE_CHANGE_EXTRA";
    public static final int NO_BLUETOOTH = 0;
    public static final int BLUETOOTH_AVAIL = 1;
    public static final int CONNECTED = 2;


    protected final Service c;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothManager mBluetoothManager;

    public final static UUID UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
    public final static UUID UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
    public final static UUID UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
    public final static UUID UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
    public final static UUID UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

    public WrifdDevice(Service c) {
        this.c = c;
        onBluetooth();
    }

    protected void onBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothManager = (BluetoothManager) this.c.getSystemService(this.c.BLUETOOTH_SERVICE);

        if (mBluetoothAdapter==null) {
            log("Bluetooth not available");
            Intent i = new Intent(RFID_STATE_CHANGE);
            i.putExtra(RFID_STATE_CHANGE_EXTRA, NO_BLUETOOTH);
            WrifdDevice.this.c.sendBroadcast(i);
            return;
        }

        log("Bluetooth there: starting up");
        Intent i = new Intent(RFID_STATE_CHANGE);
        i.putExtra(RFID_STATE_CHANGE_EXTRA, BLUETOOTH_AVAIL);
        WrifdDevice.this.c.sendBroadcast(i);

        mBluetoothAdapter.startLeScan(onLeScan);
    }

    protected BluetoothAdapter.LeScanCallback onLeScan = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int i, byte[] bytes) {
            if (device.getName() == null)
                return;

            log("found device " + device.getName() + " " + device.getAddress());

            if (!device.getName().contains("wrifd"))
                return;

            mBluetoothAdapter.stopLeScan(onLeScan);
            //mBluetoothGatt =
            mBluetoothAdapter.getRemoteDevice(device.getAddress())
                    .connectGatt(WrifdDevice.this.c, false, onGatt);
        }
    };

    private BluetoothGattService mBluetoothGattService;
    protected BluetoothGattCallback onGatt = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                Intent i = new Intent(RFID_STATE_CHANGE);
                i.putExtra(RFID_STATE_CHANGE_EXTRA, CONNECTED);
                WrifdDevice.this.c.sendBroadcast(i);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.disconnect();
                gatt.close();
                mBluetoothAdapter.startLeScan(onLeScan);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log("onServicesDiscover: " + status);

            if (status != BluetoothGatt.GATT_SUCCESS)
                return;

            mBluetoothGattService = gatt.getService(UUID_SERVICE);
            if (mBluetoothGattService == null) {
                log("RFduino GATT service not found!");
                return;
            }

            BluetoothGattCharacteristic receiveCharacteristic =
                    mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
            if (receiveCharacteristic != null) {
                BluetoothGattDescriptor receiveConfigDescriptor =
                        receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                if (receiveConfigDescriptor != null) {
                    gatt.setCharacteristicNotification(receiveCharacteristic, true);

                    receiveConfigDescriptor.setValue(
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(receiveConfigDescriptor);
                } else {log("RFduino receive config descriptor not found!");}
            } else {log("RFduino receive characteristic not found!");}
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c) {
            byte rfid_uid[] = c.getValue();

            String s = ""; // to HEX string
            for (int i=0; i<rfid_uid.length; i++)
                s += String.format("%02X:", rfid_uid[i]);
            s = s.substring(0,s.length()-1);

            Intent i = new Intent(RFID_DETECTED);
            i.putExtra(RFID_DETECTED_UID_EXTRA, s);
            WrifdDevice.this.c.sendBroadcast(i);
        }
    };

    private void log(String s) {
        Log.d(this.getClass().getName(), s);
    }
}
