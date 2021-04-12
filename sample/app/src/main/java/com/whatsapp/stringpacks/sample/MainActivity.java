/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.whatsapp.stringpacks.StringPackContext;
import com.whatsapp.stringpacks.StringPackResources;

public class MainActivity extends AppCompatActivity {

  TextView textView;

  private @Nullable StringPackResources stringPackResources;

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

  /**
   * This method needs to be overridden in an an Activity only if it matches all of the
   * following conditions
   * 1. minSdkVersion of the app is less than 17
   * 2. App has a dependency on androidx.appcompat:appcompat:1.2.0 or above (see
   * <a href="https://github.com/WhatsApp/StringPacks/blob/master/sample/app/build.gradle#L39">build.gradle</a>)
   * 3. Activity extends from {@code AppCompatActivity}
   *
   * <p>If any of the above constraints is false, there is no need to override this method
   *
   * @return StringPackResources that wraps base resources.
   */
  @Override
  public Resources getResources() {
    if (stringPackResources == null) {
      stringPackResources = StringPackResources.wrap(super.getResources());
    }
    return stringPackResources;
  }

  private void changeLanguage(String languageTag) {
    LocaleUtil.overrideCustomLanguage(this, languageTag);
    textView.setText(R.string.hello_world);
  }
}
