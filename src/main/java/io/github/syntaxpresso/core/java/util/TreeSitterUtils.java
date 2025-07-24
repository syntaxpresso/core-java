package io.github.syntaxpresso.core.java.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

@NoArgsConstructor
@Data
public class TreeSitterUtils {
  private final TSParser parser = JavaParser.get();

  public Optional<TSTree> parse(String sourceCode) {
    TSTree tree = this.parser.parseString(null, sourceCode);
    if (!tree.getRootNode().hasError()) {
      return Optional.of(tree);
    }
    return Optional.empty();
  }

  public Optional<TSTree> parse(Path filePath) {
    try {
      String sourceCode = Files.readString(filePath, StandardCharsets.UTF_8);
      return this.parse(sourceCode);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public Optional<TSTree> parse(byte[] buffer) {
    String sourceCode = new String(buffer, StandardCharsets.UTF_8);
    return this.parse(sourceCode);
  }

  public boolean isSourceCodeValid(String sourceCode) {
    return this.parse(sourceCode).isPresent();
  }
}
