package com.maple.birddetection;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Environment;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by juliansniffen on 4/14/17.
 */

public class ImageItem {
    private double latitude;
    private double longitude;
    private String filename;
    private Date dateTime;
    private Bitmap bitmap;
    private int birdPoints;

    public ImageItem(Bitmap bitmap, int birdPoints, String filename, double latitude, double longitude){
        this.bitmap = bitmap;
        this.birdPoints = birdPoints;
        this.filename = filename;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Bitmap getBitmap(){
        return bitmap;
    }

    public int getBirdPoints() { return birdPoints; }

    public void setBirdPoints(int birdPoints){
        this.birdPoints = birdPoints;
    }

    public String getFilename() { return filename; }

    public double getLatitude(){
        return latitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

}
