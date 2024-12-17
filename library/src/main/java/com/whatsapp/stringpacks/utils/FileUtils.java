/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks.utils;

import androidx.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

  public static int copyStream(@NonNull InputStream in, @NonNull OutputStream out)
      throws IOException {
    return copyStream(in, out, 4096);
  }

  private static int copyStream(@NonNull InputStream in, @NonNull OutputStream out, int bufferSize)
      throws IOException {
    byte[] buffer = new byte[bufferSize];
    int size = 0;
    int n;
    while ((n = in.read(buffer)) >= 0) {
      out.write(buffer, 0, n);
      size += n;
    }
    return size;
  }
}
