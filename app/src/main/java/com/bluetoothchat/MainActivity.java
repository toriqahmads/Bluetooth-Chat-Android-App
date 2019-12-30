package com.bluetoothchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private TextView status;
    private Button pairedDevice;
    private Button discoverDevice;
    private Button btnSend;
    private ListView listView;
    private Switch switchBt;
    private  TextView status_bluetooth;
    private Dialog dialog;
    private TextInputLayout inputLayout;
    private ChatArrayAdapter chatAdapter;
    private ArrayList<String> chatMessage;
    private BluetoothAdapter bluetoothAdapter;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private ChatSwitch chatSwitch;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private boolean side = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status);
        pairedDevice = (Button) findViewById(R.id.paired_device);
        discoverDevice = (Button) findViewById(R.id.discover_device);
        switchBt = (Switch) findViewById(R.id.switchBT);
        listView = (ListView) findViewById(R.id.list);
        inputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        btnSend = (Button) findViewById(R.id.btn_send);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        chatMessage = new ArrayList<>();
        chatAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.chat_right);
        listView.setAdapter(chatAdapter);

        switchBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(bluetoothAdapter == null){
                        Toast.makeText(getApplicationContext(), "Perangkat bluetooth tidak terdukung!", Toast.LENGTH_SHORT).show();
                    }else {
                        if (!bluetoothAdapter.isEnabled()) {
                            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
                        }
                    }
                }else{
                    bluetoothAdapter.disable();
                    setStatus("Tidak terhubung");
                    switchBt.setChecked(false);
                    pairedDevice.setEnabled(false);
                    discoverDevice.setEnabled(false);
                }
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatAdapter);

        chatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatAdapter.getCount() - 1);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(inputLayout.getEditText().getText().toString().equals("")){
                    Toast.makeText(MainActivity.this, "Mohon masukkan pesan", Toast.LENGTH_SHORT).show();
                }else{
                    sendMessage(inputLayout.getEditText().getText().toString());
                    inputLayout.getEditText().setText("");
                }
            }
        });

        pairedDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPairedDialog();
            }
        });

        discoverDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDiscoveredDialog();
            }
        });
    }

    private boolean sendChatMessage(boolean side, String message) {
        chatAdapter.add(new ChatMessage(side, message));
        return true;
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case ChatSwitch.STATE_CONNECTED:
                            setStatus("Terhubung ke: " + connectingDevice.getName());
                            inputLayout.setEnabled(true);
                            btnSend.setEnabled(true);
                            break;
                        case ChatSwitch.STATE_CONNECTING:
                            setStatus("Menghubungkan...");
                            inputLayout.setEnabled(false);
                            btnSend.setEnabled(false);
                            break;
                        case ChatSwitch.STATE_LISTEN:
                        case ChatSwitch.STATE_NONE:
                            setStatus("Tidak terhubung");
                            inputLayout.setEnabled(false);
                            btnSend.setEnabled(false);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    sendChatMessage(true, writeMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    sendChatMessage(false, readMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Terhubung ke: " + connectingDevice.getName(), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void showDiscoveredDialog(){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.discovered_bluetooth_layout);
        dialog.setTitle("Perangkat Bluetooth Terjangkau");

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(discoveredDevicesAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter2);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                IntentFilter filters = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(discoveryFinishReceiver, filters);
                try{
                    createBond(device);
                } catch (Exception e){
                    connectToDevice(address);
                }
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showPairedDialog(){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.paired_bluetooth_layout);
        dialog.setTitle("Perangkat Bluetooth Terpasang");
        ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        listView.setAdapter(pairedDeviceAdapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        if(pairedDevice.size() > 0){
            Integer ind = pairedDeviceAdapter.getPosition("Tidak ada perangkat terpasang!");
            if(ind != -1){
                pairedDeviceAdapter.remove("Tidak ada perangkat terpasang!");
            }
            for(BluetoothDevice device : pairedDevice){
                pairedDeviceAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }else{
            pairedDeviceAdapter.add("Tidak ada perangkat terpasang!");
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {
        if(BluetoothAdapter.checkBluetoothAddress(deviceAddress)){
            bluetoothAdapter.cancelDiscovery();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            chatSwitch.connect(device);
        }else{
            Toast.makeText(getApplicationContext(), "Alamat perangkat tidak valid", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK){
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
                    chatSwitch = new ChatSwitch(this, handler);
                    switchBt.setChecked(true);
                    pairedDevice.setEnabled(true);
                    discoverDevice.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Bluetooth dihidupkan", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Bluetooth dimatikan!", Toast.LENGTH_SHORT).show();
                    setStatus("Tidak terhubung");
                    switchBt.setChecked(false);
                    pairedDevice.setEnabled(false);
                    discoverDevice.setEnabled(false);
                    Toast.makeText(this, "Bluetooth belum diaktifkan. Mohon nyalakan bluetooth untuk menggunakan aplikasi!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    public boolean createBond(BluetoothDevice device) throws Exception{
        Class bt = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = bt.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    private void sendMessage(String message) {
        if(chatSwitch.getState() != ChatSwitch.STATE_CONNECTED){
            Toast.makeText(this, "Koneksi terputus!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(message.length() > 0){
            byte[] send = message.getBytes();
            chatSwitch.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            switchBt.setChecked(false);
            pairedDevice.setEnabled(false);
            discoverDevice.setEnabled(false);
            inputLayout.setEnabled(false);
            btnSend.setEnabled(false);
        } else {
            switchBt.setChecked(true);
            pairedDevice.setEnabled(true);
            discoverDevice.setEnabled(true);
            chatSwitch = new ChatSwitch(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chatSwitch != null) {
            if (chatSwitch.getState() == ChatSwitch.STATE_NONE) {
                chatSwitch.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatSwitch != null)
            chatSwitch.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    Toast.makeText(getApplicationContext(), "Tidak ada perangkat ditemukan!", Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    connectToDevice(device.getAddress());
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), "Gagal memasang perangkat", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
}
