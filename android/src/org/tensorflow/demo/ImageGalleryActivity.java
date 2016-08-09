/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import android.app.Fragment;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.LayoutInflater;

import junit.framework.Assert;

import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.env.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

public class ImageGalleryActivity extends Activity {

    private static final Logger LOGGER = new Logger();
    private static int RESULT_LOAD_IMAGE = 1;

    private final TensorFlowClassifier tensorflow = new TensorFlowClassifier();

    private ImageView imageView;
    private RecognitionScoreView scoreView;

    //private static final String MODEL_FILE = "tensorflow_inception_graph.pb";
    //private static final String LABEL_FILE = "imagenet_comp_graph_label_strings.txt";

    /*
    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private static final int NUM_CLASSES = 1001;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "output:0";
    */

    private static final String MODEL_FILE = "file:///android_asset/scene_classification_stripped.pb";
    private static final String LABEL_FILE = "file:///android_asset/output_labels.txt";

    private static final int NUM_CLASSES = 3;
    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul:0";
    private static final String OUTPUT_NAME = "final_result:0";

    private Bitmap loadedBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean hasInitialized = false;

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_imagegallery);

        scoreView = (RecognitionScoreView) findViewById(R.id.results);
        imageView = (ImageView) findViewById(R.id.imgView);

        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });


        if(hasPermission()){
            if(null == savedInstanceState){

            }
        } else{
             requestPermission();
        }

        if(hasInitialized == false){
            initializeTensorflow();
            hasInitialized = true;
        }
    }

    private void initializeTensorflow(){

        final AssetManager assetManager = getResources().getAssets();

        if(assetManager == null)
            return;

    //    Toast.makeText(getApplicationContext(), "asset got!",
    //        Toast.LENGTH_SHORT).show();

        /*
        try{
            InputStream inputStream = assetManager.open(LABEL_FILE);
            String s = readTextFile(inputStream);

            Toast.makeText(getApplicationContext(), s,
                    Toast.LENGTH_SHORT).show();

            String[] files = assetManager.list("");

            Toast.makeText(getApplicationContext(), files[1],
                    Toast.LENGTH_SHORT).show();
        } catch(IOException e){
            Toast.makeText(getApplicationContext(), "read failed!",
                    Toast.LENGTH_SHORT).show();
        }
        */

        try{
            tensorflow.initializeTensorFlow(
                    assetManager, MODEL_FILE, LABEL_FILE, NUM_CLASSES, INPUT_SIZE, IMAGE_MEAN,
                    IMAGE_STD, INPUT_NAME, OUTPUT_NAME);

            Toast.makeText(getApplicationContext(), "initialized!",
                    Toast.LENGTH_SHORT).show();
        } catch(Exception e){

        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            loadedBitmap = BitmapFactory.decodeFile(picturePath);
            imageView.setImageBitmap(loadedBitmap);

            croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

            drawResizedBitmap(loadedBitmap, croppedBitmap);

        //    ImageUtils.saveBitmap(croppedBitmap);  //for test

            try{
                final List<Classifier.Recognition> results = tensorflow.recognizeImage(croppedBitmap);

                scoreView.setResults(results);

            } catch(Exception e){

            }
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst){
        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    private String readTextFile(InputStream inputStream) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte buf[] = new byte [1024];

        int len;

        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }

            outputStream.close();
            inputStream.close();


        } catch (IOException e) {

        }

        return outputStream.toString();

    }

    private boolean hasPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)){
                Toast.makeText(getApplicationContext(), "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }
}


