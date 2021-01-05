<?php
// This software contains information and intellectual property that is
// confidential and proprietary to Facebook, Inc. and its affiliates.

// Adapted from the former FBSourcePythonFormatLinter.
final class WhatsAppAndroidPythonFormatLinter extends ArcanistFutureLinter {
  public function getLinterName() {
    return 'BLACK';
  }

  protected function buildFutures(array $paths) {
    $futures = array();
    foreach ($paths as $path) {
      $futures[$path] = id(new ExecFuture('pyfmt'))
             ->write($this->getData($path))
             ->setCWD($this->getProjectRoot())
             ->setEnv(array(
               'PATH' => '/usr/local/bin:/opt/facebook/bin:'.getenv('PATH'),
             ));
    }
    return $futures;
  }

  protected function resolveFuture($path, Future $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    if ($err) {
      $messages = array($this->parseErrorOutput($path, $future));
    } else {
      $messages = FormatterUtils::buildLintMessages(
        $path,
        'BLACK',
        ArcanistLintSeverity::SEVERITY_AUTOFIX,
        'format',
        'See https://fburl.com/fbsource-linters#black',
        $this->getData($path),
        $stdout);
    }
    foreach ($messages as $message) {
      $message->setBypassChangedLineFiltering(true);
      $this->addLintMessage($message);
    }
  }

  private function parseErrorOutput($path, Future $future) {
    list($err, $stdout, $stderr) = $future->resolve();
    $matches = null;
    preg_match(
      '/error: cannot format -: Cannot parse: (?P<line>\d+):(?P<column>\d+): (?P<message>.*)/',
      $stderr,
      $matches);
    if ($matches) {
      return id(new ArcanistLintMessage())
        ->setPath($path)
        ->setLine((int)$matches['line'])
        ->setChar((int)$matches['column'] + 1)
        ->setCode($this->getLinterName())
        ->setSeverity(ArcanistLintSeverity::SEVERITY_ERROR)
        ->setName('cannot-parse')
        ->setDescription('See https://fburl.com/fbsource-linters#black')
        ->setDescription($matches['message']);
    }
    preg_match(
      '/error: cannot format -: cannot use --safe with this file; (?P<message>.*) \(<unknown>, line (?P<line>\d+)\)/',
      $stderr,
      $matches);
    if ($matches) {
      return id(new ArcanistLintMessage())
        ->setPath($path)
        ->setLine((int)$matches['line'])
        ->setCode($this->getLinterName())
        ->setSeverity(ArcanistLintSeverity::SEVERITY_ERROR)
        ->setName('cannot-format')
        ->setDescription('See https://fburl.com/fbsource-linters#black')
        ->setDescription($matches['message']);
    } else {
      return ExternalLinterUtils::newCommandExceptionMessage(
        $path,
        $this->getLinterName(),
        $this->getEngine()->getWorkingCopy(),
        $future);
    }
  }
}
