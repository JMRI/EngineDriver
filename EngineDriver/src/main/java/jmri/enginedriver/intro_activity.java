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

package jmri.enginedriver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

//import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import jmri.enginedriver.util.PermissionsHelper;

public class intro_activity extends AppIntro2 {
    private boolean introComplete = false;
    private SharedPreferences prefs;
    private String prefTheme  = "";
    private String prefThrottleType  = "";
    private String originalPrefTheme  = "";
    private String originalPrefThrottleType  = "";

    private threaded_application mainapp;    //pointer back to application

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Engine_Driver", "intro_activity.onCreate()");

        mainapp = (threaded_application) this.getApplication();

        mainapp.introIsRunning = true;

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        originalPrefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
        originalPrefThrottleType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        // Note here that we DO NOT use setContentView();

        SliderPage sliderPage0 = new SliderPage();
        sliderPage0.setTitle(getApplicationContext().getResources().getString(R.string.introWelcomeTitle));
        sliderPage0.setDescription(getApplicationContext().getResources().getString(R.string.introWelcomeSummary));
        sliderPage0.setImageDrawable(R.drawable.intro_welcome);
        sliderPage0.setBgColor(getResources().getColor(R.color.intro_background));
        addSlide(AppIntroFragment.newInstance(sliderPage0));

        int slideNumber = 1;  // how many preceding slides
        if ( (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_CONNECTION_LIST)) ||
                (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.STORE_CONNECTION_LIST)) ) {
            SliderPage sliderPage1 = new SliderPage();
            sliderPage1.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
            sliderPage1.setDescription(getApplicationContext().getResources().getString(R.string.permissionsReadPreferences));
            sliderPage1.setImageDrawable(R.drawable.icon_xl);
            sliderPage1.setBgColor(getResources().getColor(R.color.intro_background));
            addSlide(AppIntroFragment.newInstance(sliderPage1));
            slideNumber = slideNumber + 1;
            askForPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, slideNumber);
        }

        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.READ_PHONE_STATE)) {
            SliderPage sliderPage2 = new SliderPage();
            sliderPage2.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
            sliderPage2.setDescription(getApplicationContext().getResources().getString(R.string.permissionsReadPhoneState));
            sliderPage2.setImageDrawable(R.drawable.icon_xl);
            sliderPage2.setBgColor(getResources().getColor(R.color.intro_background));
            addSlide(AppIntroFragment.newInstance(sliderPage2));
            slideNumber = slideNumber + 1;
            askForPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, slideNumber);
        }

        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.ACCESS_COARSE_LOCATION )) {
            SliderPage sliderPage3 = new SliderPage();
            sliderPage3.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
            sliderPage3.setDescription(getApplicationContext().getResources().getString(R.string.permissionsACCESS_COARSE_LOCATION));
            sliderPage3.setImageDrawable(R.drawable.icon_xl);
            sliderPage3.setBgColor(getResources().getColor(R.color.intro_background));
            addSlide(AppIntroFragment.newInstance(sliderPage3));
            slideNumber = slideNumber + 1;
            askForPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, slideNumber);
        }

        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.WRITE_SETTINGS)) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                Fragment fragment3 = new intro_write_settings();
                addSlide(fragment3);
//            } else {
//                SliderPage sliderPage3 = new SliderPage();
//                sliderPage3.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
//                sliderPage3.setDescription(getApplicationContext().getResources().getString(R.string.permissionsWriteSettings));
//                sliderPage3.setImageDrawable(R.drawable.icon_xl);
//                sliderPage3.setBgColor(getResources().getColor(R.color.intro_background));
//                addSlide(AppIntroFragment.newInstance(sliderPage3));
//                slideNumber = slideNumber + 1;
//                askForPermissions(new String[]{Manifest.permission.WRITE_SETTINGS}, slideNumber);
            }
        }

        Fragment fragment0 = new intro_throttle_name();
        addSlide(fragment0);
        Fragment fragment1 = new intro_theme();
        addSlide(fragment1);
        Fragment fragment2 = new intro_throttle_type();
        addSlide(fragment2);
        Fragment fragment3 = new intro_buttons();
        addSlide(fragment3);

        Fragment fragment99 = new intro_finish();
        addSlide(fragment99);

//        SliderPage sliderPage99 = new SliderPage();
//        sliderPage99.setTitle(getApplicationContext().getResources().getString(R.string.introFinishTitle));
//        sliderPage99.setDescription(getApplicationContext().getResources().getString(R.string.introFinishSummary));
//        sliderPage99.setImageDrawable(R.drawable.icon_xl);
//        sliderPage99.setBgColor(getResources().getColor(R.color.intro_background));
//        addSlide(AppIntroFragment.newInstance(sliderPage99));


        // OPTIONAL METHODS
        // Override bar/separator color.
//        setBarColor(Color.parseColor("#3F51B5"));
        setBarColor(getResources().getColor(R.color.intro_buttonbar_background));
//        setSeparatorColor(Color.parseColor("#2196F3"));

        // Hide Skip/Done button.
        showSkipButton(false);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(false);
        //setVibrateIntensity(30);
        setWizardMode(true);
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
        prefs.edit().putString("prefRunIntro", mainapp.INTRO_VERSION).commit();

        prefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
        prefThrottleType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        if ( (!prefTheme.equals(originalPrefTheme)) || (!prefTheme.equals(originalPrefThrottleType)) ) {

            // the theme has changed so need to restart the app.
            Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Runtime.getRuntime().exit(0); // really force the kill

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
        if (!this.isFinishing() && !mainapp.introIsRunning) {       //only invoke setContentIntentNotification when going into background
            mainapp.addNotification(this.getIntent());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mainapp.removeNotification();
//        if (this.isFinishing()) {        //if finishing, expedite it
//            return;
//        }
    }


    @Override
    public void onDestroy() {
        Log.d("Engine_Driver", "intro_activity.onDestroy() called");

        mainapp.introIsRunning = false;
        if (!introComplete) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.introbackButtonPress), Toast.LENGTH_LONG).show();
        }
        super.onDestroy();
    }
}

