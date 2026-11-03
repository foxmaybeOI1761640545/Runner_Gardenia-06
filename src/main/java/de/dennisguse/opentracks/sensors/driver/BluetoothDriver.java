/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.sensors.driver;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.sensors.BluetoothUtils;
import de.dennisguse.opentracks.sensors.ServiceMeasurementUUID;

/** One selected BLE sensor. All GATT callbacks are checked against the live transport and session. */
@SuppressLint("MissingPermission")
public class BluetoothDriver<T> implements Driver {
    private static final String TAG = BluetoothDriver.class.getSimpleName();
    public static final String RECONNECT_SUFFIX = ".RECONNECT_BLE_SENSOR";
    public static final String DISCONNECT_SUFFIX = ".DISCONNECT_BLE_SENSOR";
    private final DriverObserver<T> observer;
    private final BluetoothParser<T> parser;
    private final BluetoothAdapter adapter;
    private volatile BluetoothGatt gatt;
    private volatile BleConnectionSession session;
    private BluetoothGattCharacteristic measurement;
    private ServiceMeasurementUUID service;
    private Context context;
    private Handler handler;
    private String address;
    private int openedGeneration;
    private int optionalIndex;
    private BluetoothGattCharacteristic optionalRead;
    private Runnable optionalTimeout;
    private boolean receiversRegistered;

    public BluetoothDriver(BluetoothAdapter adapter, DriverObserver<T> observer, BluetoothParser<T> parser) {
        this.adapter = adapter;
        this.observer = observer;
        this.parser = parser;
    }

    private final BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context unused, Intent intent) {
            BleConnectionSession current = session;
            if (current == null) return;
            synchronized (current) {
                try {
                    if (adapter != null && adapter.isEnabled()) current.bluetoothOn();
                    else current.bluetoothOff();
                } catch (SecurityException denied) { current.permissionDenied(); }
            }
        }
    };
    private final BroadcastReceiver reconnectReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context unused, Intent intent) {
            BleConnectionSession current = session;
            List<String> measurements = intent.getStringArrayListExtra("measurements");
            if (measurements != null && parser.getServices().stream().noneMatch(s -> measurements.contains(s.measurementUUID().toString()))) return;
            String measurement = intent.getStringExtra("measurement");
            if (measurement != null && parser.getServices().stream().noneMatch(s -> s.measurementUUID().toString().equals(measurement))) return;
            if (current != null && address != null && address.equals(intent.getStringExtra("address"))) {
                if ((unused.getPackageName()+DISCONNECT_SUFFIX).equals(intent.getAction())) current.stop();
                else current.reconnect();
            }
        }
    };

    @Override public synchronized void connect(Context suppliedContext, Handler suppliedHandler, @NonNull String selectedAddress) {
        if (session != null && address != null && address.equals(selectedAddress)
                && session.state() != BleConnectionSession.State.DISCONNECTED) return;
        disconnect();
        context = suppliedContext.getApplicationContext();
        handler = suppliedHandler;
        address = selectedAddress;
        Handler owner = suppliedHandler;
        session = new BleConnectionSession(new BleConnectionSession.Transport() {
            @Override public boolean open(int generation) {
                openedGeneration = generation;
                parser.resetOptionalMetadata();
                try {
                    if (adapter == null || !adapter.isEnabled()) { session.bluetoothOff(); return false; }
                    BluetoothDevice device = adapter.getRemoteDevice(address);
                    gatt = device.connectGatt(context, false, callbacks, BluetoothDevice.TRANSPORT_LE,
                            BluetoothDevice.PHY_LE_1M_MASK, owner);
                    return gatt != null;
                } catch (SecurityException denied) { session.permissionDenied(); return false; }
                catch (IllegalArgumentException invalidAddress) { session.unavailable(); return false; }
                catch (IllegalStateException poweredOff) { session.bluetoothOff(); return false; }
            }
            @Override public boolean discover() {
                try { return gatt != null && gatt.discoverServices(); }
                catch (SecurityException denied) { session.permissionDenied(); return false; }
            }
            @Override public boolean subscribe() {
                try {
                    if (gatt == null || measurement == null) return false;
                    byte[] subscription = subscriptionValue(measurement.getProperties());
                    if (subscription == null) return false;
                    BluetoothGattDescriptor descriptor = measurement.getDescriptor(BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_UUID);
                    if (descriptor == null || !gatt.setCharacteristicNotification(measurement, true)) return false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        return gatt.writeDescriptor(descriptor, subscription) == BluetoothStatusCodes.SUCCESS;
                    }
                    return descriptor.setValue(subscription) && gatt.writeDescriptor(descriptor);
                } catch (SecurityException denied) { session.permissionDenied(); return false; }
            }
            @Override public void close() { closeGatt(); }
        }, new BleConnectionSession.Scheduler() {
            @Override public void schedule(Runnable task, long delayMillis) { owner.postDelayed(task, delayMillis); }
            @Override public void cancel(Runnable task) { owner.removeCallbacks(task); }
        }, new BleConnectionSession.Listener() {
            @Override public void stateChanged(BleConnectionSession.State state) { observer.onConnectionStateChanged(state); }
            @Override public void ready() {
                try {
                    observer.onConnected(address, gatt.getDevice().getName());
                    optionalIndex = 0;
                    readNextOptional();
                } catch (SecurityException denied) { session.permissionDenied(); }
            }
            @Override public void lost() { observer.onConnectionLost(); }
            @Override public void stopped() { observer.onDisconnected(); }
        });
        ContextCompat.registerReceiver(context, powerReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED);
        IntentFilter commands = new IntentFilter(context.getPackageName() + RECONNECT_SUFFIX);
        commands.addAction(context.getPackageName() + DISCONNECT_SUFFIX);
        ContextCompat.registerReceiver(context, reconnectReceiver, commands, ContextCompat.RECEIVER_NOT_EXPORTED);
        receiversRegistered = true;
        session.start();
    }

    private final BluetoothGattCallback callbacks = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt callbackGatt, int status, int newState) {
            BleConnectionSession current = session;
            if (current == null) return;
            synchronized (current) {
                if (gatt != callbackGatt) return;
                if (newState == BluetoothProfile.STATE_CONNECTED) current.connected(openedGeneration, status == BluetoothGatt.GATT_SUCCESS);
                else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) current.disconnected(openedGeneration);
            }
        }
        @Override public void onServicesDiscovered(BluetoothGatt callbackGatt, int status) {
            BleConnectionSession current = session;
            if (current == null) return;
            synchronized (current) {
                if (gatt != callbackGatt) return;
                service = null;
                measurement = null;
                for (ServiceMeasurementUUID candidate : parser.getServices()) {
                    BluetoothGattService found = callbackGatt.getService(candidate.serviceUUID());
                    if (found != null && found.getCharacteristic(candidate.measurementUUID()) != null) {
                        service = candidate;
                        measurement = found.getCharacteristic(candidate.measurementUUID());
                        break;
                    }
                }
                current.servicesDiscovered(openedGeneration, status == BluetoothGatt.GATT_SUCCESS && measurement != null);
            }
        }
        @Override public void onDescriptorWrite(BluetoothGatt callbackGatt, BluetoothGattDescriptor descriptor, int status) {
            BleConnectionSession current = session;
            if (current == null) return;
            synchronized (current) {
                if (gatt != callbackGatt || measurement == null || descriptor.getCharacteristic() != measurement
                        || !BluetoothUtils.CLIENT_CHARACTERISTIC_CONFIG_UUID.equals(descriptor.getUuid())) return;
                current.subscribed(openedGeneration, status == BluetoothGatt.GATT_SUCCESS);
            }
        }
        @Override public void onCharacteristicChanged(BluetoothGatt callbackGatt, BluetoothGattCharacteristic characteristic) {
            receive(callbackGatt, characteristic, characteristic.getValue());
        }
        @Override public void onCharacteristicChanged(@NonNull BluetoothGatt callbackGatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            receive(callbackGatt, characteristic, value);
        }
        @Override public void onCharacteristicRead(BluetoothGatt callbackGatt, BluetoothGattCharacteristic characteristic, int status) {
            optionalResult(callbackGatt, characteristic, characteristic.getValue(), status);
        }
        @Override public void onCharacteristicRead(@NonNull BluetoothGatt callbackGatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            optionalResult(callbackGatt, characteristic, value, status);
        }
    };

    private void receive(BluetoothGatt callbackGatt, BluetoothGattCharacteristic characteristic, byte[] bytes) {
        BleConnectionSession current = session;
        if (current == null) return;
        synchronized (current) {
            if (gatt != callbackGatt || current.state() != BleConnectionSession.State.CONNECTED
                    || measurement != characteristic || bytes == null || service == null) return;
            try {
                T value = parser.parsePayload(service, callbackGatt.getDevice().getName(), characteristic, bytes.clone());
                if (value != null) observer.onDataReceived(value);
            } catch (SecurityException denied) { current.permissionDenied(); }
        }
    }

    private void readNextOptional() {
        while (gatt != null && optionalIndex < parser.getOptionalReads().size()) {
            ServiceMeasurementUUID next = parser.getOptionalReads().get(optionalIndex++);
            BluetoothGattService found = gatt.getService(next.serviceUUID());
            optionalRead = found != null ? found.getCharacteristic(next.measurementUUID()) : null;
            if (optionalRead == null || (optionalRead.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) continue;
            try {
                if (!gatt.readCharacteristic(optionalRead)) continue;
            } catch (SecurityException denied) { session.permissionDenied(); return; }
            BleConnectionSession current = session;
            int token = openedGeneration;
            optionalTimeout = () -> {
                synchronized (current) {
                    if (current == session && current.generation() == token && current.state() == BleConnectionSession.State.CONNECTED) {
                        optionalRead = null;
                        readNextOptional();
                    }
                }
            };
            handler.postDelayed(optionalTimeout, 5000);
            return;
        }
        optionalRead = null;
    }

    private void optionalResult(BluetoothGatt callbackGatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        BleConnectionSession current = session;
        if (current == null) return;
        synchronized (current) {
            if (gatt != callbackGatt || characteristic != optionalRead || current.state() != BleConnectionSession.State.CONNECTED) return;
            if (optionalTimeout != null) handler.removeCallbacks(optionalTimeout);
            optionalTimeout = null;
            if (status == BluetoothGatt.GATT_SUCCESS) parser.onOptionalRead(characteristic.getUuid(), value);
            optionalRead = null;
            readNextOptional();
        }
    }

    private void closeGatt() {
        if (optionalTimeout != null && handler != null) handler.removeCallbacks(optionalTimeout);
        optionalTimeout = null;
        optionalRead = null;
        measurement = null;
        service = null;
        BluetoothGatt closing = gatt;
        gatt = null; // invalidate callback identity before invoking framework teardown
        if (closing != null) {
            try { closing.disconnect(); }
            catch (SecurityException denied) { Log.i(TAG, "Bluetooth permission revoked during disconnect"); }
            finally {
                try { closing.close(); }
                catch (SecurityException denied) { Log.i(TAG, "Bluetooth permission revoked during close"); }
            }
        }
    }

    @Override public synchronized void disconnect() {
        if (session != null) session.stop();
        if (receiversRegistered) {
            context.unregisterReceiver(powerReceiver);
            context.unregisterReceiver(reconnectReceiver);
            receiversRegistered = false;
        }
        context = null;
        handler = null;
    }
    @Override public boolean isConnected() { return session != null && session.state() == BleConnectionSession.State.CONNECTED; }

    public static byte[] subscriptionValue(int properties) {
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) return BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE.clone();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) return BluetoothGattDescriptor.ENABLE_INDICATION_VALUE.clone();
        return null;
    }

    public interface BluetoothParser<T> {
        List<ServiceMeasurementUUID> getServices();
        T parsePayload(ServiceMeasurementUUID service, String sensorName, BluetoothGattCharacteristic characteristic);
        default T parsePayload(ServiceMeasurementUUID service, String name, BluetoothGattCharacteristic characteristic, byte[] bytes) {
            characteristic.setValue(bytes);
            return parsePayload(service, name, characteristic);
        }
        default List<ServiceMeasurementUUID> getOptionalReads() { return List.of(); }
        default void onOptionalRead(UUID characteristic, byte[] value) {}
        default void resetOptionalMetadata() {}
    }
}
