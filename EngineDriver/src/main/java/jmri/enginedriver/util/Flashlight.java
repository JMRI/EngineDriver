package jmri.enginedriver.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import jmri.enginedriver.R;

/**
 * Represents an on-device flashlight.
 *
 * Provides different methods to operate depending on the device API level
 *
 * @author Matthew Harris  Copyright (C) 2018.
 */

public abstract class Flashlight {
    private static Context flashlightContext;

    public static Flashlight newInstance(Context context) {
        flashlightContext = context;
        final int sdkVersion = Build.VERSION.SDK_INT;
        Flashlight flashlight;
        if (sdkVersion < Build.VERSION_CODES.M){
            flashlight = new FroyoFlashlight();
        } else {
            flashlight = new MarshmallowFlashlight();
        }
        flashlight.init();
        Log.d("Engine_Driver", "Created new " + flashlight.getClass());
        return flashlight;
    }

    /**
     * Allow for any needed initialisation for concrete implementations
     */
    protected abstract void init();

    /**
     * Allow for any needed teardown for concrete implementations
     */
    public abstract void teardown();

    /**
     * Check to see if a flashlight is available in this context
     *
     * @return true if a flashlight is available; false if not
     */
    public boolean isFlashlightAvailable() {
        return flashlightContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * Switch on the flashlight
     *
     * @param activity the requesting activity
     * @return true if the flashlight successfully switch on; false if unsuccessful
     */
    public abstract boolean setFlashlightOn(Activity activity);

    /**
     * Switch off the flashlight
     */
    public abstract void setFlashlightOff();

    /**
     * Concrete implementation for Froyo API8 (and later) devices
     *
     * This uses the legacy {@link android.hardware.Camera} API.
     *
     * On certain devices, we need to ensure that the orientation of the camera preview
     * matches that of the activity, otherwise 'bad things happen' using the newly available
     * {@link android.hardware.Camera#setDisplayOrientation(int)} method.
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    private static class FroyoFlashlight extends Flashlight {
        private static Camera camera;

        @Override
        protected void init() {
            // No specific initialisation needed - do nothing
        }

        @Override
        public void teardown() {
            // No specific teardown needed - do nothing
        }

        @Override
        public boolean setFlashlightOn(Activity activity) {
            try {
                camera = Camera.open();
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                camera.setDisplayOrientation(getDisplayOrientation(activity));
                camera.startPreview();
                Log.d("Engine_Driver", "Flashlight switched on");
                return true;
            } catch (Exception ex) {
                Log.e("Engine_Driver", "Error switching on flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOnFailed), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        @Override
        public void setFlashlightOff() {
            try {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                Log.d("Engine_Driver", "Flashlight switched off");
            } catch (Exception ex) {
                Log.e("Engine_Driver", "Error switching off flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOffFailed), Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Retrieves the screen orientation for the specified activity
         *
         * @param activity the requesting activity
         * @return screen orientation as integer number of degrees
         */
        private int getDisplayOrientation(Activity activity) {
            switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_0: return 0;
                case Surface.ROTATION_90: return 90;
                case Surface.ROTATION_180: return 180;
                case Surface.ROTATION_270: return 270;
                default: return 90;
            }
        }
    }

    /**
     * Concrete implementation for Marshmallow API 23 (and later) devices
     *
     * This uses the {@link android.hardware.camera2.CameraManager#setTorchMode(String, boolean)}
     * method now available in API 23 to greatly simplify things.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private static class MarshmallowFlashlight extends Flashlight {

        private static CameraManager cameraManager;
        private static String cameraId;

        @Override
        protected void init() {
            cameraManager = (CameraManager) flashlightContext.getSystemService(Context.CAMERA_SERVICE);
            try {
                cameraId = cameraManager.getCameraIdList()[0];
            } catch (CameraAccessException|SecurityException ex) {
                Log.e("Engine_Driver", "Error initiating camera manager: " + ex.getMessage());
            } catch (ArrayIndexOutOfBoundsException ex) {
                Log.e("Engine_Driver", "Error initiating camera manager: " + ex.getMessage());
            }

        }

        @Override
        public void teardown() {
            // No specific teardown needed - do nothing
        }

        @Override
        public boolean setFlashlightOn(Activity activity) {
            try {
                cameraManager.setTorchMode(cameraId, true);
                Log.d("Engine_Driver", "Flashlight switched on");
                return true;
            } catch (CameraAccessException ex) {
                Log.e("Engine_Driver", "Error switching on flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOnFailed), Toast.LENGTH_LONG).show();
                return false;
            } catch (IllegalArgumentException ex) {
                Log.e("Engine_Driver", "Problem switching on flashlight:" + ex.getMessage());
                return false;
            }
        }

        @Override
        public void setFlashlightOff() {
            try {
                cameraManager.setTorchMode(cameraId, false);
                Log.d("Engine_Driver", "Flashlight switched off");
            } catch (CameraAccessException ex) {
                Log.e("Engine_Driver", "Error switching off flashlight: " + ex.getMessage());
                Toast.makeText(flashlightContext, flashlightContext.getResources().getString(R.string.toastFlashlightOffFailed), Toast.LENGTH_LONG).show();
            } catch (IllegalArgumentException ex) {
                Log.e("Engine_Driver", "Problem switching off flashlight:" + ex.getMessage());
            }
        }
    }
}
