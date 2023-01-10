package test.beeloggerbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.Menu;

import test.beeloggerbluetooth.databinding.FragmentSecondBinding;

public class MyReceiver extends BroadcastReceiver {
    private final FragmentSecondBinding binding;
    private Menu menu;

    public MyReceiver(FragmentSecondBinding binding) {
        this.binding = binding;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                binding.btBT.setTextColor(Color.LTGRAY);
                if (menu != null) {
                    menu.findItem(R.id.action_bt).setIcon(R.drawable.my_bluetooth_disabled); //TODO init State
                }
            } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                binding.btBT.setTextColor(Color.BLUE);
                if (menu != null) {
                    menu.findItem(R.id.action_bt).setIcon(R.drawable.my_bluetooth); //TODO init State
                }
            }
        }
        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                binding.btBT.setTextColor(Color.LTGRAY);
            } else if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothAdapter.STATE_ON) {
                binding.btBT.setTextColor(Color.BLUE);
            }
        }
    }

    public void setMenu(Menu menu) {
        if (menu != null) {
            this.menu = menu;
        }
    }

}
