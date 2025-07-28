package io.github.syntaxpresso.core.service;

import io.github.syntaxpresso.core.command.java.extra.SourceDirectoryType;
import io.github.syntaxpresso.core.service.extra.JavaIdentifierType;
import io.github.syntaxpresso.core.service.extra.ScopeType;
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
    if (sourceDirOptional.isEmpty()) {
      return Optional.empty();
    }
    Path packageAsPath = Path.of(packageName.replace('.', '/'));
    Path fullPackageDir = sourceDirOptional.get().resolve(packageAsPath);
    try {
      Files.createDirectories(fullPackageDir);
      return Optional.of(fullPackageDir);
    } catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public Boolean isMainClass(TSFile file) {
    if (!file.getIsValid()) {
      return false;
    }
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

  public Optional<TSNode> getPackageDeclarationNode(File file) {
    Optional<TSTree> tree = this.getTsHelper().parse(file);
    if (tree.isEmpty()) {
      return null;
    }
    TSNode rootNode = tree.get().getRootNode();
    String packageQuery = "(package_declaration (scoped_identifier) @package_name)";
    TSQuery query = new TSQuery(this.tsHelper.getParser().getLanguage(), packageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, rootNode);
    TSQueryMatch match = new TSQueryMatch();
    if (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode node = capture.getNode();
        return Optional.of(node);
      }
    }
    return Optional.empty();
  }

  public Optional<String> getPackageName(TSTree tree, String sourceCode) {
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

  public JavaIdentifierType getIdentifierType(TSNode node) {
    if (!"identifier".equals(node.getType())) {
      return null;
    }
    TSNode parent = node.getParent();
    if (parent == null) {
      return null;
    }
    String parentType = parent.getType();
    switch (parentType) {
      case "class_declaration":
        return JavaIdentifierType.CLASS_NAME;
      case "method_declaration":
        return JavaIdentifierType.METHOD_NAME;
      case "formal_parameter":
        return JavaIdentifierType.FORMAL_PARAMETER_NAME;
      case "variable_declarator":
        TSNode grandParent = parent.getParent();
        if (grandParent != null && "field_declaration".equals(grandParent.getType())) {
          return JavaIdentifierType.FIELD_NAME;
        }
        return JavaIdentifierType.LOCAL_VARIABLE_NAME;
      default:
        return null;
    }
  }

  public JavaIdentifierType getIdentifierType(File file, int line, int column) {
    Optional<TSNode> node = this.tsHelper.getNodeAtPosition(file, line, column);
    if (node.isEmpty()) {
      return null;
    }
    return this.getIdentifierType(node.get());
  }

  public List<TSNode> findClassUsages(String className, File projectRoot) {
    List<TSNode> allUsages = new ArrayList<>();
    List<File> allJavaFiles;
    try {
      allJavaFiles = pathHelper.findFilesByExtention(projectRoot, "java");
    } catch (IOException e) {
      // Handle the error appropriately
      return allUsages;
    }
    for (File file : allJavaFiles) {
      Optional<TSTree> treeOpt = tsHelper.parse(file);
      if (treeOpt.isPresent()) {
        allUsages.addAll(findValidatedUsagesInFile(treeOpt.get(), className, file));
      }
    }
    return allUsages;
  }

  /** Finds and validates all usages of a class within a single file's syntax tree. */
  private List<TSNode> findValidatedUsagesInFile(TSTree tree, String className, File file) {
    List<TSNode> confirmedUsages = new ArrayList<>();
    String sourceCode = pathHelper.getFileSourceCode(file).orElse("");
    String usageQuery = "((identifier) @usage (#eq? @usage \"" + className + "\"))";
    TSQuery query = new TSQuery(tsHelper.getParser().getLanguage(), usageQuery);
    TSQueryCursor cursor = new TSQueryCursor();
    cursor.exec(query, tree.getRootNode());
    TSQueryMatch match = new TSQueryMatch();
    while (cursor.nextMatch(match)) {
      for (TSQueryCapture capture : match.getCaptures()) {
        TSNode potentialUsage = capture.getNode();

        // Phase 2: Validate the potential usage.
        if (isUsageOfClass(potentialUsage, className, file)) {
          confirmedUsages.add(potentialUsage);
        }
      }
    }
    return confirmedUsages;
  }

  /**
   * Validates if a potential usage node actually refers to the target class. This is where you
   * would implement the logic to check imports, packages, etc.
   */
  private boolean isUsageOfClass(
      TSNode potentialUsage, String className, File fileContainingUsage) {
    // 1. Find the declaration of the potentialUsage node.
    Optional<TSNode> declarationNodeOpt =
        findDeclarationNode(
            fileContainingUsage,
            potentialUsage.getStartPoint().getRow() + 1, // Line is 1-based for this method
            potentialUsage.getStartPoint().getColumn() + 1 // Column is 1-based for this method
            );

    if (declarationNodeOpt.isEmpty()) {
      return false;
    }

    TSNode declarationNode = declarationNodeOpt.get();

    // 2. Check if the declaration is a class declaration and if the name matches.
    if ("class_declaration".equals(declarationNode.getType())) {
      // You'll need to get the text of the class name from the declaration node
      // and see if it matches your target `className`. This confirms the link.
      // This part of the logic requires a helper to extract the identifier from a declaration.
      return true;
    }

    return false;
  }
}
