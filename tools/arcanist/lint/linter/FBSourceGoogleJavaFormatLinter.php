<?php
// Copyright 2004-present Facebook. All Rights Reserved.

/**
 * Linter that delegates styling to google java format:
 * https://github.com/google/google-java-format
 *
 * This is a highly opinionated Java formatting tool that saves us time by
 * eliminating the need to nit style, and apply complicated formatting manually
 * (such as how to break long lines)
 */
final class FBSourceGoogleJavaFormatLinter extends ArcanistFutureLinter {
  const LINT_FORMAT = 0;
  const LINT_JAVA_ERROR = 1;
  const LINT_UNKNOWN_ERROR = 2;

  private $jar;

  public function __construct(array $options) {
    $this->jar = idx($options, 'jar');
    if (!is_string($this->jar)) {
      throw new Exception(__CLASS__.' options is missing "jar"');
    }
  }

  public function getLinterName() {
    return 'GOOGLEJAVAFORMAT';
  }

  public function getLintNameMap() {
    return array(
      self::LINT_FORMAT => 'Format Mismatch',
      self::LINT_JAVA_ERROR => 'Formatter Error',
      self::LINT_UNKNOWN_ERROR => 'Unknown Error',
    );
  }

  public function getLintSeverityMap() {
    return array(
      self::LINT_FORMAT => ArcanistLintSeverity::SEVERITY_AUTOFIX,
      self::LINT_JAVA_ERROR => ArcanistLintSeverity::SEVERITY_ERROR,
      self::LINT_UNKNOWN_ERROR => ArcanistLintSeverity::SEVERITY_ADVICE,
    );
  }

  protected function buildFutures(array $paths) {
    $jar = Filesystem::resolvePath($this->jar, $this->getProjectRoot());
    $futures = array();
    foreach ($paths as $path) {
      $changed_lines = $this->getEngine()->getPathChangedLines($path);
      if ($changed_lines !== null && count($changed_lines) === 0) {
        // If we're looking at changes and it's all deletions, then do nothing.
        continue;
      }
      $futures[$path] = new ExecFuture(
        'java -jar %s %C %s',
        $jar,
        $changed_lines !== null
          ? '--lines '.implode(',', array_keys($changed_lines))
          : '',
        $this->getEngine()->getFilePathOnDisk($path));
      $futures[$path]->setCWD($this->getProjectRoot());
    }
    return $futures;
  }

  protected function resolveFuture($path, Future $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    if ($err) {
      $messages = self::parseErrorOutput($path, $future);
    } else {
      $messages = FormatterUtils::buildLintMessages(
        $path,
        $this->getLintMessageFullCode(self::LINT_FORMAT),
        $this->getLintMessageSeverity(self::LINT_FORMAT),
        $this->getLintMessageName(self::LINT_FORMAT),
        'The changes in this file are formatted inconsistently with our code style.'
          .' Applying the suggested fix from lint will fix this for you.'
          .' https://fburl.com/java-style',
        $this->getData($path),
        $stdout);
    }
    foreach ($messages as $message) {
      // google-java-format's `--lines` only outputs changes for the changed
      // lines. If we're getting a replacement outside of the this range,
      // then it must mean that code is moving around (i.e. sorting imports).
      // These should not be subject to changed line filtering.
      $message->setBypassChangedLineFiltering(true);
      $this->addLintMessage($message);
    }
  }

  private function parseErrorOutput($path, Future $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    $messages = array();
    $lines = phutil_split_lines($stderr, false);
    foreach ($lines as $line) {
      $matches = null;
      preg_match(
        '/^(?P<file>[^:]+):(?:(?P<line>\d+):)?(?:(?P<column>\d+):)?(?:\s*)?error: (?P<message>.*)$/',
        $line,
        $matches);
      if ($matches) {
        $messages[] = id(new ArcanistLintMessage())
          ->setPath($path)
          ->setLine((int)$matches['line'])
          ->setChar((int)$matches['column'] - 1)
          ->setCode($this->getLintMessageFullCode(self::LINT_JAVA_ERROR))
          ->setName($this->getLintMessageName(self::LINT_JAVA_ERROR))
          ->setDescription($matches['message'])
          ->setSeverity($this->getLintMessageSeverity(self::LINT_JAVA_ERROR));
      }
    }
    if (!$messages) {
      $exception = new CommandException(
        sprintf('google-java-format exit code %s.', $err),
        $future->getCommand(),
        $err,
        $stdout,
        $stderr);
      $messages[] = id(new ArcanistLintMessage())
        ->setPath($path)
        ->setCode($this->getLintMessageFullCode(self::LINT_UNKNOWN_ERROR))
        ->setName($this->getLintMessageName(self::LINT_UNKNOWN_ERROR))
        ->setDescription($exception->getMessage())
        ->setSeverity($this->getLintMessageSeverity(self::LINT_UNKNOWN_ERROR));
    }
    return $messages;
  }
}
