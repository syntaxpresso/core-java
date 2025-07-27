package io.github.syntaxpresso.core.service;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.service.extra.ScopeType;
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

  public Optional<TSNode> findDeclarationNode(File file, int line, int column) {
    Optional<String> sourceCodeOpt = this.pathHelper.getFileSourceCode(file);
    if (sourceCodeOpt.isEmpty()) {
      return Optional.empty();
    }
    String sourceCode = sourceCodeOpt.get();

    Optional<TSTree> treeOpt = this.tsHelper.parse(sourceCode);
    if (treeOpt.isEmpty()) {
      return Optional.empty();
    }
    TSTree tree = treeOpt.get();

    Optional<TSNode> startingNodeOpt = this.tsHelper.getNodeAtPosition(tree, line, column);
    if (startingNodeOpt.isEmpty()) {
      return Optional.empty();
    }

    TSNode startingNode = startingNodeOpt.get();
    String symbolName =
        sourceCode.substring(startingNode.getStartByte(), startingNode.getEndByte());

    String declarationQuery =
        """
        [
          (local_variable_declaration
            declarator: (variable_declarator
              name: (identifier) @name)) @declaration

          (formal_parameter
            name: (identifier) @name) @declaration

          (field_declaration
            declarator: (variable_declarator
              name: (identifier) @name)) @declaration

          (class_declaration
              name: (identifier) @name) @declaration
        ]
        """;
    TSQuery query = new TSQuery(this.tsHelper.getParser().getLanguage(), declarationQuery);
    TSQueryCursor cursor = new TSQueryCursor();

    TSNode scopeNode = startingNode;
    while (scopeNode != null) {
      cursor.exec(query, scopeNode);
      TSQueryMatch match = new TSQueryMatch();
      TSNode bestCandidate = null;

      while (cursor.nextMatch(match)) {
        TSNode declarationCandidate = null;
        TSNode nameCandidate = null;
        for (TSQueryCapture capture : match.getCaptures()) {
          String captureName = query.getCaptureNameForId(capture.getIndex());
          if ("declaration".equals(captureName)) {
            declarationCandidate = capture.getNode();
          } else if ("name".equals(captureName)) {
            nameCandidate = capture.getNode();
          }
        }

        if (nameCandidate != null && declarationCandidate != null) {
          String capturedName =
              sourceCode.substring(nameCandidate.getStartByte(), nameCandidate.getEndByte());
          if (capturedName.equals(symbolName)) {
            if (bestCandidate == null
                || declarationCandidate.getStartByte() > bestCandidate.getStartByte()) {
              if (startingNode.equals(nameCandidate)) {
                return Optional.of(declarationCandidate);
              }
              if (declarationCandidate.getStartByte() < startingNode.getStartByte()) {
                bestCandidate = declarationCandidate;
              }
            }
          }
        }
      }
      if (bestCandidate != null) {
        return Optional.of(bestCandidate);
      }
      scopeNode = scopeNode.getParent();
    }
    return Optional.empty();
  }

  public Optional<ScopeType> getNodeScope(TSNode node) {
    if (node == null) {
      return Optional.empty();
    }
    String nodeType = node.getType();
    if ("local_variable_declaration".equals(nodeType) || "formal_parameter".equals(nodeType)) {
      return Optional.of(ScopeType.LOCAL);
    }
    boolean isPublic = false;
    for (int i = 0; i < node.getChildCount(); i++) {
      TSNode child = node.getChild(i);
      if ("modifiers".equals(child.getType())) {
        for (int j = 0; j < child.getChildCount(); j++) {
          if ("public".equals(child.getChild(j).getType())) {
            isPublic = true;
            break;
          }
        }
      }
      if (isPublic) {
        break;
      }
    }
    if ("class_declaration".equals(nodeType)
        || "interface_declaration".equals(nodeType)
        || "enum_declaration".equals(nodeType)
        || "record_declaration".equals(nodeType)
        || "annotation_type_declaration".equals(nodeType)) {
      return isPublic ? Optional.of(ScopeType.PROJECT) : Optional.of(ScopeType.CLASS);
    }
    if ("field_declaration".equals(nodeType) || "method_declaration".equals(nodeType)) {
      return isPublic ? Optional.of(ScopeType.PROJECT) : Optional.of(ScopeType.CLASS);
    }
    return Optional.empty();
  }
}
