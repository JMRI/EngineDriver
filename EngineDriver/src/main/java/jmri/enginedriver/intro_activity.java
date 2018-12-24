package jmri.enginedriver;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

//import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import jmri.enginedriver.util.PermissionsHelper;

public class intro_activity extends AppIntro2 {
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

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        originalPrefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
        originalPrefThrottleType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        // Note here that we DO NOT use setContentView();

        SliderPage sliderPage0 = new SliderPage();
        sliderPage0.setTitle(getApplicationContext().getResources().getString(R.string.introWelcomeTitle));
        sliderPage0.setDescription(getApplicationContext().getResources().getString(R.string.introWelcomeSummary));
        sliderPage0.setImageDrawable(R.drawable.ed_to_loco);
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

        if (!PermissionsHelper.getInstance().isPermissionGranted(intro_activity.this, PermissionsHelper.WRITE_SETTINGS)) {
            Fragment fragment3 = new intro_write_settings();
            addSlide(fragment3);
        }

        Fragment fragment0 = new intro_throttle_name();
        addSlide(fragment0);
        Fragment fragment1 = new intro_theme();
        addSlide(fragment1);
        Fragment fragment2 = new intro_throttle_type();
        addSlide(fragment2);
        Fragment fragment3 = new intro_buttons();
        addSlide(fragment3);

        SliderPage sliderPage99 = new SliderPage();
        sliderPage99.setTitle(getApplicationContext().getResources().getString(R.string.introFinishTitle));
        sliderPage99.setDescription(getApplicationContext().getResources().getString(R.string.introFinishSummary));
        sliderPage99.setImageDrawable(R.drawable.icon_xl);
        sliderPage99.setBgColor(getResources().getColor(R.color.intro_background));
        addSlide(AppIntroFragment.newInstance(sliderPage99));


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

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);


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
        this.finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}

