package test.beeloggerbluetooth;

import android.Manifest;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Menu;
import android.widget.ListView;

import androidx.core.app.ActivityCompat;

import test.beeloggerbluetooth.databinding.FragmentMainBinding;

public class MyReceiver extends BroadcastReceiver {
    private final FragmentMainBinding binding;
    private Menu menu;
    private BluetoothDevice device;
    private DeviceListAdapter pairedDevicesArrayAdapter;
    private ListView lv_BtDevices;
    private int initHeight;

    public MyReceiver(FragmentMainBinding binding) {
        this.binding = binding;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                if (menu != null) {
                    menu.findItem(R.id.action_bt).setIcon(R.drawable.my_bluetooth_disabled);
                }
            } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                if (menu != null) {
                    menu.findItem(R.id.action_bt).setIcon(R.drawable.my_bluetooth);
                }
            }
        }

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (device != null && intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE).equals(device)) {
                binding.btConnect.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.my_bluetooth_connected), null, null, null);
                binding.btConnect.setTextColor(Color.BLUE);
                binding.btConnect.setText("DISCONNECT");
                binding.btListBtDevices.setEnabled(false);

                for (int i = lv_BtDevices.getAdapter().getCount() - 1; i >= 0; i--) {
                    String lv_item = pairedDevicesArrayAdapter.getItem(i).getName();
                    if (!lv_item.contains(device.getName())) {
                        pairedDevicesArrayAdapter.remove(pairedDevicesArrayAdapter.getItem(i));

                    }
                }
                lv_BtDevices.getLayoutParams().height = ActionBar.LayoutParams.WRAP_CONTENT;
                lv_BtDevices.setSelection(0);
                lv_BtDevices.requestLayout();
            }
        }

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (device != null && intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE).equals(device)) {
                binding.btConnect.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.my_bluetooth_notconnected), null, null, null);
                binding.btConnect.setTextColor(Color.LTGRAY);
                binding.btConnect.setText("CONNECT");
                binding.btListBtDevices.setEnabled(true);
                binding.btSendFn.setEnabled(false);
                binding.btSendData.setEnabled(false);
                binding.btSendNf.setEnabled(false);
                pairedDevicesArrayAdapter.clear();
                lv_BtDevices.getLayoutParams().height = initHeight;

            }
        }
    }

    public void setMenu(Menu menu) {
        if (menu != null) {
            this.menu = menu;
        }
    }

    public void setInitHeight(int initHeight) {
        if (initHeight != 0) {
            this.initHeight = initHeight;
        }
    }

    public void setBTDevice(BluetoothDevice device) {
        if (device != null) {
            this.device = device;
        }
    }

    public void setPairedDeviceAdapter(DeviceListAdapter pairedDevicesArrayAdapter, ListView lv_BtDevices) {
        if (pairedDevicesArrayAdapter != null) {
            this.pairedDevicesArrayAdapter = pairedDevicesArrayAdapter;
            this.lv_BtDevices = lv_BtDevices;
        }
    }

}
