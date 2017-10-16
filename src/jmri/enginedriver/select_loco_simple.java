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
 */

package jmri.enginedriver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jmri.enginedriver.Consist.ConLoco;
import jmri.jmrit.roster.RosterEntry;

public class select_loco_simple extends select_loco {

    public void onCreate(Bundle savedInstanceState) {

        super.layoutViewId =R.layout.select_loco_simple;
        super.onCreate(savedInstanceState);

        Button button = (Button) findViewById(R.id.Sl_release_3);
        button.setOnClickListener(new release_button_listener(3));

        button = (Button) findViewById(R.id.Sl_release_4);
        button.setOnClickListener(new release_button_listener(4));

        button = (Button) findViewById(R.id.Sl_release_5);
        button.setOnClickListener(new release_button_listener(5));
    }

    // lookup and set values of various text labels
    protected void set_labels() {
        super.set_labels();

//        TextView vH = (TextView) findViewById(R.id.throttle_name_header);
//        // show throttle name
//        String s = "Throttle Name: "
//                + mainapp.prefs.getString("throttle_name_preference", this.getResources().getString(R.string.prefThrottleNameDefaultValue));
//        vH.setText(s);

        // format and show currently selected locos, and hide or show Release buttons
        final int conNomTextSize = 16;
        final int conSmallTextSize = 10;

        TextView vS = (TextView) findViewById(R.id.Sl_loco_3);
        Button bR = (Button) findViewById(R.id.Sl_release_3);

        for (int i = 3; i < 6; i++) {
            switch (i)
            {
                case 4:
                    vS = (TextView) findViewById(R.id.Sl_loco_4);
                    bR = (Button) findViewById(R.id.Sl_release_4);
                    break;
                case 5:
                    vS = (TextView) findViewById(R.id.Sl_loco_5);
                    bR = (Button) findViewById(R.id.Sl_release_5);
                    break;
            }

            vS.setVisibility(View.GONE);
            bR.setVisibility(View.GONE);
        }

        vS = (TextView) findViewById(R.id.Sl_loco_3);
        bR = (Button) findViewById(R.id.Sl_release_3);

        for (int i = 3; i < mainapp.numThrottles; i++) {
            switch (i) {
                case 4:
                    vS = (TextView) findViewById(R.id.Sl_loco_4);
                    bR = (Button) findViewById(R.id.Sl_release_4);
                    break;
                case 5:
                    vS = (TextView) findViewById(R.id.Sl_loco_5);
                    bR = (Button) findViewById(R.id.Sl_release_5);
                    break;
            }

            vS.setVisibility(View.VISIBLE);
            bR.setVisibility(View.VISIBLE);

            if (mainapp.consists[i].isActive()) {
                String vLabel = mainapp.consists[i].toString();
                int vWidth = vS.getWidth();                // scale text if required to fit the textView
                vS.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
                double textWidth = vS.getPaint().measureText(vLabel);
                if (vWidth == 0)
                    selectLocoRendered = false;
                else {
                    selectLocoRendered = true;
                    if (textWidth > vWidth) {
                        vS.setTextSize(TypedValue.COMPLEX_UNIT_SP, conSmallTextSize);
                    }
                }
                vS.setText(vLabel);
                bR.setEnabled(true);
            } else {
                vS.setText("");
                bR.setEnabled(false);
            }
        }

//        TextView vST = (TextView) findViewById(R.id.sl_loco_T);
//        Button bRT = (Button) findViewById(R.id.sl_release_T);
//        if (mainapp.consistT.isActive()) {
//            String vLabel = mainapp.consistT.toString();
//            int vWidth = vST.getWidth();                // scale text if required to fit the textView
//            vST.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
//            double textWidth = vST.getPaint().measureText(vLabel);
//            if (vWidth == 0)
//                selectLocoRendered = false;
//            else {
//                selectLocoRendered = true;
//                if (textWidth > vWidth) {
//                    vST.setTextSize(TypedValue.COMPLEX_UNIT_SP, conSmallTextSize);
//                }
//            }
//            vST.setText(vLabel);
//            bRT.setEnabled(true);
//        } else {
//            vST.setText("");
//            bRT.setEnabled(false);
//        }
//
//        TextView vSS = (TextView) findViewById(R.id.sl_loco_S);
//        Button bRS = (Button) findViewById(R.id.sl_release_S);
//        if (mainapp.consistS.isActive()) {
//            String vLabel = mainapp.consistS.toString();
//            int vWidth = vSS.getWidth();                // scale text if required to fit the textView
//            vSS.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
//            double textWidth = vSS.getPaint().measureText(vLabel);
//            if (vWidth == 0)
//                selectLocoRendered = false;
//            else {
//                selectLocoRendered = true;
//                if (textWidth > vWidth) {
//                    vSS.setTextSize(TypedValue.COMPLEX_UNIT_SP, conSmallTextSize);
//                }
//            }
//            vSS.setText(vLabel);
//            bRS.setEnabled(true);
//        } else {
//            vSS.setText("");
//            bRS.setEnabled(false);
//        }
//
//        TextView vSG = (TextView) findViewById(R.id.sl_loco_G);
//        Button bRG = (Button) findViewById(R.id.sl_release_G);
//        if (mainapp.consistG.isActive()) {
//            String vLabel = mainapp.consistG.toString();
//            int vWidth = vSG.getWidth();                // scale text if required to fit the textView
//            vSG.setTextSize(TypedValue.COMPLEX_UNIT_SP, conNomTextSize);
//            double textWidth = vSG.getPaint().measureText(vLabel);
//            if (vWidth == 0)
//                selectLocoRendered = false;
//            else {
//                selectLocoRendered = true;
//                if (textWidth > vWidth) {
//                    vSG.setTextSize(TypedValue.COMPLEX_UNIT_SP, conSmallTextSize);
//                }
//            }
//            vSG.setText(vLabel);
//            bRG.setEnabled(true);
//        } else {
//            vSG.setText("");
//            bRG.setEnabled(false);
//        }

        // only show loco text and release buttons for allowed # of locos
//        String numThrot = mainapp.prefs.getString("NumThrottle", getResources().getString(R.string.prefNumOfThrottlesDefault));
//        if ("One".equals(numThrot) || "Two".equals(numThrot)) {
//            vSG.setVisibility(View.GONE);
//            bRG.setVisibility(View.GONE);
//
//            if ("One".equals(numThrot)) {
//                vSS.setVisibility(View.GONE);
//                bRS.setVisibility(View.GONE);
//            }
//        }
    }
}
