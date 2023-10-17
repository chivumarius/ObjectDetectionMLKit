package com.chivumarius.objectdetectionmlkit;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;




public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int PERMISSION_CODE = 321;
    ImageView innerImage;
    private Uri image_uri;


    // ▼ "DECLARATION" OF "OBJECT DETECTOR" ▼
    ObjectDetector objectDetector;




    // ▬ "ON CREATE()" METHOD ▬
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        innerImage = findViewById(R.id.imageView2);


        innerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });

        innerImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);
                    }
                    else {
                        openCamera();
                    }
                }

                else {
                    openCamera();
                }
                return false;
            }
        });



        // (STEP 1-1 - "OBJECT DETECTION") "CREATING" THE "OBJECT DETECTION" INSTANCE
        //      → "MULTIPLE OBJECT DETECTION" →  IN "STATIC IMAGES":
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // ◄◄ OPTIONAL
                        .build();


        // (STEP 1-2 - "OBJECT DETECTION") "INSTANTIATION" OF "OBJECT DETECTOR":
        objectDetector = ObjectDetection.getClient(options);
    }








    // ▬ "OPEN CAMERA()" METHOD ▬
    private void openCamera() {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }





    // ▬ "ON ACTIVITY RESULT()" METHOD ▬
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            image_uri = data.getData();
            doObjectDetection();
        }

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK){
            doObjectDetection();
        }
    }





    // ▬ "ON OBJECT DETECTION()" METHOD
    //      → TO  PERFORM "IMAGE LABELING" ▬
    public void doObjectDetection(){
        Bitmap inputImage = uriToBitmap(image_uri);
        Bitmap rotated = rotateBitmap(inputImage);


        // (STEP 4-2.1 → "OBJEC T DETECTION") CREATING "MUTABLE COBY" OF "ROTATED" BITMAP:
        Bitmap mutable = rotated.copy(Bitmap.Config.ARGB_8888, true);

        // (STEP 4-2.2 → "OBJECT DETECTION") CREATING "CANVAS" OBJECT:
        Canvas canvas = new Canvas(mutable);

        // (STEP 4-2.3 → "OBJECT DETECTION") CREATING A "PAINT" OBJECT:
        Paint paint = new Paint();

        // (STEP 4-2.4 → "OBJECT DETECTION") "SETTING" THE "RECTANGLE COLOR":
        paint.setColor(Color.RED);

        // (STEP 4-2.5 → "OBJECT DETECTION") "SETTING" THE "RECTANGLE STYLE":
        paint.setStyle(Paint.Style.STROKE);

        // (STEP 4-2.6 → "OBJECT DETECTION") "SETTING" THE "STROKE WIDTH":
        paint.setStrokeWidth(3);



        // ▼ "DRAWING" THE "TEXT" ▼
        // (STEP 5-1.1 → "OBJECT DETECTION") CREATING A "PAINT" OBJECT FOR THE "TEXT":
        Paint paint2 = new Paint();

        // (STEP 5-1.2 → "OBJECT DETECTION") "SETTING" THE "TEXT COLOR":
        paint2.setColor(Color.YELLOW);

        // (STEP 5-1.3 → "OBJECT DETECTION") "SETTING" THE "TEXT SIZE":
        paint2.setTextSize(18);



        innerImage.setImageBitmap(rotated);


        // (STEP 2 - "OBJECT DETECTION") "PERFORMING" "INPUT IMAGE"
        //      → USING "BITMAP" AS "INPUT IMAGE":
        InputImage image = InputImage.fromBitmap(rotated, 0);


        // (STEP 3 - "OBJECT DETECTION") "PROCESS" THE "IMAGE":
        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {

                                // (STEP 4-1 → "OBJECT DETECTION") "GETTING INFORMATION" ABOUT "DETECTED OBJECTS":
                                //      → WHICH CONTAIN "ONE ITEM"
                                //      → IF "MULTIPLE OBJECT DETECTION" WASN'T "ENABLED":
                                // ▼ LOOPING FOR "RECTANGLE" ▼
                                for (DetectedObject detectedObject: detectedObjects) {
                                    Rect boundingBox = detectedObject.getBoundingBox();


                                    // (STEP 4-3 → "OBJECT DETECTION") "DRAWING" A "RECTANGLE" ON "CANVAS":
                                    canvas.drawRect(boundingBox, paint);


                                    Integer trackingId = detectedObject.getTrackingId();


                                    // ▼ LOOPING FOR "LABEL" ▼
                                    for (DetectedObject.Label label: detectedObject.getLabels()) {
                                        String text = label.getText();

                                        // (STEP 5-2 → "OBJECT DETECTION") "DRAWING" A "TEXT" ON "CANVAS":
                                        canvas.drawText(
                                                    text,
                                                    boundingBox.left,
                                                    boundingBox.top,
                                                    paint2
                                                );


                                        if (PredefinedCategory.FOOD.equals(text)) {

                                        }

                                        int index = label.getIndex();

                                        if (PredefinedCategory.FOOD_INDEX == index) {

                                        }

                                        float confidence = label.getConfidence();
                                        break;
                                    }
                                }

                                // (STEP 4-4 → "OBJECT DETECTION") SHOWING "MUTABLE" IN THE "INNER IMAGE":
                                innerImage.setImageBitmap(mutable);

                            }
                        })

                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
    }




    // ▬ "ON DESTROY()" METHOD ▬
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }




    // ▬ "ROTATE BITMAP()" METHOD
    //      → TO R"OTATE IMAGE" IF "IMAGE" IS "CAPTURED" ON "SAMSUNG DEVICES" ▬
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }





    // ▬ "URI TO BITMAP()" METHOD
    //      → IT TAKES "URI" OF THE "IMAGE" AND "RETURN BITMAP" ▬
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

}



