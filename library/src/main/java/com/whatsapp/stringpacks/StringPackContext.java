/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;

public class StringPackContext extends ContextWrapper {

  public final StringPackResources spResources;

  public static StringPackContext wrap(Context context) {
    if (context instanceof StringPackContext) {
      return (StringPackContext) context;
    }

    return new StringPackContext(context);
  }

  StringPackContext(final Context base) {
    super(base);

    spResources = StringPackResources.wrap(base.getResources());
  }

  @Override
  public Context getBaseContext() {
    throw new UnsupportedOperationException(
        "Use base context would not guarantee to get strings from StringPacks. "
            + "Use getApplicationContext() would be proper in some cases.");
  }

  @Override
  public Resources getResources() {
    return spResources;
  }

  @Override
  public Context createConfigurationContext(Configuration overrideConfiguration) {
    return StringPackContext.wrap(super.createConfigurationContext(overrideConfiguration));
  }
}
