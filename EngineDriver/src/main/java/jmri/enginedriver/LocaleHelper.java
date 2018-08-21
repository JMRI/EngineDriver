/*
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
*/

package jmri.enginedriver;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Locale;

/*
This class is used to change the application locale.
It is not designed to allow change on the fly.  The app needs to be re-started.

Modified from code by gunhansancar on 07/10/15.
*/

// TODO: Fix the Locale change code so that in changes the Country as well as the Language.  For unknown reasons it ignores the Country.

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    public static String languageCountry = "";  // will contain the default language at launch
    private static String prefLocale ="";

    // needs to be called from each activity other than mainapp
    public static Context onAttach(Context context) {
        String lang = getLanguagePreference(context);
        return setLocale(context, lang);
    }

    // called once from the mainapp activity
    public static Context onAttach(Context context, String defaultLanguage) {
        if (languageCountry.equals("")) {
            languageCountry = defaultLanguage; // set the inital value
        }
        String lang = getLanguagePreference(context);
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String language) {

        if (!language.toUpperCase().equals(languageCountry.toUpperCase())) {   // if it as the same as the language at launch do nothing
            if (Build.VERSION.SDK_INT >= 17) {
                return updateResources(context, language);
            }
            return updateResourcesLegacy(context, language);
        } else {
            return context;
        }
    }

    private static String getLanguagePreference(Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefLocale = prefs.getString("prefLocale", context.getResources().getString(R.string.prefLocaleDefaultValue));

        if (prefLocale.equals("Default")) {
            prefLocale = languageCountry;
        }
        return prefLocale;
    }

    @TargetApi(17)
    private static Context updateResources(Context context, String language) {

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }
}