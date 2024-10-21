package jmri.enginedriver.util;

import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class BackgroundImageLoader {
    private String prefBackgroundImageFileName;
    private String prefBackgroundImagePosition;
    private boolean prefBackgroundImage;
    private SharedPreferences prefs;
    private ImageView image;
    private threaded_application mainapp;

    public BackgroundImageLoader(SharedPreferences myPrefs, threaded_application myMainapp, ImageView myImage) {
        prefs = myPrefs;
        image = myImage;
        mainapp = myMainapp;
        prefBackgroundImage = prefs.getBoolean("prefBackgroundImage", mainapp.getResources().getBoolean(R.bool.prefBackgroundImageDefaultValue));
        prefBackgroundImageFileName = prefs.getString("prefBackgroundImageFileName", mainapp.getResources().getString(R.string.prefBackgroundImageFileNameDefaultValue));
        prefBackgroundImagePosition = prefs.getString("prefBackgroundImagePosition", mainapp.getResources().getString(R.string.prefBackgroundImagePositionDefaultValue));
    }

    public void loadBackgroundImage() {
        if (prefBackgroundImage) {
            boolean result = false;
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (PermissionsHelper.getInstance().isPermissionGranted(mainapp, PermissionsHelper.READ_IMAGES)) {
                    result = true;
                }
            } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (PermissionsHelper.getInstance().isPermissionGranted(mainapp, PermissionsHelper.READ_MEDIA_IMAGES)) {
                    result = true;
                }
            } else {
                if ((PermissionsHelper.getInstance().isPermissionGranted(mainapp, PermissionsHelper.READ_MEDIA_VISUAL_USER_SELECTED))
                   || (PermissionsHelper.getInstance().isPermissionGranted(mainapp, PermissionsHelper.READ_MEDIA_VISUAL_USER_SELECTED)) ) {
                    result = true;
                }
            }
            if (result) loadBackgroundImageImpl();
        }
    }

    protected void loadBackgroundImageImpl() {
        try {
//            File sdcard_path = Environment.getExternalStorageDirectory();
            File image_file = new File(prefBackgroundImageFileName);
//            File image_file = new File(getApplicationContext().getExternalFilesDir(null), prefBackgroundImageFileName);
            image.setImageBitmap(BitmapFactory.decodeFile(image_file.getPath()));
            switch (prefBackgroundImagePosition){
                case "FIT_CENTER":
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    break;
                case "CENTER_CROP":
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    break;
                case "CENTER":
                    image.setScaleType(ImageView.ScaleType.CENTER);
                    break;
                case "FIT_XY":
                    image.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case "CENTER_INSIDE":
                    image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    break;
            }
        } catch (Exception e) {
            Log.d("Engine_Driver", "backgroundImageLoader: failed loading background image");
        }
    }
}
