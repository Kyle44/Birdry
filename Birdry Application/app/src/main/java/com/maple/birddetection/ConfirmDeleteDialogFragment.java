package com.maple.birddetection;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.io.File;

/**
 * Created by juliansniffen on 5/6/17.
 */

public class ConfirmDeleteDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Bundle args = getArguments();
        final int birdPoints = args.getInt("birdPoints");
        final String filename = args.getString("filename");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle("Are you sure you want to delete the image?")
                //on ok
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        //delete the file
                        File file = new File(filename);
                        file.delete();

                        //subtract bird points if they're not -1 or 0
                        if(birdPoints > 0){
                            MainActivity.subtractBirdPoints(birdPoints);
                        }

                        //remove the file from the imageList
                        for(int i = 0; i < MainActivity.imageList.size(); i++){
                            if(MainActivity.imageList.get(i).getFilename().equals(filename)){
                                MainActivity.imageList.remove(i);
                                break;
                            }
                        }

                        //update the gallery
                        GalleryFragment.getAdapter().notifyDataSetChanged();

                        //remove the marker from the map
                        MapFragment.removeImage(filename);

                        //return to mainactivity
                        getActivity().finish();

                        dialog.dismiss();
                    }
                })
                //on cancel
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }
}
