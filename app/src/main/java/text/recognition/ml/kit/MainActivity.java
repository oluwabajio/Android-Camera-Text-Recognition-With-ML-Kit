package text.recognition.ml.kit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {


    private SurfaceHolder surfaceViewHolder;
    private SurfaceView surfaceView;
    private ImageView imageView;
    private Button btnTakePicture;
    private TextView textView;
    private Camera camera;
    public static final int REQUEST_CODE = 100;
    private static final String TAG = "ScanVoucherFragment";
    byte[] data2 = null;
    TextRecognizer textRecognizer = TextRecognition.getClient();
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        requestPermission();
//        setupSurfaceHolder();


        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            takePictureAndRecognizeText();
            }
        });
    }

    private void initViews() {
        surfaceView = findViewById(R.id.surfaceView);
        btnTakePicture =findViewById(R.id.btnTakePicture);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},   MY_PERMISSIONS_REQUEST_CAMERA);

        } else {
            setupSurfaceHolder();
        }
    }


    private void takePictureAndRecognizeText() {
        try {
            byte[] imgBytes = CameraUtils.convertYuvToJpeg(data2, camera);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
            imageView.setImageBitmap(bitmap);

            InputImage image = InputImage.fromBitmap(bitmap, CameraUtils.getRotationCompensation(CameraUtils.getCameraID(), this));

            textRecognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text result) {
                    Log.e(TAG, "onSuccess: result size is "+result.getTextBlocks().size() );
                    String resultText = result.getText();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        String blockText = block.getText();
                        Point[] blockCornerPoints = block.getCornerPoints();
                        Rect blockFrame = block.getBoundingBox();
                        for (Text.Line line : block.getLines()) {
                            String lineText = line.getText();
                            Point[] lineCornerPoints = line.getCornerPoints();
                            Rect lineFrame = line.getBoundingBox();
                            for (Text.Element element : line.getElements()) {
                                String elementText = element.getText();
                                Point[] elementCornerPoints = element.getCornerPoints();
                                Rect elementFrame = element.getBoundingBox();
                                Log.e(TAG, "onSuccess:  "+elementText );

                                stringBuilder.append(elementText + "  -  ");
                            }
                        }
                    }
                    textView.setText(stringBuilder.toString());


                }
            });


        } catch (Exception e) {
            Log.e(TAG, "ERROR");
        }

        resetCamera();
    }

    private void setupSurfaceHolder() {

        surfaceViewHolder =surfaceView.getHolder();
        surfaceViewHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                resetCamera();
                Log.e(TAG, "surfaceChanged: Changing");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //releaseCamera();
            }

        });

    }


    private void startCamera() {


        camera = Camera.open();

        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);


        camera.setParameters(params);

        camera.setDisplayOrientation(CameraUtils.getRotationCompensation(CameraUtils.getCameraID(), this));

        try {
            camera.setPreviewDisplay(surfaceViewHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //     Log.e(TAG, "onPreviewFrame: getyk");

                data2 = data;


            }
        });


    }


    public void resetCamera() {
        if (surfaceViewHolder.getSurface() == null) {
            // Return if preview surface does not exist
            return;
        }

        if (camera != null) {
            // Stop if preview surface is already running.
            camera.stopPreview();
            try {
                // Set preview display
                camera.setPreviewDisplay(surfaceViewHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Start the camera preview...
            camera.startPreview();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "onRequestPermissionsResult: Permission check");

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: Permission Granted");
setupSurfaceHolder();
                    startCamera();

                    // main logic
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "You need to allow access permissions", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }
}