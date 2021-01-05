/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.sample;

import android.app.Application;
import android.content.Context;
import com.whatsapp.stringpacks.StringPackContext;
import com.whatsapp.stringpacks.StringPacks;

public class SampleApplication extends Application {

  @Override
  protected void attachBaseContext(Context base) {
    StringPackIds.registerStringPackIds();
    StringPacks.getInstance().setUp(base);
    super.attachBaseContext(StringPackContext.wrap(base));
  }
}
