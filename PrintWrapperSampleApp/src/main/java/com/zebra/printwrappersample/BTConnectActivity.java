package com.zebra.printwrappersample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.zebra.printwrapper.GetPrinterStatusTask;
import com.zebra.printwrapper.PrinterDiscoveryDataMapKeys;
import com.zebra.printwrapper.PrinterDiscoveryCallback;

import com.zebra.printwrapper.PrinterWrapperException;
import com.zebra.printwrapper.SGDHelper;
import com.zebra.printwrapper.SendZPLTask;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
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

public class BTConnectActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 0;
    private static final int OPEN_FILE_REQUEST_CODE = 54561;
    private static final String[] PERMISSIONS = {
            ACCESS_FINE_LOCATION,
            INTERNET,
            ACCESS_NETWORK_STATE,
            ACCESS_WIFI_STATE
    };

    private static final String TAG = "PrintWSample";
    private DiscoveredPrinter selectedPrinter = null;
    private Map<String, String> selectedPrinterDiscoveryMap = null;

    /*
    Handler and runnable to scroll down textview
    */
    private Handler mScrollDownHandler = null;
    private Runnable mScrollDownRunnable = null;

    private TextView tv_results;
    private ScrollView sv_results;
    private String mResults = "";

    private EditText et_macaddress;
    private EditText etZpl_to_send;

    private SendZPLTask sendZPLTask = null;

    private final String m_defaultZpltoSend = "^XA\n" +
            "^LH55,30\n" +
            "^FO20,10^CFD,27,13^FDZebra Technologies^FS\n" +
            "^FO20,60^AD^FDExemple Etiquette^FS\n" +
            "^FO40,160^BY2,2.0^BCN,100,Y,N,N,N^FD<PART,-1>^FS\n" +
            "^XZ";

    //private final String m_defaultMacAddress = //"48A493EF6B7B";//"8CD54A10AF43"; //D4J242208881
    //private final String m_defaultMacAddress = "AC3FA4CE7931";
    private final String m_defaultMacAddress = "00074DBCE91C";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.bt_window_name);

        tv_results = (TextView)findViewById(R.id.tv_results);
        sv_results = (ScrollView)findViewById(R.id.sv_results);
        et_macaddress = (EditText)findViewById(R.id.et_macaddress);
        etZpl_to_send = (EditText)findViewById(R.id.et_zpl);

        if(mScrollDownHandler == null)
            mScrollDownHandler = new Handler(Looper.getMainLooper());

        // Verify Permissions
        if (!permissionsGranted()) {
            // Request Permissions
            requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST);
        }
        else
            requestBTPermissions();

        findViewById(R.id.button_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverBluetoothPrinters();
            }
        });

        findViewById(R.id.connecttcp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectTCPIP();
            }
        });

        findViewById(R.id.bruteforcebluetooth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bruteForceBluetooth();
            }
        });

        findViewById(R.id.button_sendzpl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendZPL();
            }

        });


        findViewById(R.id.button_writesettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToSettings();
            }
        });

        findViewById(R.id.button_readsettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFromSettings();
            }
        });

        findViewById(R.id.button_openzpl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                openFileIntent.setType("*/*");
                openFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                openFileIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(openFileIntent, OPEN_FILE_REQUEST_CODE);
            }
        });

        findViewById(R.id.button_getallcvs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAllCVs();
            }
        });

        findViewById(R.id.button_getstatus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPrinterStatus();
            }
        });

        findViewById(R.id.button_getlabelcount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLabelCount();
            }
        });

        findViewById(R.id.button_getserialnumber).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                getSerialNumber();
            }
        });

        getFromSettings();
    }

    private void getAllCVs() {
        if(selectedPrinter != null)
        {
            try {
                SGDHelper.GET("allcv", selectedPrinter, 15000, 15000, new SGDHelper.SGDHelperCallback() {
                    @Override
                    public void onMessage(String message) {
                        addLineToResults("**************************");
                        addLineToResults("******* GET ALL CVs ******");
                        addLineToResults(message);
                        addLineToResults("**************************");
                    }
                });
            } catch (PrinterWrapperException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getLabelCount() {
        if (selectedPrinter != null) {
            try {
                String user_label_count = SGDHelper.GET("odometer.user_label_count", selectedPrinter,message -> {
                    addLineToResults("Debug Log: " + message);
                });
                addLineToResults("******User Label Count******");
                addLineToResults("User Label Count :" + user_label_count);
            } catch (PrinterWrapperException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getSerialNumber() {
        if (selectedPrinter != null) {
            try {
                String serialNumber = SGDHelper.GET("device.unique_id", selectedPrinter,message -> {
                    addLineToResults("Debug Log: " + message);
                });
                addLineToResults("******Serial Number******");
                addLineToResults("Device serial number : " + serialNumber);

            } catch (PrinterWrapperException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void getPrinterStatus()
    {
        if(selectedPrinter != null)
        {
            GetPrinterStatusTask getPrinterStatusTask = new GetPrinterStatusTask();
            DiscoveredPrinter[] discoveredPrinters = new DiscoveredPrinter[]{ selectedPrinter };
            getPrinterStatusTask.setGetPrinterStatusTaskCallback(new GetPrinterStatusTask.GetPrinterStatusTaskCallback() {
                @Override
                public void onPrinterStatus(PrinterStatus status) {
                    addLineToResults("***************************");
                    addLineToResults("Current printer status:");
                    if (status.isReadyToPrint) {
                        addLineToResults("Ready To Print");
                    } else if (status.isPaused) {
                        addLineToResults("Cannot Print because the printer is paused.");
                    } else if (status.isHeadOpen) {
                        addLineToResults("Cannot Print because the printer head is open.");
                    } else if (status.isPaperOut) {
                        addLineToResults("Cannot Print because the paper is out.");
                    } else {
                        addLineToResults("Cannot Print.");
                    }
                }
            });
            getPrinterStatusTask.executeAsync(discoveredPrinters);

            /*
            addLineToResults("***************************");
            addLineToResults("Current printer status:");

            if(selectedPrinter.getConnection() != null ) {
                Connection connection = selectedPrinter.getConnection();
                if(connection.isConnected() == false) {
                    try {
                        connection.open();
                        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                        if(printer != null)
                        {
                            PrinterStatus printerStatus = printer.getCurrentStatus();
                            addLineToResults("***************************");
                            addLineToResults("Current printer status:");
                            if (printerStatus.isReadyToPrint) {
                                addLineToResults("Ready To Print");
                            } else if (printerStatus.isPaused) {
                                addLineToResults("Cannot Print because the printer is paused.");
                            } else if (printerStatus.isHeadOpen) {
                                addLineToResults("Cannot Print because the printer head is open.");
                            } else if (printerStatus.isPaperOut) {
                                addLineToResults("Cannot Print because the paper is out.");
                            } else {
                                addLineToResults("Cannot Print.");
                            }
                        }
                    } catch (ConnectionException e) {
                        throw new RuntimeException(e);
                    } catch (ZebraPrinterLanguageUnknownException e) {
                        throw new RuntimeException(e);
                    }
                    finally {
                        try {
                            connection.close();
                        } catch (ConnectionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
             */
        }
    }

    private void sendZPL() {
        String zpltoSend = etZpl_to_send.getText().toString();
        if (zpltoSend.isEmpty()) {
            zpltoSend = m_defaultZpltoSend;
            etZpl_to_send.setText(m_defaultZpltoSend);
        }

        if (selectedPrinter != null) {

            if(sendZPLTask == null) {
                addLineToResults("Sending ZPL to printer.");

                sendZPLTask = new SendZPLTask(zpltoSend, selectedPrinter, new SendZPLTask.SendZPLTaskCallback() {
                    @Override
                    public void onError(SendZPLTask.SendZPLTaskErrors error, String message) {
                        addLineToResults("Error while sending ZPL to printer: " + error.toString());
                        addLineToResults("Error message: message");
                        sendZPLTask = null;
                    }

                    @Override
                    public void onSuccess() {
                        addLineToResults("ZPL sent with success to printer.");
                        sendZPLTask = null;
                    }
                });
                sendZPLTask.executeAsync();
                }
            else {
                addLineToResults("A Send ZPL Task is already running, wait for it to finish.");
            }
            }
            else {
            addLineToResults("Please connect to a printer before sending ZPL");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mScrollDownHandler == null)
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                InputStream selectedFileInputStream = null;
                String str = "";
                StringBuffer buf = new StringBuffer();
                try {
                    selectedFileInputStream = getContentResolver().openInputStream(uri);
                    if(selectedFileInputStream != null)
                    {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(selectedFileInputStream));
                        if(bufferedReader != null) {
                            while ((str = bufferedReader.readLine()) != null) {
                                buf.append(str + "\n");
                            }
                            etZpl_to_send.setText(buf.toString());
                        }
                    }
                } catch (Exception e) {
                    addLineToResults("Exception while opening file:" + uri);
                    addLineToResults(e.getMessage());
                    e.printStackTrace();
                }
                finally {
                    if(selectedFileInputStream != null) {
                        try {
                            selectedFileInputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    private void getFromSettings()
    {
        addLineToResults("Getting data from settings.");
        SharedPreferences sharedpreferences = this.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String macAddress = sharedpreferences.getString(Constants.SHARED_PREFERENCES_MACADDRESS, null);
        String zplToSend = sharedpreferences.getString(Constants.SHARED_PREFERENCES_ZPL, null);

        if(macAddress == null || macAddress.isEmpty())
        {
            addLineToResults("No settings for MacAddress, using default.");
            macAddress = m_defaultMacAddress;
        }

        if(zplToSend == null || zplToSend.isEmpty())
        {
            addLineToResults("No settings for ZPL, using default.");
            zplToSend = m_defaultZpltoSend;
        }

        et_macaddress.setText(macAddress);
        etZpl_to_send.setText(zplToSend);
        addLineToResults("Settings read successfully");
    }

    private void writeToSettings()
    {
        addLineToResults("Writing data to settings.");
        SharedPreferences sharedpreferences = this.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        String macAddress = et_macaddress.getText().toString();
        if(macAddress.isEmpty())
            macAddress = m_defaultMacAddress;
        String zplToSend = etZpl_to_send.getText().toString();
        if(zplToSend.isEmpty())
            zplToSend = m_defaultZpltoSend;
        editor.putString(Constants.SHARED_PREFERENCES_MACADDRESS, macAddress);
        editor.putString(Constants.SHARED_PREFERENCES_ZPL, zplToSend);
        editor.commit();
        addLineToResults("Data successfully written to settings.");
    }

    private void addLineToResults(final String lineToAdd)
    {
        Log.d(TAG, lineToAdd);
        mResults += lineToAdd + "\n";
        updateAndScrollDownTextView();
    }

    private void addToResultWitoutRC(final String charsToAdd)
    {
        Log.d(TAG, charsToAdd);
        mResults += charsToAdd;
        updateAndScrollDownTextView();
    }

    private void updateAndScrollDownTextView() {
        if (mScrollDownRunnable == null) {
            mScrollDownRunnable = new Runnable() {
                @Override
                public void run() {
                    BTConnectActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_results.setText(mResults);
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
            if(mScrollDownHandler != null)
                mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
        }
        if(mScrollDownHandler != null)
            mScrollDownHandler.postDelayed(mScrollDownRunnable, 300);
    }

    private void connectTCPIP()
    {
        //Connection connection = new TcpConnection("192.168.1.139", TcpConnection.DEFAULT_ZPL_TCP_PORT, 2000, 500);
        new Thread(new Runnable() {
            @Override
            public void run() {
                addLineToResults("Initiating TcpIP connexion on not working IP");
                Date initDate = new Date();
                Connection connection = new TcpConnection("192.168.1.140", TcpConnection.DEFAULT_ZPL_TCP_PORT, 500, 500);
                try {
                    connection.open();
                } catch (ConnectionException e) {
                    addLineToResults("Exception trying to connect TcpIP:" + e.getMessage());
                }
                Date endDate = new Date();
                long differenceInMillis = endDate.getTime() - initDate.getTime();
                addLineToResults("Connection took:" + differenceInMillis + "ms");
            }
        }).start();
    }

    private void bruteForceBluetooth()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                addLineToResults("Initiating bluetooth connexion.");
                //BluetoothConnection btConnect = new BluetoothConnection("AC:3F:A4:CE:79:31", 2000, 2000);
                BluetoothConnection btConnect = new BluetoothConnection("00:07:4D:BC:E9:1C", 2000, 2000);
                for(int i = 0; i < 20; i++)
                {
                    try {
                        addLineToResults("Tryout number:" + i);
                        btConnect.open();
                        Thread.sleep(500);
                        btConnect.close();
                        Thread.sleep(500);
                        addLineToResults("Tryout " + i + " succeeded.");
                    } catch (Exception e) {
                        addLineToResults("Exception trying to connect:" + e.getMessage());
                        addLineToResults("Exception occured after:" + i + " tryouts.");
                        break;
                    }
                }
                try {
                    addLineToResults("Re Connecting and sending ZPL to Printer");
                    btConnect.open();
                    byte[] zplAsByte = m_defaultZpltoSend.getBytes();
                    btConnect.write(zplAsByte);
                    btConnect.close();
                    addLineToResults("ZPL Sent with success.");
                } catch (ConnectionException e) {
                    addLineToResults("Exception trying to connect:" + e.getMessage());
                }
            }
        }).start();

    }


    private void discoverBluetoothPrinters()
    {
        addLineToResults("**********************************");
        addLineToResults("Starting bluetooth discovery to find requested printer.");
        final Map<String, DiscoveredPrinter> mymap = new HashMap<>();

        // Get Mac address from edittext
        final String editMacAddress = et_macaddress.getText().toString();
        final String macAddress = getCleanedMacAddress(editMacAddress);
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
                    addLineToResults("Found requested printer with address:" + editMacAddress);
                    addLineToResults("Selecting requested printer for demo.");
                    selectedPrinter = printer;
                    selectedPrinterDiscoveryMap = discoveryDataMap;
                    addLineToResults("You are not forced to wait for discovery to finish.");
                    addLineToResults("You can send ZPL to the printer right now.");
                    addLineToResults("Waiting for discovery to finish.");
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
                    addLineToResults("Discovery ended.");
                    addLineToResults("Found:" + mymap.size() + " printers.");
                }
                addLineToResults("**********************************");
                if(selectedPrinter != null) {
                    addLineToResults("Requested printer was found:" + editMacAddress);
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