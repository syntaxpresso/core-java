package io.github.syntaxpresso.core.service;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.util.PathHelper;
import java.io.File;
import java.io.IOException;
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

@Data
@RequiredArgsConstructor
public class JavaService {
  private final PathHelper pathHelper;

  public boolean isJavaProject(File rootDir) {
    if (rootDir == null || !rootDir.isDirectory()) {
      return false;
    }
    return Files.exists(rootDir.toPath().resolve("build.gradle"))
        || Files.exists(rootDir.toPath().resolve("build.gradle.kts"))
        || Files.exists(rootDir.toPath().resolve("pom.xml"))
        || Files.isDirectory(rootDir.toPath().resolve("src/main/java"));
  }

  public Optional<Path> findFilePath(
      Path rootDir, String packageName, SourceDirectoryType sourceDirectoryType) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return Optional.empty();
    }
    if (packageName == null || packageName.isBlank()) {
      return Optional.empty();
    }
    if (sourceDirectoryType == null) {
      return Optional.empty();
    }
    final String srcDirName =
        (sourceDirectoryType == SourceDirectoryType.MAIN) ? "src/main/java" : "src/test/java";
    Optional<Path> sourceDirOptional;
    try {
      sourceDirOptional = this.pathHelper.findDirectoryRecursively(rootDir, srcDirName);
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
    Path sourceDir;
    if (sourceDirOptional.isPresent()) {
      sourceDir = sourceDirOptional.get();
    } else {
      sourceDir = rootDir.resolve(srcDirName);
    }
    Path packageAsPath = Path.of(packageName.replace('.', '/'));
    Path fullPackageDir = sourceDir.resolve(packageAsPath);
    try {
      Files.createDirectories(fullPackageDir);
      return Optional.of(fullPackageDir);
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public Boolean isMainClass(TSFile file) {
    String mainMethodQuery =
        "(class_declaration  body: (class_body    (method_declaration       (modifiers) @mods      "
            + " type: (void_type)       name: (identifier) @name       parameters:"
            + " (formal_parameters         [          (formal_parameter type: (array_type element:"
            + " (type_identifier) @param_type))          (spread_parameter (type_identifier)"
            + " @param_type)        ]      )     )  ))";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), mainMethodQuery);
    TSQueryCursor queryCursor = new TSQueryCursor();
    queryCursor.exec(query, file.getTree().getRootNode());
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
        String methodName =
            file.getSourceCode().substring(nameNode.getStartByte(), nameNode.getEndByte());
        String paramType =
            file.getSourceCode()
                .substring(paramTypeNode.getStartByte(), paramTypeNode.getEndByte());
        String methodModifiers =
            file.getSourceCode().substring(modsNode.getStartByte(), modsNode.getEndByte());
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

  public Optional<String> getPackageName(TSFile file) {
    String packageQuery = "(package_declaration (scoped_identifier) @package_name)";
    TSQuery query = new TSQuery(file.getParser().getLanguage(), packageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, file.getTree().getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode node = capture.getNode();
        String packageName = file.getTextFromRange(node.getStartByte(), node.getEndByte());
        return Optional.of(packageName);
      }
    }
    return Optional.empty();
  }
}
