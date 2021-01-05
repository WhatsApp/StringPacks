<?php
// This software contains information and intellectual property that is
// confidential and proprietary to Facebook, Inc. and its affiliates.

/**
 * Common functionality used by linters that shell out to other tools.
 */
final class ExternalLinterUtils extends Phobject {
  const COMMAND_EXCEPTION = 'command-exception';

  private static $realVCSRoot = null;

  /**
   * The difference between `getRealVCSRoot` and `getVCSRoot` is subtle.
   * `getRealVCSRoot` is *always* the fbsource root. It should be used when
   * referring to things that are checked into the repo. `getVCSRoot` is the
   * fbsource root only when running `arc lint`, but it's a temp directory
   * when running unit tests. It should be used when resolving paths provided
   * by the user or by the Lint Engine.
   */
  static public function getRealVCSRoot() {
    if (self::$realVCSRoot === null) {
      self::$realVCSRoot = realpath(__DIR__.'/../../..');
    }
    return self::$realVCSRoot;
  }

  /**
   * This Lint Message should be used when a linter fails and we don't know
   * why. It's description is 1000 chars of stdout and stderr. It should
   * be thought of as a debug message. The strings matching the repo root and
   * temp dir paths will be masked as $ROOT_DIR and $TMP, respectively.
   */
  static public function newCommandExceptionMessage(
      $path,
      $code,
      ArcanistWorkingCopyIdentity $working_copy,
      ExecFuture $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    $command = $future->getCommand();

    // Include all of the output in the `--trace` log:
    PhutilConsole::getConsole()->writeLog(
      "--- %s COMMAND (exit code %s) ---\n%s\n".
      "--- %s STDOUT ---\n%s\n".
      "--- %s STDERR ---\n%s\n",
      $code, $err, $command,
      $code, $stdout,
      $code, $stderr);

    // Prefer the repo root, but fallback to the project root (like during
    // unit tests).
    $root_dir = $working_copy->getVCSRoot();
    if (!$root_dir) {
      $root_dir = $working_copy->getProjectRoot();
    }
    $sys_temp_dir = sys_get_temp_dir();
    // `sys_get_temp_dir` may not be the system temp dir, but some value
    // configured in `php.ini`.
    $real_temp_dir = getenv('TMPDIR');
    if (!$real_temp_dir) {
      $real_temp_dir = getenv('TEMP');
    }
    if (!$real_temp_dir) {
      $real_temp_dir = getenv('TMP');
    }
    // On Macs this is a symlink (i.e. `/tmp` -> `/private/tmp`).
    if ($real_temp_dir) {
      $real_temp_dir = realpath($real_temp_dir);
    }

    $mask_paths = function($text) use (
        $root_dir,
        $real_temp_dir,
        $sys_temp_dir) {
      $masked = $text;
      $masked = str_replace($root_dir, '$ROOT_DIR', $masked);
      $masked = str_replace($real_temp_dir, '$TMP', $masked);
      $masked = str_replace($sys_temp_dir, '$TMP', $masked);
      return $masked;
    };

    $exception = new CommandException(
      sprintf('Command failed with exit code #%s', $err),
      $mask_paths($command),
      $err,
      $mask_paths($stdout),
      $mask_paths($stderr));
    $description = $exception->getMessage();

    return id(new ArcanistLintMessage())
      ->setPath($path)
      ->setLine(null)
      ->setChar(null)
      ->setCode($code)
      ->setSeverity(ArcanistLintSeverity::SEVERITY_ERROR)
      ->setName(self::COMMAND_EXCEPTION)
      ->setDescription($description);
  }

  static public function maskRootDir(
      ArcanistWorkingCopyIdentity $working_copy,
      $text) {
    // Prefer the repo root, but fallback to the project root
    // (like during unit tests).
    $root_dir = $working_copy->getVCSRoot();
    if (!$root_dir) {
      $root_dir = $working_copy->getProjectRoot();
    }
    return str_replace($root_dir, '$ROOT_DIR', $text);
  }
}
