/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.whatsapp.stringpacks.StringPackContext;
import com.whatsapp.stringpacks.StringPacks;

import java.util.Locale;

public class MainActivity extends Activity {

    private int mSelectedPos = 0;
    private static final String LANGUAGE_KEY = "language_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Need this to force reset the locale
        StringPacks.getInstance().setUp(getBaseContext());

        ((TextView) findViewById(R.id.text_view)).setText(R.string.hello_world);
        Spinner spinner = findViewById(R.id.language_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView .OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                if (mSelectedPos != pos) {
                    setNewLocale(parent.getItemAtPosition(pos).toString(), pos);
                    mSelectedPos = pos;
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
        Intent intent = getIntent();
        if (intent != null) {
            mSelectedPos = intent.getIntExtra(LANGUAGE_KEY, 0);
            spinner.setSelection(mSelectedPos);
        }
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(StringPackContext.wrap(base));
    }


    private void setNewLocale(String language, int pos) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources resources = getBaseContext().getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        Locale.setDefault(locale);
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Intent intent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt(LANGUAGE_KEY, pos);
        intent.putExtras(bundle);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
