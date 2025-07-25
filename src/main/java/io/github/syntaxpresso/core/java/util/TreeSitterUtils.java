package io.github.syntaxpresso.core.java.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
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

  public Boolean isMainClass(TSTree tree, String sourceCode) {
    String mainMethodQuery =
        "(class_declaration  body: (class_body    (method_declaration       (modifiers) @mods      "
            + " type: (void_type)       name: (identifier) @name       parameters:"
            + " (formal_parameters         [          (formal_parameter type: (array_type element:"
            + " (type_identifier) @param_type))          (spread_parameter (type_identifier)"
            + " @param_type)        ]      )     )  ))";
    TSQuery query = new TSQuery(this.parser.getLanguage(), mainMethodQuery);
    TSQueryCursor queryCursor = new TSQueryCursor();
    queryCursor.exec(query, tree.getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (queryCursor.nextMatch(match)) {
      Map<String, TSNode> captures = new HashMap<>();
      for (TSQueryCapture capture : match.getCaptures()) {
        String captureName = query.getCaptureNameForId(capture.getIndex());
        captures.put(captureName, capture.getNode());
      }
      TSNode nameNode = captures.get("name");
      TSNode modsNode = captures.get("mods");
      TSNode paramTypeNode = captures.get("param_type");
      if (nameNode != null && modsNode != null && paramTypeNode != null) {
        String methodName = sourceCode.substring(nameNode.getStartByte(), nameNode.getEndByte());
        String paramType =
            sourceCode.substring(paramTypeNode.getStartByte(), paramTypeNode.getEndByte());
        String methodModifiers =
            sourceCode.substring(modsNode.getStartByte(), modsNode.getEndByte());
        Set<String> modifiersSet =
            new HashSet<>(Arrays.asList(methodModifiers.trim().split("\\s+")));
        boolean hasCorrectModifiers =
            modifiersSet.size() == 2
                && modifiersSet.contains("public")
                && modifiersSet.contains("static");
        if (methodName.equals("main") && paramType.equals("String") && hasCorrectModifiers) {
          return true;
        }
      }
    }
    return false;
  }

  public Optional<String> getPackageName(TSTree tree, String sourceCode) {
    String packageQuery = "(package_declaration (scoped_identifier) @package_name)";
    TSQuery query = new TSQuery(this.parser.getLanguage(), packageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, tree.getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode node = capture.getNode();
        String packageName = sourceCode.substring(node.getStartByte(), node.getEndByte());
        return Optional.of(packageName);
      }
    }
    return Optional.empty();
  }
}
