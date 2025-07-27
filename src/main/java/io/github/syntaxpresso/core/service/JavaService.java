package io.github.syntaxpresso.core.service;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.util.PathHelper;
import io.github.syntaxpresso.core.util.TSHelper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;

@Data
@RequiredArgsConstructor
public class JavaService {
  private final PathHelper pathHelper;
  private final TSHelper tsHelper;
  private static final String SRC_MAIN_JAVA = "src/main/java";
  private static final String SRC_TEST_JAVA = "src/test/java";

  public boolean isJavaProject(File rootDir) {
    if (rootDir == null || !rootDir.isDirectory()) {
      return false;
    }
    return Files.exists(rootDir.toPath().resolve("build.gradle"))
        || Files.exists(rootDir.toPath().resolve("build.gradle.kts"))
        || Files.exists(rootDir.toPath().resolve("pom.xml"))
        || Files.isDirectory(rootDir.toPath().resolve("src/main/java"));
  }

  public Optional<File> findFilePath(
      File rootDir, String packageName, SourceDirectoryType sourceDirectoryType) {
    if (rootDir == null || packageName == null || sourceDirectoryType == null) {
      return Optional.empty();
    }
    Path packageAsPath = Path.of(packageName.replace('.', '/'));
    Optional<File> sourceDirOptional =
        (sourceDirectoryType == SourceDirectoryType.MAIN)
            ? this.pathHelper.findDirectoryRecursively(rootDir, SRC_MAIN_JAVA)
            : this.pathHelper.findDirectoryRecursively(rootDir, SRC_TEST_JAVA);
    if (sourceDirOptional.isEmpty()) {
      return Optional.empty();
    }
    File fullPackageDir = sourceDirOptional.get().toPath().resolve(packageAsPath).toFile();
    if (fullPackageDir.exists() || fullPackageDir.mkdirs()) {
      return Optional.of(fullPackageDir);
    }
    return Optional.empty();
  }

  public Boolean isMainClass(TSTree tree, String sourceCode) {
    String mainMethodQuery =
        "(class_declaration  body: (class_body    (method_declaration       (modifiers) @mods      "
            + " type: (void_type)       name: (identifier) @name       parameters:"
            + " (formal_parameters         [          (formal_parameter type: (array_type element:"
            + " (type_identifier) @param_type))          (spread_parameter (type_identifier)"
            + " @param_type)        ]      )     )  ))";
    TSQuery query = new TSQuery(this.tsHelper.getParser().getLanguage(), mainMethodQuery);
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
    TSQuery query = new TSQuery(this.tsHelper.getParser().getLanguage(), packageQuery);
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
