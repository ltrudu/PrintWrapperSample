package com.zebra.printwrappersample;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;

import java.io.File;

public class sendPDFTest {
    public static void sendPDF(ZebraPrinter printer, File pdfFile) throws ConnectionException {

        PrinterStatus printerStatus = printer.getCurrentStatus();

        if(printerStatus.isReadyToPrint)
        {
            printer.sendFileContents(pdfFile.getAbsolutePath(), (bytesWritten, totalBytes) -> {
                // Calc Progress
                double rawProgress = bytesWritten * 100 / totalBytes;
                int progress = (int) Math.round(rawProgress);
            });
            //.... Rest of the code
        }
    }

}
