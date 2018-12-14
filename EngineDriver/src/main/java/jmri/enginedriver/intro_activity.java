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

public class intro_activity extends AppIntro2 {
    private SharedPreferences prefs;
    private String prefTheme  = "";
    private String prefThrottleType  = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Engine_Driver", "intro.onCreate()");

        prefs = getSharedPreferences("jmri.enginedriver_preferences", 0);
        prefTheme = prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue));
        prefThrottleType = prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault));

        // Note here that we DO NOT use setContentView();

        SliderPage sliderPage1 = new SliderPage();
        sliderPage1.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
        sliderPage1.setDescription(getApplicationContext().getResources().getString(R.string.permissionsReadPreferences));
        sliderPage1.setImageDrawable(R.drawable.icon_xl);
        //sliderPage.setBgColor(backgroundColor);
        addSlide(AppIntroFragment.newInstance(sliderPage1));
        askForPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        SliderPage sliderPage2= new SliderPage();
        sliderPage2.setTitle(getApplicationContext().getResources().getString(R.string.permissionsRequestTitle));
        sliderPage2.setDescription(getApplicationContext().getResources().getString(R.string.permissionsReadPhoneState));
        sliderPage2.setImageDrawable(R.drawable.icon_xl);
        addSlide(AppIntroFragment.newInstance(sliderPage2));
        askForPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 2);

        Fragment fragment0 = new intro_throttle_name();
        addSlide(fragment0);
        Fragment fragment1 = new intro_theme();
        addSlide(fragment1);
        Fragment fragment2 = new intro_throttle_type();
        addSlide(fragment2);

        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#3F51B5"));
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
        // Do something when users tap on Done button.
        if ( ( prefTheme != prefs.getString("prefTheme", getApplicationContext().getResources().getString(R.string.prefThemeDefaultValue)))
        || ( prefTheme != prefs.getString("prefThrottleScreenType", getApplicationContext().getResources().getString(R.string.prefThrottleScreenTypeDefault))) ) {
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

