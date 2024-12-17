/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package {package.name};

import com.whatsapp.stringpacks.StringPacks;

public class StringPackIds {

  public static void registerStringPackIds() {
    StringPacks.getInstance().register(getStringPacksMapping());
  }

  // region StringPacks ID Map
  // endregion
}
