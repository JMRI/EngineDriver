package jmri.enginedriver.util;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import java.io.File;

import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;

public class BackgroundImageLoader {
    static final String activityName = "BackgroundImageLoader";

    private final String prefBackgroundImageFileName;
    private final String prefBackgroundImagePosition;
    private final boolean prefBackgroundImage;
    private final ImageView image;
    private final threaded_application mainapp;

    public BackgroundImageLoader(SharedPreferences myPrefs, threaded_application myMainapp, ImageView myImage) {
        image = myImage;
        mainapp = myMainapp;
        prefBackgroundImage = myPrefs.getBoolean("prefBackgroundImage", mainapp.getResources().getBoolean(R.bool.prefBackgroundImageDefaultValue));
        prefBackgroundImageFileName = myPrefs.getString("prefBackgroundImageFileName", mainapp.getResources().getString(R.string.prefBackgroundImageFileNameDefaultValue));
        prefBackgroundImagePosition = myPrefs.getString("prefBackgroundImagePosition", mainapp.getResources().getString(R.string.prefBackgroundImagePositionDefaultValue));
    }

    public void loadBackgroundImage() {
        try {
            File image_file = new File(prefBackgroundImageFileName);
            if (!prefBackgroundImagePosition.equals("TILE")) {
                image.setImageBitmap(BitmapFactory.decodeFile(image_file.getPath()));
                switch (prefBackgroundImagePosition) {
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
            } else {
                Bitmap bitmap = BitmapFactory.decodeFile(image_file.getPath());
                BitmapDrawable drawable = new BitmapDrawable(mainapp.getApplicationContext().getResources(), bitmap);
                drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                image.setImageDrawable(drawable);
                image.setScaleType(ImageView.ScaleType.FIT_XY);
            }
        } catch (Exception e) {
            threaded_application.logging(activityName + ": loadBackgroundImageImpl(): failed loading background image");
        }
    }
}
