package com.del.hotoil;

import static com.del.hotoil.Utils.error;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ConnectionManager {

    private final static String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int REQUEST_ENABLE_BT = 1;

    final private AppCompatActivity ownerActivity;
    private final ActivityResultLauncher<String[]> multipleBTPermissionsLauncher;
    private ConnectionListener listener;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;

    private String deviceName;

    @SuppressLint("InlinedApi")
    public ConnectionManager(AppCompatActivity ownerActivity) {
        this.ownerActivity = ownerActivity;
        this.multipleBTPermissionsLauncher = ownerActivity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean connectGranted = result.get(android.Manifest.permission.BLUETOOTH_CONNECT);
                    Boolean scanGranted = result.get(Manifest.permission.BLUETOOTH_SCAN);
                    if (connectGranted != null && connectGranted && scanGranted != null && scanGranted) {
                        tryToConnect();
                    } else {
                        Toast.makeText(ownerActivity, R.string.no_bt_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void tryToConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(ownerActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                multipleBTPermissionsLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                });
                return;
            }
        }

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.getBondedDevices().stream().
                        filter(d -> deviceName.trim().equalsIgnoreCase(d.getName().trim())).
                        findFirst().ifPresentOrElse(d -> {
                            try {
                                this.bluetoothDevice = d;
                                mmSocket = d.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING_WELL_KNOWN_SPP));
                                executor.execute(() -> {
                                    try {
                                        mmSocket.connect();
                                        mmInStream = mmSocket.getInputStream();
                                        mmOutStream = mmSocket.getOutputStream();
                                        if (listener != null)
                                            handler.post(() -> listener.onConnect(d.getName()));
                                    } catch (Exception e) {
                                        error(e);
                                        showMessageOnView(R.string.connection_fail);
                                        Utils.close(mmSocket);
                                    }
                                });
                            } catch (Exception e) {
                                error(e);
                                showMessageOnView(R.string.connection_fail);
                            }
                        }, () -> showMessageOnView(R.string.device_not_found));
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ownerActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

    }

    private void showMessageOnView(int m) {
        handler.post(() -> Toast.makeText(ownerActivity, m, Toast.LENGTH_LONG).show());
    }

    public void setListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public boolean begin(String name) {
        this.deviceName = name;
        if (!ownerActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(ownerActivity, R.string.no_bluetooth, Toast.LENGTH_LONG).show();
            return false;
        }
        BluetoothManager bluetoothManager = ownerActivity.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        tryToConnect();
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                tryToConnect();
            } else {
                Toast.makeText(ownerActivity, R.string.bluetooth_off, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void sendAndWaitResponse(Cmd cmd, Consumer<Response> cb, Consumer<Exception> err) {
        if (mmOutStream != null && cmd != null) {
            try {
                mmOutStream.write(cmd.getBytes());
                executor.execute(() -> {
                    Log.d(Utils.TAG, "wait response");
                    WaitScanner s = new WaitScanner(mmInStream);
                    s.waitLine(5).ifPresentOrElse(response -> {
                        Log.d(Utils.TAG, "get response " + response);
                        try {
                            Response r1 = new Response(response);
                            if (!r1.isError()) cb.accept(r1);
                            else err.accept(new Exception("Response ERROR"));
                        } catch (Exception e) {
                            err.accept(e);
                        }
                    }, () -> {
                        Log.d(Utils.TAG, "no response");
                    });
                });
            } catch (Exception e) {
                error(e);
                err.accept(e);
            }
        }
    }

    public void sendAndProcessResponse(String pack, Consumer<String> cb) {
        if (mmOutStream != null && pack != null) {
            try {
                mmOutStream.write(pack.getBytes(StandardCharsets.UTF_8));
                executor.execute(() -> {
                    Log.d(Utils.TAG, "process response");
                    WaitScanner s = new WaitScanner(mmInStream);
                    s.processWhile(5, cb);
                });
            } catch (Exception e) {
                error(e);
                Toast.makeText(ownerActivity, R.string.send_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public String getDeviceInfo() {
        return bluetoothDevice.getName();
    }

    public boolean isConnected() {
        return mmSocket != null && mmSocket.isConnected();
    }

    public void close() {
        this.bluetoothDevice = null;
        Utils.close(mmSocket);
        if (listener != null) listener.onDisconnect();
    }

}
