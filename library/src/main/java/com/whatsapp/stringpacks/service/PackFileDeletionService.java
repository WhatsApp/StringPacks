/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.service;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import com.whatsapp.stringpacks.BuildConfig;
import com.whatsapp.stringpacks.SpLog;
import com.whatsapp.stringpacks.StringPacks;

/** Service that deletes the old pack files */
public class PackFileDeletionService extends JobIntentService {

  private static final int PACK_FILE_DELETE_JOB_ID = 1;

  public static final String ACTION_PACK_FILE_DELETE =
      BuildConfig.LIBRARY_PACKAGE_NAME + ".action.PACK_FILE_DELETE";

  public static void start(Context context, Intent intent) {
    JobIntentService.enqueueWork(
        context, PackFileDeletionService.class, PACK_FILE_DELETE_JOB_ID, intent);
  }

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    SpLog.d("PackFileDeletionService received intent; intent=" + intent);
    final String action = intent.getAction();
    if (action == null) {
      return;
    }
    if (action.equals(ACTION_PACK_FILE_DELETE)) {
      StringPacks.cleanupOldPackFiles(getApplicationContext());
    }
  }
}
