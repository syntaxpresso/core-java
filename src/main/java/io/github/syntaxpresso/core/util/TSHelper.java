package io.github.syntaxpresso.core.util;

import java.io.File;
import java.util.Optional;

import io.github.syntaxpresso.core.common.ParserFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

@AllArgsConstructor
@Data
public class TSHelper {
  private TSLanguage language;
  private TSParser parser;
  private PathHelper pathHelper;

  public TSHelper(TSLanguage language, PathHelper pathHelper) {
    this.parser = ParserFactory.get(language);
    this.pathHelper = pathHelper;
  }

  public Optional<TSTree> parse(String sourceCode) {
    TSTree tree = this.parser.parseString(null, sourceCode);
    if (!tree.getRootNode().hasError()) {
      return Optional.of(tree);
    }
    return Optional.empty();
  }

  public Optional<TSTree> parse(File file) {
    Optional<String> sourceCode = this.pathHelper.getFileSourceCode(file);
    if (sourceCode.isEmpty()) {
      return Optional.empty();
    }
    TSTree tree = this.parser.parseString(null, sourceCode.get());
    if (!tree.getRootNode().hasError()) {
      return Optional.of(tree);
    }
    return Optional.empty();
  }

  public boolean isSourceCodeValid(String sourceCode) {
    return this.parse(sourceCode).isPresent();
  }
}
