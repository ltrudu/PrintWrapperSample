package com.zebra.printwrappersample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.printwrapper.BluetoothPrinterDiscovery;
import com.zebra.printwrapper.ConnectToBluetoothPrinterTask;
import com.zebra.printwrapper.PrinterDiscoveryDataMapKeys;
import com.zebra.printwrapper.PrinterDiscoveryCallback;
import com.zebra.printwrapper.SelectedPrinterTaskCallbacks;
import com.zebra.printwrapper.SelectedPrinterTaskError;

import com.zebra.printwrapper.SendZPLTask;
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
    private DiscoveredPrinter selectedPrinter = null;
    private Map<String, String> selectedPrinterDiscoveryMap = null;

    /*
    Handler and runnable to scroll down textview
    */
    private Handler mScrollDownHandler = null;
    private Runnable mScrollDownRunnable = null;

    private TextView et_results;
    private ScrollView sv_results;
    private String mResults = "";

    private EditText et_macaddress;
    private EditText etZpl_to_send;

    private final String ms_zpltoSend = "^XA\n" +
            "^LH55,30\n" +
            "^FO20,10^CFD,27,13^FDZebra Technologies^FS\n" +
            "^FO20,60^AD^FDExemple Etiquette^FS\n" +
            "^FO40,160^BY2,2.0^BCN,100,Y,N,N,N^FD<PART,-1>^FS\n" +
            "^XZ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_results = (TextView)findViewById(R.id.et_results);
        sv_results = (ScrollView)findViewById(R.id.sv_results);
        et_macaddress = (EditText)findViewById(R.id.et_macaddress);
        etZpl_to_send = (EditText)findViewById(R.id.et_zpl);
        etZpl_to_send.setText(ms_zpltoSend);

        // Verify Permissions
        if (!permissionsGranted()) {
            // Request Permissions
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSIONS_REQUEST);
        }
        else
            requestBTPermissions();

        findViewById(R.id.button_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverBluetoothPrinters();
            }
        });



        findViewById(R.id.button_sendzpl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String zpltoSend = etZpl_to_send.getText().toString();
                if(zpltoSend.isEmpty())
                    zpltoSend = ms_zpltoSend;
                new SendZPLTask(zpltoSend, selectedPrinter, new SendZPLTask.SendZPLTaskCallback() {
                    @Override
                    public void onError(SendZPLTask.SendZPLTaskErrors error, String message) {

                    }

                    @Override
                    public void onSuccess() {

                    }
                }).execute();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        mScrollDownHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        if(mScrollDownRunnable != null)
        {
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            mScrollDownRunnable = null;
            mScrollDownHandler = null;
        }
        super.onPause();
    }

    private void addLineToResults(final String lineToAdd)
    {
        Log.d(TAG, lineToAdd);
        mResults += lineToAdd + "\n";
        updateAndScrollDownTextView();
    }

    private void updateAndScrollDownTextView() {
        if (mScrollDownRunnable == null) {
            mScrollDownRunnable = new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            et_results.setText(mResults);
                            sv_results.post(new Runnable() {
                                @Override
                                public void run() {
                                    sv_results.fullScroll(ScrollView.FOCUS_DOWN);
                                }
                            });
                        }
                    });
                }
            };
        } else {
            // A new line has been added while we were waiting to scroll down
            // reset handler to repost it....
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
        }
        if(mScrollDownHandler != null)
            mScrollDownHandler.postDelayed(mScrollDownRunnable, 300);
    }


    private void doDemoStuffs()
    {
        discoverBluetoothPrinters();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(selectedPrinter != null)
        {
            connectToPrinterBluetooth(selectedPrinter);
        }

    }


    private void discoverBluetoothPrinters()
    {
        addLineToResults("Starting bluetooth discovery to find requested printer.");
        final Map<String, DiscoveredPrinter> mymap = new HashMap<>();
        final String macAddress = getCleanedMacAddress(et_macaddress.getText().toString());
        PrinterDiscoveryCallback printerDiscoveryCallback = new PrinterDiscoveryCallback() {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                Map<String, String> discoveryDataMap = printer.getDiscoveryDataMap();
                String friendlyName = discoveryDataMap.get(PrinterDiscoveryDataMapKeys.FRIENDLY_NAME);
                String address = printer.address;

                mymap.put(address, printer);
                addLineToResults("PrinterDiscovered:" + address + " : " + friendlyName);

                if(address.equalsIgnoreCase(macAddress))
                {
                    addLineToResults("Found printer with address:" + macAddress);
                    addLineToResults("Selecting printer for demo.");
                    selectedPrinter = printer;
                    selectedPrinterDiscoveryMap = discoveryDataMap;
                    connectToPrinterBluetooth(selectedPrinter);
                }
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                if(printerList.size() != mymap.size())
                {
                    addLineToResults("Error, printerlist size differs from discovered size.");
                }
                else
                {
                    addLineToResults("Found:" + mymap.size() + " printers.");
                }
            }

            @Override
            public void onDiscoveryFailed(String message) {
                addLineToResults( "BTDiscovery failed:" + message);
            }
        };
        BluetoothPrinterDiscovery bluetoothPrinterDiscovery = new BluetoothPrinterDiscovery(this, printerDiscoveryCallback);
        bluetoothPrinterDiscovery.startDiscovery();
    }

    private void connectToPrinterBluetooth(DiscoveredPrinter selectedPrinter)
    {
        addLineToResults("Connecting to printer:" + selectedPrinter.address);
        SelectedPrinterTaskCallbacks selectedPrinterTaskCallbacks = new SelectedPrinterTaskCallbacks() {
            @Override
            public void onSuccess(DiscoveredPrinter printer, Map<String, String> printerDiscoveryMap) {
                addLineToResults("Successfully connected to printer: " + selectedPrinter.address);
            }

            @Override
            public void onError(SelectedPrinterTaskError error, String errorMessage) {
                addLineToResults("Error while trying to connect to printer:" + selectedPrinter.address);
                addLineToResults("Error:" + error.toString());
                addLineToResults("Error message:" + errorMessage);
            }
        };
        new ConnectToBluetoothPrinterTask(selectedPrinter, selectedPrinterTaskCallbacks, this).execute();

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
            addLineToResults( "Permissions Request Complete - checking permissions granted...");

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
                addLineToResults( "Classic Permissions Granted...");
                requestBTPermissions();
            } else {
                addLineToResults( "Permissions Denied - Exiting App" );

                // Explain reason
                Toast.makeText(this, "Please enable all permissions to run this app",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public int requestBTPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            int REQUEST_CODE = 1;

            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
            {

                ActivityCompat.requestPermissions(this,new String[] {android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_SCAN,android.Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_CODE);
                return 0;
            }
        }
        return 1;
    }//

    private String getCleanedMacAddress(String macAddress)
    {
        String cleanedMacAddress = macAddress.replace(":", "");
        StringBuilder formattedMacAddress = new StringBuilder();
        for (int i = 0; i < cleanedMacAddress.length(); i += 2) {
            formattedMacAddress.append(cleanedMacAddress.substring(i, i + 2));
            if (i < cleanedMacAddress.length() - 2) {
                formattedMacAddress.append(":");
            }
        }
        String finalMacAddress = formattedMacAddress.toString();
        return finalMacAddress;
    }
}