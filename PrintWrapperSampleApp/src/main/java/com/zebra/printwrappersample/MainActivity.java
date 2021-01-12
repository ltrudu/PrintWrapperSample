package com.zebra.printwrappersample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.zebra.printwrapper.BluetoothPrinterDiscovery;
import com.zebra.printwrapper.ConnectToBluetoothPrinterTask;
import com.zebra.printwrapper.ConnectToTCPPrinterTask;
import com.zebra.printwrapper.DirectIPNetworkPrinterDiscovery;
import com.zebra.printwrapper.DiscoveryDataMapKeys;
import com.zebra.printwrapper.LocalNetworkPrinterDiscovery;
import com.zebra.printwrapper.PrinterDiscoveryCallback;
import com.zebra.printwrapper.SelectedPrinterTaskCallbacks;
import com.zebra.printwrapper.SelectedPrinterTaskError;
import com.zebra.printwrapper.SendPDFTask;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 0;
    private static final String[] PERMISSIONS = {
            ACCESS_FINE_LOCATION,
            WRITE_EXTERNAL_STORAGE,
            INTERNET,
            ACCESS_NETWORK_STATE,
            ACCESS_WIFI_STATE,
            READ_EXTERNAL_STORAGE
    };

    private static final String TAG = "PrintWSample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verify Permissions
        if (!permissionsGranted()) {
            // Request Permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST);
        }
        else
            doDemoStuffs();

    }

    private void doDemoStuffs()
    {
        discoverBluetoothPrinters();
    }

    private void discoverLocalNetwork()
    {
        final Map<String, DiscoveredPrinter> mymap = new HashMap<>();
        PrinterDiscoveryCallback printerDiscoveryCallback = new PrinterDiscoveryCallback() {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                Map<String, String> discoveryDataMap = printer.getDiscoveryDataMap();
                String friendlyName = discoveryDataMap.get(DiscoveryDataMapKeys.FRIENDLY_NAME);
                String address = printer.address;
                mymap.put(address, printer);
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                if(printerList.size() != mymap.size())
                {
                }
            }

            @Override
            public void onDiscoveryFailed(String message) {

            }
        };
        LocalNetworkPrinterDiscovery localNetworkPrinterDiscovery = new LocalNetworkPrinterDiscovery(this, printerDiscoveryCallback);
        localNetworkPrinterDiscovery.startDiscovery();
    }

    private void discoverBluetoothPrinters()
    {
        final Map<String, DiscoveredPrinter> mymap = new HashMap<>();
        PrinterDiscoveryCallback printerDiscoveryCallback = new PrinterDiscoveryCallback() {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                Map<String, String> discoveryDataMap = printer.getDiscoveryDataMap();
                String friendlyName = discoveryDataMap.get(DiscoveryDataMapKeys.FRIENDLY_NAME);
                String address = printer.address;

                mymap.put(address, printer);
                Log.d(TAG, "PrinterDiscovered:" + address + " : " + friendlyName);
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                if(printerList.size() != mymap.size())
                {
                    Log.d(TAG, "Error, printerlist size differs from discovered size.");
                }
                else
                {
                    Log.d(TAG, "Found:" + mymap.size() + " printers.");
                }
            }

            @Override
            public void onDiscoveryFailed(String message) {
                Log.e(TAG, "BTDiscovery failed:" + message);
            }
        };
        BluetoothPrinterDiscovery bluetoothPrinterDiscovery = new BluetoothPrinterDiscovery(this, printerDiscoveryCallback);
        bluetoothPrinterDiscovery.startDiscovery();
    }

    private void connectToPrinterBluetooth(DiscoveredPrinter selectedPrinter)
    {
        SelectedPrinterTaskCallbacks selectedPrinterTaskCallbacks = new SelectedPrinterTaskCallbacks() {
            @Override
            public void onSuccess(DiscoveredPrinter printer) {

            }

            @Override
            public void onError(SelectedPrinterTaskError error, String errorMessage) {

            }
        };
        new ConnectToBluetoothPrinterTask(selectedPrinter, selectedPrinterTaskCallbacks, this).execute();

    }

    private void connectToPrinterTCP(DiscoveredPrinter selectedPrinter)
    {
        SelectedPrinterTaskCallbacks selectedPrinterTaskCallbacks = new SelectedPrinterTaskCallbacks() {
            @Override
            public void onSuccess(DiscoveredPrinter printer) {
                Log.d("PrintWrapper", "SelectedPrinterTaskCallbacks Success connecting:" + printer.address);
                SendPDFTask.SendPDFTaskCallback callback = new SendPDFTask.SendPDFTaskCallback() {
                    @Override
                    public void onError(SendPDFTask.SendPDFTaskErrors error, String message) {
                        Log.d("PrintWrapper", "SendPDFTaskonError:" + error.toString() + "\n" + message);
                        switch(error)
                        {
                            case CONNECTION_ERROR:
                                break;
                            case HEAD_OPEN:
                                break;
                        }
                    }

                    @Override
                    public void onPrintProgress(String fileName, int progress, int bytesWritten, int totalBytes) {
                        Log.d("PrintWrapper", "SendPDFTaskonProgress:" + progress);
                    }

                    @Override
                    public void onSuccess() {
                        Log.d("PrintWrapper", "SendPDFTaskonSuccess");
                    }
                };
                new SendPDFTask("/sdcard/Download/etiquette.pdf", printer, callback).execute();
            }

            @Override
            public void onError(SelectedPrinterTaskError error, String errorMessage) {
                Log.d("PrintWrapper", "SelectedPrinterTaskCallbacks Error:" + error.toString() + "\n" + errorMessage);
            }
        };
        new ConnectToTCPPrinterTask(selectedPrinter, selectedPrinterTaskCallbacks, this).execute();
    }

    private void sendPDF()
    {
        DirectIPNetworkPrinterDiscovery discovery = new DirectIPNetworkPrinterDiscovery(this, "192.168.1.36", new PrinterDiscoveryCallback() {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                Log.d("PrintWrapper", "discovered:" + printer.address);
                Map<String, String> discoveryMap = printer.getDiscoveryDataMap();
                for(String key : discoveryMap.keySet())
                {
                    String value = discoveryMap.get(key);
                }
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                if(printerList.size() > 0) {
                    Log.d("PrintWrapper", "list size > 0");

                    DiscoveredPrinter printer = printerList.get(0);
                    connectToPrinterTCP(printer);
                }
            }

            @Override
            public void onDiscoveryFailed(String message) {
                Log.d("PrintWrapper", "onDiscoveryFailed");
            }
        });

        discovery.startDiscovery();
    }

    /**
     * Permissions Methods
     */

    private boolean permissionsGranted() {
        boolean permissionsGranted = true;
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }

        return permissionsGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        // Handle Permissions Request
        if (requestCode == PERMISSIONS_REQUEST) {
            Log.i(TAG, "Permissions Request Complete - checking permissions granted...");

            // Validate Permissions State
            boolean permissionsGranted = true;
            if (results.length > 0) {
                for (int result : results) {
                    if (result != PERMISSION_GRANTED) {
                        permissionsGranted = false;
                    }
                }
            } else {
                permissionsGranted = false;
            }

            // Check Permissions were granted & Load slide images or exit
            if (permissionsGranted) {
                Log.i(TAG, "Permissions Granted...");
                doDemoStuffs();
            } else {
                Log.e(TAG, "Permissions Denied - Exiting App");

                // Explain reason
                Toast.makeText(this, "Please enable all permissions to run this app",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}