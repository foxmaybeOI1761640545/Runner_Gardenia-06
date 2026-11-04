package de.dennisguse.opentracks.settings.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressLint("MissingPermission")
public class BluetoothLeAdapter extends BaseAdapter {

    /**
     * Contains a unique list (by address) of devices.
     */
    private final List<Device> devices = new ArrayList<>();

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return devices.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View currentView = convertView;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            //TODO Check if there is a better way to achieve identical look and feel to ListPreference.
            //Use material design single choice; for old style use: android.R.layout.select_dialog_singlechoice
            currentView = inflater.inflate(androidx.appcompat.R.layout.select_dialog_singlechoice_material, null);
        }

        Device device = devices.get(position);
        TextView textView = currentView.findViewById(android.R.id.text1);
        String detail = device.rssi != null ? parent.getContext().getString(R.string.sensor_scan_rssi, device.rssi) : "";
        if (device.connected) detail += (detail.isEmpty() ? "" : " · ") + parent.getContext().getString(R.string.sensor_scan_connected);
        textView.setSingleLine(false);
        textView.setMaxLines(3);
        String title=device.getNameOrAddress();
        if(!title.equals(device.address)&&android.bluetooth.BluetoothAdapter.checkBluetoothAddress(device.address))title+="\n"+device.address;
        textView.setText(title + (detail.isEmpty() ? "" : "\n" + detail));

        return currentView;
    }

    /**
     * @return Data changed?
     */
    public boolean add(String address, String name) { return add(address, name, null, false); }

    public boolean add(String address, String name, Integer rssi, boolean connected) {
        for (Device current : devices) {
            if (current.address.equals(address)) {
                if (name != null && !name.isBlank()) current.name = name;
                if (rssi != null) current.rssi = rssi;
                current.connected = connected;
                return true;
            }
        }
        Device device = new Device(address, name);
        device.rssi = rssi;
        device.connected = connected;
        devices.add(device);
        return true;
    }

    public void addAll(List<BluetoothDevice> bluetoothDevices) {
        // Do not use anyMatch: its short circuit drops all remaining scan results.
        for (BluetoothDevice device : bluetoothDevices) add(device.getAddress(), device.getName());
        if (!bluetoothDevices.isEmpty()) notifyDataSetChanged();
    }

    public Device get(int index) {
        return devices.get(index);
    }

    public static class Device {

        @NonNull
        private final String address;
        private String name;
        private Integer rssi;
        private boolean connected;

        Device(@NonNull String address, String name) {
            Objects.requireNonNull(address);
            this.address = address;
            this.name = name;
        }

        @NonNull
        public String getAddress() {
            return address;
        }

        public String getNameOrAddress() {
            return name != null && !name.isBlank() ? name : address;
        }

        public Integer getRssi() { return rssi; }
        public boolean isConnected() { return connected; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Device device)) return false;

            return address.equals(device.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }
    }
}
