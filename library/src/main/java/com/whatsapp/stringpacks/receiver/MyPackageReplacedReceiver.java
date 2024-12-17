/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.whatsapp.stringpacks.service.PackFileDeletionService;

/**
 * Receiver that listens to system broadcast event <a
 * href="https://developer.android.com/reference/android/content/Intent#ACTION_MY_PACKAGE_REPLACED">MY_PACKAGE_REPLACED</a>,
 * which is generated by the system when the application is updated.
 *
 * <p>When this broadcast event is received, we cleanup all the .pack files, from internal storage,
 * that were created by the previous build, in a {@code Service}
 */
public class MyPackageReplacedReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent serviceIntent = new Intent(context, PackFileDeletionService.class);
    serviceIntent.setAction(PackFileDeletionService.ACTION_PACK_FILE_DELETE);

    PackFileDeletionService.start(context, serviceIntent);
  }
}
