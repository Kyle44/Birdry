package com.maple.birddetection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_detail_activity);

        //get image info
        birdPoints = getIntent().getIntExtra("birdPoints", 0);
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
        if(birdPoints == -1){
            birdPointsView.setText("Bird Points: Unknown");
        }else{
            birdPointsView.setText("Bird Points: " + birdPoints);
            if(birdPoints > 0){
                RelativeLayout border = (RelativeLayout) findViewById(R.id.border);
                border.setBackgroundColor(Color.parseColor("#fc9246"));
            }
        }

        ImageButton returnButton = (ImageButton) findViewById(R.id.return_button);
        returnButton.setOnClickListener(
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
                                finish();
                            }
                        });
                        set.start();
                    }
                }
        );

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
                                try {
                                    resendImage(bitmap);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
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
                                File file = new File(filename);
                                file.delete();
                                for(int i = 0; i < MainActivity.imageList.size(); i++){
                                    if(MainActivity.imageList.get(i).getFilename().equals(filename)){
                                        MainActivity.subtractBirdPoints(MainActivity.imageList.get(i).getBirdPoints());
                                        MainActivity.imageList.remove(i);
                                        break;
                                    }
                                }
                                GalleryFragment.getAdapter().notifyDataSetChanged();
                                MapFragment.removeImage(filename);
                                finish();
                            }
                        });
                        set.start();
                    }
                }
        );

    }

    public void resendImage(Bitmap bitmap) throws JSONException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);

        JsonObjectRequest jsonObjectRequest1 = new JsonObjectRequest(
                Request.Method.GET,
                "http://74.96.84.98:8000/test",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //this will come from responseObject
                        int responseBirdPoints = 50;

                        //if new value
                        if(responseBirdPoints != birdPoints){
                            ExifInterface exif = null;
                            try {
                                exif = new ExifInterface(filename);
                                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, String.valueOf(responseBirdPoints));
                                exif.saveAttributes();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //update the bird points
                            File file = new File(filename);
                            for(int i = 0; i < MainActivity.imageList.size(); i++){
                                ImageItem image = MainActivity.imageList.get(i);
                                if(image.getFilename().equals(filename)){
                                    MainActivity.totalBirdPoints -= image.getBirdPoints();
                                    image.setBirdPoints(responseBirdPoints);
                                    MainActivity.totalBirdPoints += image.getBirdPoints();
                                    break;
                                }
                            }

                            GalleryFragment.getAdapter().notifyDataSetChanged();
                            birdPoints = responseBirdPoints;

                            updateBirdPointsView();
                        }
                        Log.d(TAG, response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //error
                        Log.d(TAG, error.getMessage());
                        int responseBirdPoints =100;

                        //if new value
                        if(responseBirdPoints != birdPoints) {
                            ExifInterface exif = null;
                            try {
                                exif = new ExifInterface(filename);
                                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, String.valueOf(responseBirdPoints));
                                exif.saveAttributes();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //update the bird points
                            File file = new File(filename);
                            for (int i = 0; i < MainActivity.imageList.size(); i++) {
                                ImageItem image = MainActivity.imageList.get(i);
                                if (image.getFilename().equals(filename)) {
                                    if(image.getBirdPoints() != -1){
                                        MainActivity.totalBirdPoints -= image.getBirdPoints();
                                    }
                                    image.setBirdPoints(responseBirdPoints);
                                    MainActivity.totalBirdPoints += image.getBirdPoints();
                                    break;
                                }
                            }

                            GalleryFragment.getAdapter().notifyDataSetChanged();
                            birdPoints = responseBirdPoints;

                            updateBirdPointsView();
                        }

                    }
                }
        );

        MainActivity.mRequestQueue.add(jsonObjectRequest1);
    }

    public void updateBirdPointsView(){
        TextView birdPointsView = (TextView) findViewById(R.id.bird_points);
        birdPointsView.setText("Bird Points: " + birdPoints);
        RelativeLayout border = (RelativeLayout) findViewById(R.id.border);
        if(birdPoints > 0){
            border.setBackgroundColor(Color.parseColor("#fc9246"));
        }else{
            border.setBackgroundColor(Color.parseColor("#d3d3d3"));
        }
    }
}
