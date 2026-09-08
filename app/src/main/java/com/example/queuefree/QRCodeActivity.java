package com.example.queuefree;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QRCodeActivity - Generate QR code for token
 * Admin can scan to mark as served
 */
public class QRCodeActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private TextView tokenInfoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        tokenInfoTextView = findViewById(R.id.tokenInfoTextView);

        String tokenNumber = getIntent().getStringExtra("TOKEN_NUMBER");
        String serviceCenter = getIntent().getStringExtra("SERVICE_CENTER");

        tokenInfoTextView.setText("Token #" + tokenNumber + "\n" + serviceCenter);

        // Generate QR Code
        String qrData = "QUEUEFREE:" + serviceCenter + ":" + tokenNumber;
        generateQRCode(qrData);
    }

    private void generateQRCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            qrCodeImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
