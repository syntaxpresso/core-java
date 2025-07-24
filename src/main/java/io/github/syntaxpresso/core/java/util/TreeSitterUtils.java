package io.github.syntaxpresso.core.java.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryCursor.TSMatchIterator;
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

  public Boolean isMainClass(TSTree tree) {
    String mainMethodQuery =
        ""
            + "(method_declaration "
            + "  (modifiers) @mods "
            + "  type: (void_type) "
            + "  name: (identifier) @name "
            + "  parameters: (formal_parameters "
            + "    (formal_parameter "
            + "      type: (array_type element: (type_identifier) @param_type) "
            + "    ) "
            + "  ) "
            + "  (#match? @mods \"public static\")"
            + "  (#eq? @name \"main\") "
            + "  (#eq? @param_type \"String\") "
            + ")";
    TSQuery query = new TSQuery(this.parser.getLanguage(), mainMethodQuery);
    TSQueryCursor queryCursor = new TSQueryCursor();
    queryCursor.exec(query, tree.getRootNode());
    TSMatchIterator captures = queryCursor.getCaptures();
    return captures.hasNext();
  }
}
