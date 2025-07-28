package io.github.syntaxpresso.core.common.extra;

import lombok.Getter;
import org.treesitter.TSLanguage;
import org.treesitter.TreeSitterJava;

@Getter
public enum SupportedLanguage {
  JAVA(new TreeSitterJava());

  private final TSLanguage language;

  SupportedLanguage(TSLanguage language) {
    this.language = language;
  }
}
