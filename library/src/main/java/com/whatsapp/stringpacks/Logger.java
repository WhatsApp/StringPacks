/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

/** This allows the StringPacks consumer to pass down a custom logger. */
public interface Logger {
  void e(String message);

  void w(String message);

  void i(String message);

  void d(String message);
}
