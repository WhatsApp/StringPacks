/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import androidx.annotation.NonNull;
import java.util.concurrent.atomic.AtomicReference;

public class SpLog {
  private static AtomicReference<Logger> loggerRef = new AtomicReference<>(null);

  /** The client can set a logger here. Otherwise StringPacks will not do any logging */
  public static void setLogger(@NonNull Logger logger) {
    loggerRef.set(logger);
  }

  public static void e(String message) {
    final Logger logger = loggerRef.get();
    if (logger != null) {
      logger.e(message);
    }
  }

  public static void w(String message) {
    final Logger logger = loggerRef.get();
    if (logger != null) {
      logger.w(message);
    }
  }

  public static void i(String message) {
    final Logger logger = loggerRef.get();
    if (logger != null) {
      logger.i(message);
    }
  }

  public static void d(String message) {
    final Logger logger = loggerRef.get();
    if (logger != null) {
      logger.d(message);
    }
  }
}
