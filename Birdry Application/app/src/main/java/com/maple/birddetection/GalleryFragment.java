package com.maple.birddetection;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by juliansniff on 3/19/2017.
 */

public class GalleryFragment extends Fragment {
    public final static String TAG = "GalleryFragment";
    private static GridView mGridView;
    private static ImageAdapter mAdapter;
    private static int sortType;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.gallery_fragment, container, false);

        sortType = 0;

        mGridView = (GridView) v.findViewById(R.id.image_gallery);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ImageItem image = (ImageItem) parent.getItemAtPosition(position);
                LinearLayout imageView = (LinearLayout) mGridView.getChildAt(position);
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, .5f);
                fadeOut.setDuration(150);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", .5f, 1f);
                fadeIn.setDuration(150);
                AnimatorSet set = new AnimatorSet();
                set.play(fadeIn).after(fadeOut);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        Intent intent = new Intent(getActivity(), PictureDetailActivity.class);
                        intent.putExtra("filename", image.getFilename());
                        startActivity(intent);
                    }
                });
                set.start();
            }
        });

        mAdapter = new ImageAdapter(getActivity(), R.layout.image_layout, MainActivity.imageList);
        mGridView.setAdapter(mAdapter);

        final ImageButton sortButton = (ImageButton) v.findViewById(R.id.gallery_sort);
        sortButton.setOnClickListener(
                new ImageButton.OnClickListener(){
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
                                DialogFragment sortFragment = new GallerySortDialogFragment();
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

        return v;
    }

    public static GalleryFragment newInstance(){
        GalleryFragment fragment = new GalleryFragment();
        return fragment;
    }

    public static ImageAdapter getAdapter(){
        return mAdapter;
    }

    public static void setSortType(int sortType){
        GalleryFragment.sortType = sortType;
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
        }
    }

    public static void showAll(){
        mAdapter.getFilter().filter("showAll");
    }

    public static void showOnlyBirds(){
        mAdapter.getFilter().filter("showOnlyBirds");
    }

    public static void showOnlyNotBirds(){
        mAdapter.getFilter().filter("showOnlyNotBirds");
    }

}
