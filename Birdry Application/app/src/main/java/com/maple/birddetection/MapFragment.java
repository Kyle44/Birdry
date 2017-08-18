package com.maple.birddetection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by juliansniff on 3/18/2017.
 */

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private MapView mMapView;
    private static int sortType;
    private static HashMap<String, Marker> mHashMap;
    private static GoogleMap mMap;
    private static String TAG = "MapFragment";

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_fragment, container, false);

        //show all
        sortType = 0;

        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();// needed to get the map to display immediately

        mMapView.getMapAsync(this);

        mHashMap = new HashMap<String, Marker>();

        final ImageButton sortButton = (ImageButton) v.findViewById(R.id.map_sort);
        sortButton.setOnClickListener(
                new ImageButton.OnClickListener() {
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
                                DialogFragment sortFragment = new MapSortDialogFragment();
                                Bundle args = new Bundle();
                                args.putInt("sortType", sortType);
                                sortFragment.setArguments(args);
                                sortFragment.show(getFragmentManager(), "mapSort");
                            }
                        });
                        set.start();
                    }
                }
        );

        RelativeLayout mapCenterContainer = (RelativeLayout) v.findViewById(R.id.map_center_container);
        mapCenterContainer.bringToFront();

        ImageButton mapCenter = (ImageButton) v.findViewById(R.id.map_center);
        mapCenter.setOnClickListener(
                new ImageButton.OnClickListener() {
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
                                //move the camera to wherever you last took the picture
                                try{
                                    Location location = MainActivity.mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                    LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 17));
                                }catch(SecurityException e){
                                    Log.d(TAG, e.getMessage());
                                }
                            }
                        });
                        set.start();
                    }
                }
        );

        return v;
    }

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        for(int i = 0; i < MainActivity.imageList.size(); i++){
            ImageItem image = MainActivity.imageList.get(i);
            addImage(image);
        }

        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
        } catch (SecurityException e){
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Intent intent = new Intent(getActivity(), PictureDetailActivity.class);
        for(Map.Entry<String, Marker> entry : mHashMap.entrySet()) {
            Marker mMarker = entry.getValue();
            String filename = entry.getKey();

            if(mMarker.equals(marker)){
                intent.putExtra("filename", filename);
                break;
            }

        }
        startActivity(intent);
        return false;
    }

    public static void addImage(ImageItem image){
        Log.d(TAG, "adding to map");

        //move the camera to wherever you last took the picture
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(image.getLatitude(), image.getLongitude()), 17));

        if(image.getLatitude() != 0.0 && image.getLongitude() != 0.0){
            LatLng imageLatLng = new LatLng(image.getLatitude(), image.getLongitude());
            for(Marker marker: mHashMap.values()){
                if(marker.getPosition().equals(imageLatLng)){
                    Log.d(TAG, "conflict!");
                    //generate random decimal
                    Random rand = new Random();
                    double latDiff = rand.nextInt(9)/new Double(100000);
                    latDiff *= Math.floor(Math.random()*2) == 1 ? 1 : -1;
                    double longDiff = rand.nextInt(9)/new Double(100000);
                    longDiff *= Math.floor(Math.random()*2) == 1 ? 1 : -1;

                    //change latlong slightly so they don't overlap
                    imageLatLng = new LatLng(imageLatLng.latitude + latDiff, imageLatLng.longitude + longDiff);
                }
            }
            Bitmap bitmap = Bitmap.createScaledBitmap(image.getBitmap(), 200, 200, true);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .position(imageLatLng));
            marker.hideInfoWindow();

            mHashMap.put(image.getFilename(), marker);
        }
    }

    public static void removeImage(String filename){
        try{
            mHashMap.get(filename).remove();
            mHashMap.remove(filename);
        }catch(NullPointerException e){
            Log.d(TAG, e.getMessage());
        }
    }

    public static void setSortType(int sortType){
        MapFragment.sortType = sortType;
        switch(sortType){
            case 0:
                //all
                showAll();
                break;
            case 1:
                //only birds
                showOnlyBirds();
                break;
            case 2:
                //only not birds
                showOnlyNotBirds();
                break;
            case 3:
                //only today
                showOnlyToday();
                break;
        }
    }

    public static void showAll(){
        for (Marker marker: mHashMap.values()) {
            marker.setVisible(true);
        }
    }

    public static void showOnlyBirds(){
        for(Map.Entry<String, Marker> entry : mHashMap.entrySet()){
            Marker marker = entry.getValue();
            String filename = entry.getKey();
            ExifInterface exif;
            int birdPoints = -1;
            try {
                exif = new ExifInterface(filename);
                birdPoints = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_USER_COMMENT));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(birdPoints > 0){
                marker.setVisible(true);
            }else{
                marker.setVisible(false);
            }
        }
    }

    public static void showOnlyNotBirds(){
        for(Map.Entry<String, Marker> entry : mHashMap.entrySet()){
            Marker marker = entry.getValue();
            String filename = entry.getKey();
            ExifInterface exif;
            int birdPoints = -1;
            try {
                exif = new ExifInterface(filename);
                birdPoints = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_USER_COMMENT));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(birdPoints == 0){
                marker.setVisible(true);
            }else{
                marker.setVisible(false);
            }
        }
    }

    public static void showOnlyToday(){
        for(Map.Entry<String, Marker> entry : mHashMap.entrySet()){
            Marker marker = entry.getValue();
            String filename = entry.getKey();

            ExifInterface exif;
            String dateTime = null;
            Date imgDate = null;
            Date today = new Date();
            try {
                exif = new ExifInterface(filename);
                dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            } catch (IOException e) {
                e.printStackTrace();
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
            SimpleDateFormat compareFormat = new SimpleDateFormat("yyyyMMdd");


            try {
                imgDate = simpleDateFormat.parse(dateTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(compareFormat.format(imgDate).equals(compareFormat.format(today))){
                //if the days are the same
                marker.setVisible(true);
            }else{
                marker.setVisible(false);
            }
        }
    }

}
