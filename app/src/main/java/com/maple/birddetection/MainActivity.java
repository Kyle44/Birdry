package com.maple.birddetection;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";

    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int LOCATION_REQUEST_CODE = 2;
    private static final int STORAGE_REQUEST_CODE = 3;
    public static final int ALL_REQUEST_CODE = 10;

    private String appName = "Birdry";
    public static Camera mCamera;
    private CameraPreview mPreview;
    private boolean appStart;
    private static RelativeLayout cameraLayout;

    private boolean isCameraOpen = false;

    public static ArrayList<ImageItem> imageList;
    ViewPager viewPager;   //set the page adapter
    public static int totalBirdPoints;
    private static TextView mBirdPointsView;

    public static RequestQueue mRequestQueue;

    boolean isStopPreview = false; // true when mCamera.stopPreview() is active
    boolean isButtonsAdded = false; // true when the accept/cancel buttons are present on the screen

    public static String token;
    public static LocationManager mLocationManager;
    private int fragmentIndex;

    private static Context context;

    //override the back button so that it returns to the camera
    //view from the map and gallery fragments
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if(fragmentIndex == 1){
                return super.onKeyDown(keyCode, event);
            }else{
                viewPager.setCurrentItem(1, true);
                return true;
            }
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override // onCreate happens once, right when the app is launched
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //request permissions
        requestAllPermissions();

        //set image loader configuration
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(new DisplayImageOptions.Builder().considerExifParams(true).build())
                .build();
        ImageLoader.getInstance().init(config);

        //set up request queue
        mRequestQueue = Volley.newRequestQueue(this);

        //set the content view
        setContentView(R.layout.main_activity);

        appStart = true;
        cameraLayout = (RelativeLayout) findViewById(R.id.camera_fragment);
        viewPager = (ViewPager) findViewById(R.id.view_pager);

        //set offscreen page limit
        viewPager.setOffscreenPageLimit(3);

        context = getApplicationContext();

        //set the window to fullscreen initially
        getWindow().getDecorView().getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE);
        getWindow().getDecorView().getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int mposition;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //remove the status bar
                getWindow().getDecorView().getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                if (appStart) {
                    viewPager.setZ(1);
                    appStart = false;
                } else {
                    viewPager.setZ(100);
                }
            }

            @Override
            public void onPageSelected(int position) {
                fragmentIndex = position;
                mposition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        if (mposition == 1) {
                            //remove the status bar
                            getWindow().getDecorView().getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                            viewPager.setZ(1);
                        } else {
                            //add the status bar
                            getWindow().getDecorView().getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                            viewPager.setZ(100);
                        }
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "resuming applicatioon");

        if(fragmentIndex == 1){
            getWindow().getDecorView().getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        if(!isCameraPermissionGranted()){ // if no camera permission...
            Log.d(TAG, "requesting permissions");
            requestCameraPermission(); // request it
        }else if(!isCameraOpen){
            createCamera();
            isCameraOpen = true;
            viewPager.bringToFront();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCamera != null){
            isCameraOpen = false;
            mCamera.release();
            mCamera = null;
        }
        if(isButtonsAdded){
            destroyAcceptAndCancelButtons();
            isButtonsAdded = false;
        }
        cameraLayout.removeView(mPreview);
        mPreview = null;
    }

    // Creates the camera and puts it on the screen
    private void createCamera() {
        //add the camera preview to the view
        mCamera = Camera.open();

        //rotate the image for picturecallback
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(90);
        mCamera.setParameters(params);

        //add the camera preview to the view
        mPreview = new CameraPreview(this, mCamera);

        cameraLayout.addView(mPreview);

        //add the camera buttons and Bird Points
        addCaptureButton(cameraLayout);
        addBirdPointsView();
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            final RelativeLayout cameraLayout = (RelativeLayout) findViewById(R.id.camera_fragment);

            //pause the preview
            mCamera.stopPreview();
            isStopPreview = true;

            //remove capture button
            destroyCaptureButton();

            //cancel button
            addCancelButton(cameraLayout);

            //accept button
            addAcceptButton(cameraLayout, data);
            isButtonsAdded = true;
        }
    };

    private void addCaptureButton(final RelativeLayout cameraLayout) {
        final Button captureButton = new Button(this);
        captureButton.setBackgroundResource(R.drawable.capture_button);
        captureButton.setTag("capture_button");

        captureButton.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPicture);
                        cameraLayout.removeView(captureButton);
                    }
                }
        );

        RelativeLayout.LayoutParams buttonDetails = new RelativeLayout.LayoutParams(300, 300);

        buttonDetails.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonDetails.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonDetails.bottomMargin = 100;

        cameraLayout.addView(captureButton, buttonDetails);
    }

    private void addCancelButton(final RelativeLayout cameraLayout) {
        Button cancelButton = new Button(this);
        cancelButton.setBackgroundResource(R.drawable.cancel_button);
        cancelButton.setTag("cancel_button");

        RelativeLayout.LayoutParams buttonDetails = new RelativeLayout.LayoutParams(200, 200);

        buttonDetails.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        buttonDetails.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonDetails.bottomMargin = 100;
        buttonDetails.leftMargin = 100;

        cancelButton.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //destroy accept and cancel buttons
                        destroyAcceptAndCancelButtons();

                        //show capture button
                        addCaptureButton(cameraLayout);

                        //start preview
                        mCamera.startPreview();
                        isStopPreview = false;
                    }
                }
        );

        cameraLayout.addView(cancelButton, buttonDetails);
    }

    private void addAcceptButton(final RelativeLayout cameraLayout, final byte[] data) {
        Button acceptButton = new Button(this);
        acceptButton.setBackgroundResource(R.drawable.accept_button);
        acceptButton.setTag("accept_button");

        RelativeLayout.LayoutParams buttonDetails = new RelativeLayout.LayoutParams(200, 200);

        buttonDetails.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonDetails.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonDetails.bottomMargin = 100;
        buttonDetails.rightMargin = 100;

        acceptButton.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final File pictureFile = getOutputMediaFile();
                        if (pictureFile == null) {
                            Log.d(TAG, "Error creating media file, check storage permissions");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                            MediaScannerConnection.scanFile(MainActivity.this, new String[] { pictureFile.getPath() }, new String[] { "image/jpeg" }, null);
                        } catch (FileNotFoundException e) {
                            Log.d(TAG, "File not found: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d(TAG, "Error accessing file: " + e.getMessage());
                        }

                        ImageLoader imageLoader = ImageLoader.getInstance();
                        Bitmap bitmap = imageLoader.loadImageSync("file://" + pictureFile);

                        //create initial lat and long
                        double latitude = 0;
                        double longitude = 0;

                        //create the image
                        ImageItem image = new ImageItem(bitmap, -1, pictureFile.getAbsolutePath(), latitude, longitude);

                        //add to the imageList
                        imageList.add(image);

                        //update gallery
                        GalleryFragment.getAdapter().notifyDataSetChanged();

                        //write initial bird points
                        try {
                            ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "-1");
                            exif.saveAttributes();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try{
                            Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            //write to file
                            try {
                                ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
                                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convert(latitude));
                                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeRef(latitude));
                                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convert(longitude));
                                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeRef(longitude));
                                exif.saveAttributes();
                                Log.d(TAG,exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) + " " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //update map
                            image.setLatitude(latitude);
                            image.setLongitude(longitude);
                            MapFragment.addImage(image);

                            //update imagelist
                            for(int i = 0; i < imageList.size(); i++){
                                if(image.getFilename().equals(imageList.get(i).getFilename())){
                                    imageList.get(i).setLongitude(longitude);
                                    imageList.get(i).setLatitude(latitude);
                                }
                            }

                            GalleryFragment.getAdapter().notifyDataSetChanged();


                            //send the image
                            try {
                                sendImage(image);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }catch(SecurityException e){
                            Log.d(TAG, e.getMessage());
                        }

                        //destroy accept and cancel buttons
                        destroyAcceptAndCancelButtons();

                        //show capture button
                        addCaptureButton(cameraLayout);

                        //start preview
                        mCamera.startPreview();
                        isStopPreview = false;
                    }
                }
        );

        cameraLayout.addView(acceptButton, buttonDetails);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    return MapFragment.newInstance();
                case 1:
                    return BlankFragment.newInstance();
                case 2:
                    return GalleryFragment.newInstance();
                default:
                    return BlankFragment.newInstance();
            }
        }



        @Override
        public int getCount() {
            return 3;
        }


    }

    private void destroyAcceptAndCancelButtons(){
        RelativeLayout cameraLayout = (RelativeLayout)findViewById(R.id.camera_fragment);
        Button acceptButton = (Button)cameraLayout.findViewWithTag("accept_button");
        Button cancelButton = (Button)cameraLayout.findViewWithTag("cancel_button");

        cameraLayout.removeView(acceptButton);
        cameraLayout.removeView(cancelButton);
        isButtonsAdded = false;
    }

    private void destroyCaptureButton(){
        RelativeLayout cameraLayout = (RelativeLayout)findViewById(R.id.camera_fragment);
        Button captureButton = (Button)cameraLayout.findViewWithTag("capture_button");
        cameraLayout.removeView(captureButton);
    }

    private File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appName);

        //check that the directory exists or we can make it
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG" + timeStamp + ".jpg");
        return mediaFile;
    }

    // Request the permissions from the user if they are not granted yet
    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE}, ALL_REQUEST_CODE);
    }

    private void requestLocationPermissions(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    private void requestStoragePermissions(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    private boolean isLocationPermissionGranted(){
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
    }

    private boolean isStoragePermissionGranted(){
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
    }

    private boolean isCameraPermissionGranted(){
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == ALL_REQUEST_CODE){
            //after location permission granted
            if(isLocationPermissionGranted()){
                Log.d(TAG, "location permissions");
                //get LocationManager
                mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                try{
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            Log.d(TAG, location.toString());
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                            Log.d(TAG, String.valueOf(status));

                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                            Log.d(TAG, "enabled: " + provider);

                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                            Log.d(TAG, "disabled: " + provider);
                        }
                    });
                }catch(SecurityException e){
                    Log.d(TAG, e.getMessage());
                }
            }

            //after storage permission granted
            if(isStoragePermissionGranted()){
                Log.d(TAG, "storage permissions");
                imageList = getData(appName);

                testAuth();
            }

            viewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
            viewPager.setCurrentItem(1);
            viewPager.bringToFront();
        }

        //called on resume applcation
        if(requestCode == CAMERA_REQUEST_CODE && isCameraPermissionGranted()){
            Log.d(TAG, "camera permissions");
            createCamera(); // Put the camera on the screen
            isCameraOpen = true;
            viewPager.bringToFront();
        }

    }

    public static ArrayList<ImageItem> getData(String appName){
        ArrayList<ImageItem> imageItems = new ArrayList<>();
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appName);
        ImageLoader imageLoader = ImageLoader.getInstance();

        File [] imageFiles = mediaStorageDir.listFiles();

        if(imageFiles != null){
            for(int i = 0; i < imageFiles.length; i++){
                Bitmap bitmap = imageLoader.loadImageSync("file://" + imageFiles[i].getPath());
                ExifInterface exif = null;

                double latitude = 0.0;
                double longitude = 0.0;
                int birdPoints = -1;

                try {
                    exif = new ExifInterface(imageFiles[i].getPath());
                    latitude = convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                    String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                    longitude = convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                    String longRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                    birdPoints = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_USER_COMMENT));
                    if(latRef.equals("S")){
                        latitude = -latitude;
                    }
                    if(longRef.equals("W")){
                        longitude = -longitude;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //if we sent to server add bird points
                if(birdPoints != -1){
                    addBirdPoints(birdPoints);
                }

                ImageItem image = new ImageItem(bitmap, birdPoints, imageFiles[i].getAbsolutePath(), latitude, longitude);
                imageItems.add(image);
            }
        }
        return imageItems;
    }

    public static String latitudeRef(double latitude) {
        return latitude < 0.0d ? "S":"N";
    }

    public static String longitudeRef(double longitude) {
        return longitude < 0.0d ? "W":"E";
    }

    //can be used for longitude too
    public static String convert(double latitude) {
        StringBuilder sb = new StringBuilder(20);
        latitude=Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude*1000.0d);

        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000,");
        return sb.toString();
    }

    public static double convertToDegree(String stringDMS){
        if(stringDMS == null){
            return 0.0;
        }
        String[] dms = stringDMS.split(",", 3);

        String[] StringD = dms[0].split("/", 2);
        double d0 = Double.valueOf(StringD[0]);
        double d1 = Double.valueOf(StringD[1]);
        double d = d0/d1;

        String[] StringM = dms[1].split("/", 2);
        double m0 = Double.valueOf(StringM[0]);
        double m1 = Double.valueOf(StringM[1]);
        double m = m0/m1;

        String[] StringS = dms[2].split("/", 2);
        double s0 = Double.valueOf(StringS[0]);
        double s1 = Double.valueOf(StringS[1]);
        double s = s0/s1;

        return Double.valueOf(d + (m/60) + (s/3600));

    }

    public static void subtractBirdPoints(int points){
        totalBirdPoints -= points;
        if(mBirdPointsView != null){
            mBirdPointsView.setText("Bird Points: " + String.valueOf(totalBirdPoints));
        }
    }

    private static void addBirdPoints(int points){
        totalBirdPoints += points;
        if(mBirdPointsView != null){
            mBirdPointsView.setText("Bird Points: " + String.valueOf(totalBirdPoints));
        }
    }

    private void addBirdPointsView(){
        TextView birdPointsView = new TextView(this);
        birdPointsView.setTag("bird_points");
        String text = "Bird Points: " + String.valueOf(totalBirdPoints);
        birdPointsView.setText(text);
        birdPointsView.setTextSize(24);
        birdPointsView.setTextColor(Color.WHITE);
        birdPointsView.setTypeface(null, Typeface.BOLD);
        RelativeLayout.LayoutParams birdDetails = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        RelativeLayout cameraLayout = (RelativeLayout) findViewById(R.id.camera_fragment);
        cameraLayout.addView(birdPointsView);
        mBirdPointsView = birdPointsView;
    }

    public void testAuth(){
        //name of file to store token
        final String authFilename = "Birdry_Auth_Token";

        //try to load the token from the filesystem
        try {
            FileInputStream fis = openFileInput(authFilename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            fis.close();
            token =  sb.toString();
            Log.d(TAG, "reusing token: " + token);
        }catch (FileNotFoundException e) {
            Log.d(TAG, "file not found");
            e.printStackTrace();
            getAuth();
            return;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
            getAuth();
            return;
        }

        //if you read it from the filesystem then test it to make sure it's valid
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                "http://74.96.84.98:8000/test",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "token exists!");
                        //do nothing
                        //good to go
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "token is invalid, getting a new one");
                        getAuth();
                    }
                }
        );

        mRequestQueue.add(jsonObjectRequest);
    }

    public void getAuth(){
        //name of file to store token
        final String authFilename = "Birdry_Auth_Token";

        //if you didn't read it from the filesystem make a request for a new token
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                "http://74.96.84.98:8000/auth",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String authToken = response.getString("token");
                            token = response.getString("token");
                            Log.d(TAG, "obtained new token: " + token);
                            try {
                                FileOutputStream fos = openFileOutput(authFilename, Context.MODE_PRIVATE);
                                fos.write(authToken.getBytes());
                                fos.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.getMessage());
                    }
                }
        );

        mRequestQueue.add(jsonObjectRequest);
    }

    public static void sendImage(final ImageItem image) throws JSONException {
        //send the image
        Bitmap bitmap = image.getBitmap();
        final String filename = image.getFilename();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);

        //only send the last section of the filename
        String[] arrayOfStrings = image.getFilename().split("/");
        String endOfFilename = arrayOfStrings[arrayOfStrings.length - 1];

        JSONObject imageJSON = new JSONObject();
        imageJSON.put("image", encodedImage);
        imageJSON.put("filename", endOfFilename);
        imageJSON.put("latitude", image.getLatitude());
        imageJSON.put("longitude", image.getLongitude());

        JsonObjectRequest jsonObjectRequest1 = new JsonObjectRequest(
                Request.Method.POST,
                "http://74.96.84.98:8000/upload",
                imageJSON,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());

                        //on success add the image to the image list and update bird points
                        ExifInterface exif = null;

                        //this will be from the jsonObject
                        int responseBirdPoints;

                        try {
                            responseBirdPoints = response.getInt("birdPoints");
                        } catch (JSONException e) {
                            responseBirdPoints = -1;
                            e.printStackTrace();
                        }

                        //success toast
                        Toast successToast = Toast.makeText(MainActivity.getContext(), "+" + responseBirdPoints + " bird points!", Toast.LENGTH_SHORT);
                        successToast.show();

                        //add to bird points
                        addBirdPoints(responseBirdPoints);

                        //update the exif bird points
                        try {
                            exif = new ExifInterface(filename);
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, String.valueOf(responseBirdPoints));
                            exif.saveAttributes();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //update the bird points in imageList
                        for(int i = 0; i < MainActivity.imageList.size(); i++){
                            ImageItem image = MainActivity.imageList.get(i);
                            if(image.getFilename().equals(filename)){
                                image.setBirdPoints(responseBirdPoints);
                                break;
                            }
                        }

                        //update gallery
                        GalleryFragment.getAdapter().notifyDataSetChanged();

                        try{
                            PictureDetailActivity.updateBirdPointsViewFromMainActivity(responseBirdPoints);
                        }catch(Exception e){
                            Log.d(TAG, e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                        //on error add the image to the image list and don't update the bird points

                        //error toast
                        Toast errorToast = Toast.makeText(MainActivity.getContext(), "Image failed to send!", Toast.LENGTH_SHORT);
                        errorToast.show();

                        //update the exif bird points
                        try {
                            ExifInterface exif = new ExifInterface(filename);
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, String.valueOf(0));
                            exif.saveAttributes();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //update the bird points in imageList
                        for(int i = 0; i < MainActivity.imageList.size(); i++){
                            ImageItem image = MainActivity.imageList.get(i);
                            if(image.getFilename().equals(filename)){
                                image.setBirdPoints(0);
                                break;
                            }
                        }

                        //update gallery
                        GalleryFragment.getAdapter().notifyDataSetChanged();

                        try{
                            PictureDetailActivity.updateBirdPointsViewFromMainActivity(0);
                        }catch(Exception e){
                            Log.d(TAG, e.getMessage());
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Token " + token);
                return headers;
            }
        };

        jsonObjectRequest1.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(jsonObjectRequest1);
    }

    public static Context getContext(){
        return context;
    }
}

