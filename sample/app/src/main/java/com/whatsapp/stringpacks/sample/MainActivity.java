/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.whatsapp.stringpacks.StringPackContext;

public class MainActivity extends Activity {

  TextView textView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    textView = findViewById(R.id.text_view);
    textView.setText(R.string.hello_world);

    setupLanguageSelector();
  }

  private void setupLanguageSelector() {
    Spinner spinner = findViewById(R.id.language_spinner);
    // TODO: display language full name instead of language code
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
      R.array.language_tags, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    // TODO: set selection to be current system language

    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        changeLanguage(parent.getItemAtPosition(pos).toString());
      }

      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(StringPackContext.wrap(base));
  }


  private void changeLanguage(String languageTag) {
    LocaleUtil.overrideCustomLanguage(this, languageTag);
    textView.setText(R.string.hello_world);
  }
}
