package com.sailab.mathexpressionscanner;

import static android.Manifest.permission_group.CAMERA;
import static android.Manifest.permission_group.STORAGE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.mariuszgromada.math.mxparser.Expression;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView expImg;
    private TextView expressionTxt, resultTxt;
    private ImageButton capture_btn, mediaBtn, calculatorBtn;
    private Bitmap imageBitmap;
    int CAM_PERMISSION_CODE = 200;
    int STORAGE_PERMISSION_CODE = 80;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expImg = findViewById(R.id.expImg);
        expressionTxt = findViewById(R.id.expressionTxt);
        resultTxt = findViewById(R.id.resultTxt);
        capture_btn = findViewById(R.id.captureBtn);
        mediaBtn = findViewById(R.id.mediaBtn);
        calculatorBtn = findViewById(R.id.calculatorBtn);

        capture_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkCamPermission()){
                    CaptureImage();
                } else {
                    reqCamPermission();
                }
            }
        });

        mediaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkGalleryPermission()) {
                    selectGalleryImage();
                } else {
                    reqGalleryPermission();
                }
            }
        });

        calculatorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent calculator = new Intent(getApplicationContext(), CalculatorActivity.class);
                startActivity(calculator);
            }
        });

    }

    private boolean checkCamPermission() {
        int cameraPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkGalleryPermission() {
        int galleryPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        return galleryPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void reqCamPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAM_PERMISSION_CODE);
    }

    private void reqGalleryPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    private void CaptureImage() {
        ImagePicker.with(this)
                .cameraOnly()
                .crop(16f, 6f)
                .compress(1024)
                .maxResultSize(480, 480)
                .start();
    }

    private void selectGalleryImage() {
        ImagePicker.with(this)
                .galleryOnly()
                .crop(16f, 6f)
                .compress(1024)
                .maxResultSize(480, 480)
                .start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == CAM_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (cameraPermission) {
                    CaptureImage();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied !", Toast.LENGTH_SHORT).show();
                }
            }
        }

        if(requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean galleryPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (galleryPermission) {
                    selectGalleryImage();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied !", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri resultUri = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            expImg.setImageBitmap(imageBitmap);
            DetectText();
        }
    }

    private void DetectText() {

        InputImage inputImage = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        Task<Text> result = textRecognizer.process(inputImage).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                StringBuilder result = new StringBuilder();

                for(Text.TextBlock block : text.getTextBlocks()) {

                    String blockText = block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();

                    for (Text.Line line : block.getLines()) {

                        String lineText = line.getText();
                        Point[] lineCornerPoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();

                        for (Text.Element element : line.getElements()) {
                            result.append(" ");
                            String elementText = element.getText();
                            Point[] elementPoint = element.getCornerPoints();
                            Rect elementRect = element.getBoundingBox();
                            result.append(elementText.toLowerCase(Locale.ROOT));
                        }
                    }
                }
                String detectTxt = String.valueOf(result);

                detectTxt = detectTxt.replaceAll("x", "*");
                detectTxt = detectTxt.replaceAll("÷", "/");

                Expression exp = new Expression(detectTxt);
                String resultExp = String.valueOf(exp.calculate());

                expressionTxt.setText(detectTxt);

                if(!resultExp.equals("NaN")) {
                    resultTxt.setText(resultExp);
                } else {
                    resultTxt.setText("");
                    showAlertBox();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Failed to Detect Expression", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlertBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder
                .setTitle("Math Recognition Problem")
                .setMessage("Couldn't able to recognize math expression." +"\n"+"\n" +
                        "Do you want to try again?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(checkCamPermission()) {
                            if(checkGalleryPermission()) {
                                ImagePicker.with(MainActivity.this)
                                        .crop(16f, 6f)
                                        .compress(1024)
                                        .maxResultSize(480, 480)
                                        .start();
                            } else reqGalleryPermission();
                        } else reqCamPermission();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }
}