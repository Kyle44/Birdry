package com.maple.birddetection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PictureDetailActivity extends AppCompatActivity {

    private static final String TAG = "PictureDetailActivity";
    private String filename;
    private Bitmap bitmap;
    private String dateTime;
    private double latitude;
    private double longitude;
    private Date imgDate;
    private ExifInterface exif;
    private int birdPoints;
    private static RelativeLayout mBorder;
    private static TextView mBirdPointsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_detail_activity);

        //get image info
        filename = getIntent().getStringExtra("filename");

        try {
            exif = new ExifInterface(filename);
            dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            latitude = MainActivity.convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longitude = MainActivity.convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
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


        //load bitmap
        ImageLoader imageLoader = ImageLoader.getInstance();
        bitmap = imageLoader.loadImageSync("file://" + filename);
        ImageView imageView = (ImageView) findViewById(R.id.bitmap);
        imageView.setImageBitmap(bitmap);

        //load text fields
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
        SimpleDateFormat convertDate = new SimpleDateFormat("MMM dd, yyyy");
        SimpleDateFormat convertTime = new SimpleDateFormat("hh:mm:ss aa");

        try {
            imgDate = simpleDateFormat.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //set the location text view
        TextView locationView = (TextView) findViewById(R.id.location);
        DecimalFormat decFormat = new DecimalFormat("#.00");
        locationView.setText("Coordinates: "
                + String.valueOf(decFormat.format(latitude))
                + ", "
                + String.valueOf(decFormat.format(longitude)));

        //set the date text view
        TextView dateView = (TextView) findViewById(R.id.date);
        dateView.setText("Date: " + convertDate.format(imgDate));

        //set the time text view
        TextView timeView = (TextView) findViewById(R.id.time);
        timeView.setText("Time: " + convertTime.format(imgDate));

        //set the bird points text view
        TextView birdPointsView = (TextView) findViewById(R.id.bird_points);
        mBirdPointsView = birdPointsView;
        RelativeLayout border = (RelativeLayout) findViewById(R.id.border);
        mBorder = border;
        if(birdPoints > 0){
            birdPointsView.setText("Bird Points: " + birdPoints);
            border.setBackgroundColor(Color.parseColor("#fc9246"));
        }else if(birdPoints == 0){
            birdPointsView.setText("Bird Points: " + birdPoints);
            border.setBackgroundColor(Color.parseColor("#d3d3d3"));
        }else{
            birdPointsView.setText("Bird Points: Processing");
            border.setBackgroundColor(Color.parseColor("#ffff00"));
        }

        ImageButton resendButton = (ImageButton) findViewById(R.id.resend_button);
        resendButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(v, "alpha", 1f, .5f);
                        fadeOut.setDuration(150);
                        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(v, "alpha", .5f, 1f);
                        fadeIn.setDuration(150);
                        AnimatorSet set = new AnimatorSet();
                        set.play(fadeIn).after(fadeOut);
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                reviewImage();
                            }
                        });
                        set.start();

                    }
                }
        );

        ImageButton trashButton = (ImageButton) findViewById(R.id.trash_button);
        trashButton.setOnClickListener(
                new Button.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(v, "alpha", 1f, .5f);
                        fadeOut.setDuration(150);
                        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(v, "alpha", .5f, 1f);
                        fadeIn.setDuration(150);
                        AnimatorSet set = new AnimatorSet();
                        set.play(fadeIn).after(fadeOut);
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                DialogFragment confirmDelete = new ConfirmDeleteDialogFragment();
                                Bundle args = new Bundle();
                                args.putString("filename", filename);
                                args.putInt("birdPoints", birdPoints);
                                confirmDelete.setArguments(args);
                                confirmDelete.show(getSupportFragmentManager(), "ConfirmDelete");
                            }
                        });
                        set.start();
                    }
                }
        );

    }

    public void reviewImage(){
        final int oldBirdPoints = birdPoints;

        //can't review if you haven't sent yet
        if(birdPoints == -1){
            //inform user toast
            Toast.makeText(getApplicationContext(), "You can't review an image that hasn't been sent before!", Toast.LENGTH_SHORT).show();
            return;
        }else if(birdPoints == 0){
            birdPoints = 1;
        }else if(birdPoints > 0){
            birdPoints = 0;
        }

        //update the file
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filename);
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, String.valueOf(birdPoints));
            exif.saveAttributes();
        }catch(IOException e){
            e.printStackTrace();
        }

        //update the bird points in imageList
        for(int i = 0; i < MainActivity.imageList.size(); i++){
            ImageItem image = MainActivity.imageList.get(i);
            if(image.getFilename().equals(filename)){
                MainActivity.totalBirdPoints -= image.getBirdPoints();
                image.setBirdPoints(birdPoints);
                MainActivity.totalBirdPoints += image.getBirdPoints();
                break;
            }
        }

        //update the gallery
        GalleryFragment.getAdapter().notifyDataSetChanged();

        //update the current view
        updateBirdPointsView();

        JSONObject data = new JSONObject();

        //only send the last section of the filename
        String[] arrayOfStrings = filename.split("/");
        String endOfFilename = arrayOfStrings[arrayOfStrings.length - 1];

        try {
            data.put("filename", endOfFilename);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "json: " + data.toString());

        JsonObjectRequest review = new JsonObjectRequest(
                Request.Method.PUT,
                "http://74.96.84.98:8000/review",
                data,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "success review");

                        //success toast
                        Toast successToast = Toast.makeText(getApplicationContext(), "The review was processed successfully!, Thanks for your feedback!", Toast.LENGTH_SHORT);
                        successToast.show();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //do nothing
                        Log.d(TAG, "error review " + error.getMessage());

                        //error toast
                        Toast errorToast = Toast.makeText(getApplicationContext(), "There was an error sending the review!", Toast.LENGTH_SHORT);
                        errorToast.show();

                        birdPoints = oldBirdPoints;

                        //update the file
                        ExifInterface exif = null;
                        try {
                            exif = new ExifInterface(filename);
                            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, String.valueOf(birdPoints));
                            exif.saveAttributes();
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        //update the bird points in imageList
                        for(int i = 0; i < MainActivity.imageList.size(); i++){
                            ImageItem image = MainActivity.imageList.get(i);
                            if(image.getFilename().equals(filename)){
                                MainActivity.totalBirdPoints -= image.getBirdPoints();
                                image.setBirdPoints(birdPoints);
                                MainActivity.totalBirdPoints += image.getBirdPoints();
                                break;
                            }
                        }

                        //update the gallery
                        GalleryFragment.getAdapter().notifyDataSetChanged();

                        //update the current view
                        updateBirdPointsView();

                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Token " + MainActivity.token);
                return headers;
            }
        };

        review.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MainActivity.mRequestQueue.add(review);
    }

    public void updateBirdPointsView(){
        TextView birdPointsView = (TextView) findViewById(R.id.bird_points);
        RelativeLayout border = (RelativeLayout) findViewById(R.id.border);
        if(birdPoints > 0){
            border.setBackgroundColor(Color.parseColor("#fc9246"));
            birdPointsView.setText("Bird Points: " + birdPoints);
        }else if(birdPoints == 0){
            border.setBackgroundColor(Color.parseColor("#d3d3d3"));
            birdPointsView.setText("Bird Points: " + birdPoints);
        }else{
            border.setBackgroundColor(Color.parseColor("#ffff00"));
            birdPointsView.setText("Bird Points: processing");
        }
    }

    public static void updateBirdPointsViewFromMainActivity(int newBirdPoints){
        mBirdPointsView.setText("Bird Points: " + newBirdPoints);
        if(newBirdPoints > 0){
            mBorder.setBackgroundColor(Color.parseColor("#fc9246"));
            mBirdPointsView.setText("Bird Points: " + newBirdPoints);
        }else if(newBirdPoints == 0){
            mBorder.setBackgroundColor(Color.parseColor("#d3d3d3"));
            mBirdPointsView.setText("Bird Points: " + newBirdPoints);
        }else{
            mBorder.setBackgroundColor(Color.parseColor("#ffff00"));
            mBirdPointsView.setText("Bird Points: processing");
        }
    }
}
