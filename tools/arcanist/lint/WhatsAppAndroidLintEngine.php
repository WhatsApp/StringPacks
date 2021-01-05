<?php
// Copyright 2004-present Facebook. All Rights Reserved.

abstract class WhatsAppAndroidLintEngine extends ArcanistLintEngine {
  public function buildLinters() {
    $linters = array();
    $paths = $this->getPaths();

    // ArcanistGeneratedLinter stops other linters from
    // running on generated code.
    $linters[] = id(new ArcanistGeneratedLinter())
      ->setPaths($paths);

    // ArcanistNoLintLinter stops other linters from
    // running on code marked with a nolint annotation.
    $linters[] = id(new ArcanistNoLintLinter())
      ->setPaths($paths);

    // These check for Arcanist correctness.
    $xhp_ast_linter = new ArcanistXHPASTLinter();
    $linters[] = $xhp_ast_linter
      ->setCustomSeverityMap(array(
        ArcanistXHPASTLinter::LINT_RAGGED_CLASSTREE_EDGE =>
          ArcanistLintSeverity::SEVERITY_WARNING,
      ))
      ->setPaths(preg_grep('@^tools/arcanist/.*\.php$@', $paths));
    $linters[] = id(new ArcanistPhutilXHPASTLinter())
      ->setXHPASTLinter($xhp_ast_linter)
      ->setPaths(preg_grep('@^tools/arcanist/.*\.php$@', $paths));

    $linters[] = id(new WhatsAppAndroidPythonFormatLinter())
      ->setPaths(preg_grep('@\.py$@', $paths));

    $linters[] = id(new WhatsAppAndroidKotlinFormatLinter())
      ->setPaths(preg_grep('@\.kt$@', $paths));

    $linters[] = id(new FBSourceGoogleJavaFormatLinter(array(
      'jar' => 'tools/third-party/google-java-format/google-java-format-1.7-all-deps.jar',
    )))->setPaths(preg_grep('@\.java$@', $paths));

    return $linters;
  }
}
