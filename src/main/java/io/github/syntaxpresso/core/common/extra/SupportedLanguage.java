package io.github.syntaxpresso.core.common.extra;

import lombok.Getter;
import org.treesitter.TSLanguage;
import org.treesitter.TreeSitterJava;

@Getter
public enum SupportedLanguage {
  JAVA(new TreeSitterJava(), ".java");

  private final TSLanguage language;
  private final String fileExtension;

  SupportedLanguage(TSLanguage language, String fileExtension) {
    this.language = language;
    this.fileExtension = fileExtension;
  }
}
