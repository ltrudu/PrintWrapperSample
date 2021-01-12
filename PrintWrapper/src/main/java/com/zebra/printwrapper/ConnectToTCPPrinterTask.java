package com.zebra.printwrapper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.comm.internal.ConnectionInfo;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.io.IOException;
import java.util.Map;

public class ConnectToTCPPrinterTask extends AsyncTask<Void, Boolean, Boolean> {

    private static final String TAG = "CONNECT_TCP_TASK";

    private Context context;
    private SelectedPrinterTaskCallbacks callback;
    private DiscoveredPrinter selectedPrinter = null;

    public ConnectToTCPPrinterTask(DiscoveredPrinter selectedPrinter, SelectedPrinterTaskCallbacks aCallback, Context aContext) {
        this.context = aContext;
        this.callback = aCallback;
        this.selectedPrinter = selectedPrinter;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        class TCPConnectionQuickClose extends TcpConnection {

            public TCPConnectionQuickClose(String address, int port) {
                super(address, port);
            }

            @Override
            public void close() throws ConnectionException {
                if (this.isConnected) {
                    this.isConnected = false;

                    try {
                        this.inputStream.close();
                        this.outputStream.close();
                        this.commLink.close();
                    } catch (IOException e) {
                        // Ugly... don't even know if it will be helpful or not...
                        if(callback != null)
                            callback.onError(SelectedPrinterTaskError.DEVICE_DISCONNECT_ERROR, context.getString(R.string.could_not_disconnect_device) + ":" + e.getLocalizedMessage());
                        throw new ConnectionException(context.getString(R.string.could_not_disconnect_device) + ":" + e.getMessage());
                    }
                }
            }
        }

        boolean result;

        try {
            Map<String, String> discoveryDataMap = selectedPrinter.getDiscoveryDataMap();
            int port = Integer.parseInt(discoveryDataMap.get(DiscoveryDataMapKeys.PORT_NUMBER));
            Connection connection = new TCPConnectionQuickClose(selectedPrinter.address, port);
            connection.open();
            try {
                ZebraPrinter zebraPrinter = ZebraPrinterFactory.getInstance(connection);
                boolean isPdfPrinter = isPDFEnabled(connection);
                if (!isPdfPrinter) {
                    String errorMessage = context.getString(R.string.wrong_firmware);
                    if(callback != null)
                        callback.onError(SelectedPrinterTaskError.WRONG_FIRMWARE, errorMessage);
                    connection.close();
                    return false;
                }
            }catch (ZebraPrinterLanguageUnknownException e)
            {
                if(callback != null)
                    callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, context.getString(R.string.open_connection_error));
                Log.e(TAG, context.getString(R.string.open_connection_error), e);
            };

            Map<String, String> discoveryMap = selectedPrinter.getDiscoveryDataMap();
            discoveryMap.put(DiscoveryDataMapKeys.PDF_ENABLED, isPDFEnabled(connection) ? "true" : "false");
            discoveryMap.put(DiscoveryDataMapKeys.CONNEXION_TYPE, DiscoveryDataMapKeys.CONNEXION_TYPE_NETWORK);
            result = true;

            connection.close();

        } catch (ConnectionException e) {
            Log.e(TAG, "Open connection error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, R.string.open_connection_error + ":" + e.getLocalizedMessage());
            result = false;
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean populateBluetoothDiscoDataSuccessful) {
        super.onPostExecute(populateBluetoothDiscoDataSuccessful);

        if (populateBluetoothDiscoDataSuccessful) {
            if(callback != null)
                callback.onSuccess(selectedPrinter);
        } else {
            // TODO: return resetConnectingStatus
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.RESET_CONNECTION, context.getString(R.string.reset_connection_status));
        }
    }

    // Checks the selected printer to see if it has the pdf virtual device installed.
    private boolean isPDFEnabled(Connection connection) {
        try {
            String printerInfo = SGD.GET("apl.enable", connection);
            if (printerInfo.equals("pdf")) {
                return true;
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        return false;
    }
}