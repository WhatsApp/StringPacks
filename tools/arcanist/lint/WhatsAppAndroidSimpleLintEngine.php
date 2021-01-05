<?php
// Copyright 2004-present Facebook. All Rights Reserved.

final class WhatsAppAndroidSimpleLintEngine extends WhatsAppAndroidLintEngine {
  public function buildLinters() {
    $linters = array();
    foreach (parent::buildLinters() as $linter) {
      $linters[] = $linter;
    }
    return $linters;
  }
}
