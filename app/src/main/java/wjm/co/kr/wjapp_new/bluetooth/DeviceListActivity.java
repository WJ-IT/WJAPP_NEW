package wjm.co.kr.wjapp_new.bluetooth;

import android.Manifest;
import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
import android.content.Intent;
//import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.*;

import wjm.co.kr.wjapp_new.InventoryMenuActivity;
import wjm.co.kr.wjapp_new.R;

import java.util.Objects;
import java.util.Set;

public class DeviceListActivity extends Activity {
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
//        Button scanButton = (Button) findViewById(R.id.button_scan);
//        scanButton.setOnClickListener(v -> {
//            doDiscovery();
//            v.setVisibility(View.GONE);
//        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        // Member fields
        //private static BluetoothAdapter mBtAdapter;
        ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        ArrayAdapter<String> mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
//        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        this.registerReceiver(mReceiver, filter);

//        // Get the local Bluetooth adapter
//        WjmMain.BTadapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (D) Log.d(TAG, "Bluetooth Permission Check");
            return;
        }
        Set<BluetoothDevice> pairedDevices = Objects.requireNonNull(InventoryMenuActivity.bt_data.Companion.getBTadapter()).getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "페어링된 장치가 없습니다.";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
//        if (WjmMain.BTadapter != null) {
//            WjmMain.BTadapter.cancelDiscovery();
//        }

        // Unregister broadcast listeners
//        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
//    private void doDiscovery() {
//        if (D) Log.d(TAG, "doDiscovery()");
//
//        // Indicate scanning in the title
//        setProgressBarIndeterminateVisibility(true);
//        setTitle(R.string.scanning);
//
//        // Turn on sub-title for new devices
//        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
//
//        // If we're already discovering, stop it
//        if (InventoryMenuActivity.bt_data.Companion.getBTadapter().isDiscovering()) {
//            InventoryMenuActivity.bt_data.Companion.getBTadapter().cancelDiscovery();
//        }
//
//        // Request discover from BluetoothAdapter
//        InventoryMenuActivity.bt_data.Companion.getBTadapter().startDiscovery();
//    }

    // The on-click listener for all devices in the ListViews
    private final AdapterView.OnItemClickListener mDeviceClickListener = (av, v, arg2, arg3) -> {
        // Cancel discovery because it's costly and we're about to connect
//        InventoryMenuActivity.bt_data.Companion.getBTadapter().cancelDiscovery();
        // progressDialog.dismiss();
        // Get the device MAC address, which is the last 17 chars in the View
        String info = ((TextView) v).getText().toString();
        String address = info.substring(info.length() - 17);

        // Create the result Intent and include the MAC address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Log.d(TAG, "111");
//            // When discovery finds a device
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Get the BluetoothDevice object from the Intent
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                // If it's already paired, skip it, because it's been listed already
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                }
//                // When discovery is finished, change the Activity title
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
////                setProgressBarIndeterminateVisibility(false);
//                setTitle(R.string.select_device);
//                if (mNewDevicesArrayAdapter.getCount() == 0) {
//                    String noDevices = getResources().getText(R.string.none_found).toString();
//                    mNewDevicesArrayAdapter.add(noDevices);
//                }
//            }
//        }
//    };
}
