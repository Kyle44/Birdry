package com.maple.birddetection;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by juliansniff on 3/23/2017.
 */

public class BlankFragment extends Fragment {
    private final String TAG = "BlankFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.blank_fragment, container, false);

        v.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try{

                            MainActivity.mCamera.autoFocus(
                                    new Camera.AutoFocusCallback() {
                                        @Override
                                        public void onAutoFocus(boolean success, Camera camera) {
                                            Log.d(TAG, "autofocusing camera");
                                            if(success){
                                                Log.d(TAG, "autofocused");
                                            }else{
                                                Log.d(TAG, "couldn't autofocus");
                                            }
                                            camera.cancelAutoFocus();
                                        }
                                    }
                            );
                        }catch(RuntimeException e){
                            Log.d(TAG, e.getMessage());
                        }
                    }
                }
        );

        return v;
    }

    public static BlankFragment newInstance(){
        BlankFragment blankFragment = new BlankFragment();
        return blankFragment;
    }


}
