/*Copyright (C) 2017 M. Steve Todd mstevetodd@gmail.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

Derived from the samples for AppIntro at https://github.com/paolorotolo/AppIntro

*/

package jmri.enginedriver.intro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import eu.esu.mobilecontrol2.sdk.MobileControl2;
import jmri.enginedriver.R;
import jmri.enginedriver.threaded_application;
import jmri.enginedriver.type.activity_id_type;
import jmri.enginedriver.util.PermissionsHelper;

public class intro_activity extends AppIntro2 implements PermissionsHelper.PermissionsHelperGrantedCallback {
    static final String activityName = "intro_activity";

    private boolean introComplete = false;
    private SharedPreferences prefs;
    //    private String prefThrottleType  = "";
    private String originalPrefTheme  = "";
//    private String originalPrefThrottleType  = "";

    private threaded_application mainapp;    //pointer back to application

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(threaded_application.applicationName, activityName + ": onCreate()");

        mainapp = (threaded_application) this.getApplication();

        mainapp.introIsRunning = true;

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        originalPrefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
//        originalPrefThrottleType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        mainapp.getFakeDeviceId(true); // force getting a new ID

        // Note here that we DO NOT use setContentView();

        SliderPage sliderPage0 = new SliderPage();
        sliderPage0.setTitle(getApplicationContext().getResources().getString(R.string.introWelcomeTitle));
        sliderPage0.setDescription(getApplicationContext().getResources().getString(R.string.introWelcomeSummary));
        sliderPage0.setImageDrawable(R.drawable.intro_welcome);
        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
        addSlide(AppIntroFragment.newInstance(sliderPage0));

//        int slideNumber = 1;  // how many preceding slides
//
////<!-- needed for API 33 -->
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.POST_NOTIFICATIONS)) {
//                SliderPage sliderPage = new SliderPage();
//                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsPOST_NOTIFICATIONS));
//                sliderPage.setImageDrawable(R.drawable.icon_vector);
//                sliderPage.setBgColor(ContextCompat.getColor(context, R.color.intro_background));
//                addSlide(AppIntroFragment.newInstance(sliderPage));
//                slideNumber = slideNumber + 1;
//                askForPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, slideNumber);
//            }
//        }=
////<!-- needed for API 33 -->
//
////<!-- needed for API 33 -->
//        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
////<!-- needed for API 33 -->
//            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_IMAGES)) {
//                SliderPage sliderPage = new SliderPage();
//                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsREAD_IMAGES));
//                sliderPage.setImageDrawable(R.drawable.icon_vector);
//                sliderPage.setBgColor(getResources().getColor(R.color.intro_background));
//                addSlide(AppIntroFragment.newInstance(sliderPage));
//                slideNumber = slideNumber + 1;
//                askForPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, slideNumber);
//            }
////<!-- needed for API 33 -->
//        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_IMAGES)) {
//                SliderPage sliderPage = new SliderPage();
//                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_IMAGES));
//                sliderPage.setImageDrawable(R.drawable.icon_vector);
//                sliderPage.setBgColor(getResources().getColor(R.color.intro_background));
//                addSlide(AppIntroFragment.newInstance(sliderPage));
//                slideNumber = slideNumber + 1;
//                askForPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, slideNumber);
//            }
//        } else { // needed for API 34
//            if ( (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_IMAGES))
//                && (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_VISUAL_USER_SELECTED)) ) {
//
//                SliderPage sliderPage = new SliderPage();
//                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_VISUAL_USER_SELECTED));
//                sliderPage.setImageDrawable(R.drawable.icon_vector);
//                sliderPage.setBgColor(ContextCompat.getColor(context, R.color.intro_background));
//                addSlide(AppIntroFragment.newInstance(sliderPage));
//                slideNumber = slideNumber + 1;
//                askForPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}, slideNumber);
//            }
//        }
////<!-- needed for API 34 -->
//
//        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_PHONE_STATE)) {
//            SliderPage sliderPage = new SliderPage();
//            sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//            sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsReadPhoneState));
//            sliderPage.setImageDrawable(R.drawable.icon_vector);
//            sliderPage.setBgColor(getResources().getColor(R.color.intro_background));
//            addSlide(AppIntroFragment.newInstance(sliderPage));
//            slideNumber = slideNumber + 1;
//            askForPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, slideNumber);
//        }
//
//////<!-- needed for API 33 -->
////        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//////<!-- needed for API 33 -->
//            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.ACCESS_FINE_LOCATION)) {
//                SliderPage sliderPage = new SliderPage();
//                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsACCESS_FINE_LOCATION));
//                sliderPage.setImageDrawable(R.drawable.icon_vector);
//                sliderPage.setBgColor(getResources().getColor(R.color.intro_background));
//                addSlide(AppIntroFragment.newInstance(sliderPage));
//                slideNumber = slideNumber + 1;
//                askForPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, slideNumber);
//            }
//////<!-- needed for API 33 -->
////        } else {
////            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.NEARBY_WIFI_DEVICES)) {
////                SliderPage sliderPage = new SliderPage();
////                sliderPage.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
////                sliderPage.setDescription(getApplicationContext().getResources().getString(R.string.permissionsNEARBY_WIFI_DEVICES));
////                sliderPage.setImageDrawable(R.drawable.icon_vector);
////                sliderPage.setBgColor(getResources().getColor(R.color.intro_background));
////                addSlide(AppIntroFragment.newInstance(sliderPage));
////                slideNumber = slideNumber + 1;
////                askForPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, slideNumber);
////            }
////        }
//////<!-- needed for API 33 -->
//
////        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.VIBRATE )) {
////            SliderPage sliderPage5 = new SliderPage();
////            sliderPage5.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
////            sliderPage5.setDescription(getApplicationContext().getResources().getString(R.string.permissionsVIBRATE));
////            sliderPage5.setImageDrawable(R.drawable.icon_vector);
////            sliderPage5.setBgColor(getResources().getColor(R.color.intro_background));
////            addSlide(AppIntroFragment.newInstance(sliderPage5));
////            slideNumber = slideNumber + 1;
////            askForPermissions(new String[]{Manifest.permission.VIBRATE}, slideNumber);
////        }
//
//
//        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.WRITE_SETTINGS)) {
//            if (android.os.Build.VERSION.SDK_INT >= 23) {
//                Fragment fragment3 = new intro_write_settings();
//                addSlide(fragment3);
//            }
//        }

        Bundle args;
        Fragment fragment;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.POST_NOTIFICATIONS)) {
                args = new Bundle();
                args.putString("id", Integer.toString(PermissionsHelper.POST_NOTIFICATIONS));
                args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsPOST_NOTIFICATIONS));
                fragment = new intro_permissions();
                fragment.setArguments(args);
                addSlide(fragment);
            }
        }

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_IMAGES)) {
                args = new Bundle();
                args.putString("id", Integer.toString(PermissionsHelper.READ_IMAGES));
                args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsREAD_IMAGES));
                fragment = new intro_permissions();
                fragment.setArguments(args);
                addSlide(fragment);
            }
        } else if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_IMAGES)) {
                args = new Bundle();
                args.putString("id", Integer.toString(PermissionsHelper.READ_MEDIA_IMAGES));
                args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_IMAGES));
                fragment = new intro_permissions();
                fragment.setArguments(args);
                addSlide(fragment);
            }
        } else { // needed for API 34
            if ( (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_IMAGES))
                && (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_MEDIA_VISUAL_USER_SELECTED)) ) {
                args = new Bundle();
                args.putString("id", Integer.toString(PermissionsHelper.READ_MEDIA_VISUAL_USER_SELECTED));
                args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsREAD_MEDIA_VISUAL_USER_SELECTED));
                fragment = new intro_permissions();
                fragment.setArguments(args);
                addSlide(fragment);
            }
        }


        if (!MobileControl2.isMobileControl2()) {
            if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_PHONE_STATE)) {
                args = new Bundle();
                args.putString("id", Integer.toString(PermissionsHelper.READ_PHONE_STATE));
                args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsReadPhoneState));
                fragment = new intro_permissions();
                fragment.setArguments(args);
                addSlide(fragment);
            }
        }

        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.ACCESS_FINE_LOCATION)) {
            args = new Bundle();
            args.putString("id", Integer.toString(PermissionsHelper.ACCESS_FINE_LOCATION));
            args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsACCESS_FINE_LOCATION));
            fragment = new intro_permissions();
            fragment.setArguments(args);
            addSlide(fragment);
        }

        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.WRITE_SETTINGS)) {
//            args = new Bundle();
//            args.putString("id", Integer.toString(PermissionsHelper.WRITE_SETTINGS));
//            args.putString("label", getApplicationContext().getResources().getString(R.string.permissionsWriteSettings));
            fragment = new intro_write_settings();
//            fragment.setArguments(args);
            addSlide(fragment);
        }


        fragment = new intro_throttle_name();
        addSlide(fragment);
        fragment = new intro_theme();
        addSlide(fragment);
        if (MobileControl2.isMobileControl2()) {
            fragment = new intro_esu_mc2();
            addSlide(fragment);
        }
        fragment = new intro_throttle_type();
        addSlide(fragment);
        fragment = new intro_buttons();
        addSlide(fragment);
        fragment = new intro_dccex();
        addSlide(fragment);

        fragment = new intro_finish();
        addSlide(fragment);

//        SliderPage sliderPage99 = new SliderPage();
//        sliderPage99.setTitle(getApplicationContext().getResources().getString(R.string.introFinishTitle));
//        sliderPage99.setDescription(getApplicationContext().getResources().getString(R.string.introFinishSummary));
//        sliderPage99.setImageDrawable(R.drawable.icon_vector);
//        sliderPage99.setBgColor(getResources().getColor(R.color.intro_background));
//        addSlide(AppIntroFragment.newInstance(sliderPage99));


        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(getResources().getColor(R.color.intro_buttonbar_background));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        setVibrate(false);
        //setVibrateIntensity(30);
        setWizardMode(true);
    }


    @SuppressLint("SwitchIntDef")
    public void navigateToHandler(@PermissionsHelper.RequestCodes int requestCode) {
        Log.d(threaded_application.applicationName, activityName + ": navigateToHandler:" + requestCode);
        if (!PermissionsHelper.getInstance().isPermissionGranted(this, requestCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionsHelper.getInstance().requestNecessaryPermissions(this, requestCode);
            }
        } else {
            // do nothing
            Log.d(threaded_application.applicationName, activityName + ": Unrecognised permissions request code: " + requestCode);
        }
    }
    @Override
    public void onRequestPermissionsResult(@PermissionsHelper.RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.getInstance().processRequestPermissionsResult(this, requestCode, permissions, grantResults)) {
            Log.d(threaded_application.applicationName, activityName + ": onRequestPermissionsResult(): Unrecognised request - send up to super class");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        introComplete = true;
        SharedPreferences prefsNoBackup = mainapp.getSharedPreferences("jmri.enginedriver_preferences_no_backup", 0);
        prefsNoBackup.edit().putString("prefRunIntro", threaded_application.INTRO_VERSION).commit();

        String prefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
//        prefThrottleType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        if (!prefTheme.equals(originalPrefTheme))  {
            // the theme has changed so need to restart the app.
            Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            if (intent != null ) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            finish();
//            Runtime.getRuntime().exit(0); // really force the kill
        }
        mainapp.introIsRunning = false;
        this.finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    @Override
    public void onPause() {
        super.onPause();
        threaded_application.activityPaused(activityName);
    }

    @Override
    public void onResume() {
        threaded_application.currentActivity = activity_id_type.INTRO;
        super.onResume();
        threaded_application.activityResumed(activityName);
        mainapp.removeNotification(this.getIntent());

//        if (this.isFinishing()) {        //if finishing, expedite it
//            return;
//        }
    }


    @Override
    public void onDestroy() {
        Log.d(threaded_application.applicationName, activityName + ": onDestroy()");

        mainapp.introIsRunning = false;
        if (!introComplete) {
            threaded_application.safeToast(R.string.introbackButtonPress, Toast.LENGTH_LONG);
        }
        super.onDestroy();
    }
}

