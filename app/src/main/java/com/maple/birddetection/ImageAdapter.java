package com.maple.birddetection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

/**
 * Created by Julian on 4/11/2017.
 */

public class ImageAdapter extends ArrayAdapter implements Filterable {

    private Context mContext;
    private int mLayoutResourceId;
    private ArrayList mData = new ArrayList();
    private ArrayList originalData = new ArrayList();
    private final String TAG = "ImageAdapter";

    public ImageAdapter(Context context, int layoutResourceId, ArrayList data){
        super(context, layoutResourceId, data);
        mLayoutResourceId = layoutResourceId;
        mContext = context;
        originalData = data;
        mData = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ImageView imageView = null;
        RelativeLayout border = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
            imageView = (ImageView) row.findViewById(R.id.image);
            border = (RelativeLayout) row.findViewById(R.id.border);
            row.setTag(imageView);
        } else {
            imageView = (ImageView) row.getTag();
            border = (RelativeLayout) row.findViewById(R.id.border);
        }

        try{
            ImageItem image = (ImageItem) mData.get(position);
            if(image.getBirdPoints() > 0){
                border.setBackgroundColor(Color.parseColor("#fc9246"));
            }else if(image.getBirdPoints() == 0){
                border.setBackgroundColor(Color.parseColor("#d3d3d3"));
            }else{
                border.setBackgroundColor(Color.parseColor("#ffff00"));
            }
            imageView.setImageBitmap(image.getBitmap());
        }catch(IndexOutOfBoundsException e){
            e.printStackTrace();
        }

        return row;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if(constraint == "showAll"){
                    results.values = originalData;
                    results.count = originalData.size();
                }else if(constraint == "showOnlyBirds"){
                    ArrayList onlyBirds = new ArrayList();
                    for(int i = 0; i < originalData.size(); i++){
                        ImageItem image = (ImageItem) originalData.get(i);
                        if(image.getBirdPoints() > 0){
                            onlyBirds.add(image);
                        }
                    }
                    results.values = onlyBirds;
                    results.count = onlyBirds.size();
                }else if(constraint == "showOnlyNotBirds"){
                    ArrayList onlyNotBirds = new ArrayList();
                    for(int i = 0; i < originalData.size(); i++){
                        ImageItem image = (ImageItem) originalData.get(i);
                        if(image.getBirdPoints() == 0){
                            onlyNotBirds.add(image);
                        }
                    }
                    results.values = onlyNotBirds;
                    results.count = onlyNotBirds.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mData = (ArrayList) results.values;
                notifyDataSetChanged();
            }
        };

        return filter;
    }

    @Override
    public int getCount() {
        return mData.size();
    }
}