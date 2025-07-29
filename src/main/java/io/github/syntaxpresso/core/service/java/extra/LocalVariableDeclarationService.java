package io.github.syntaxpresso.core.service.java.extra;

import io.github.syntaxpresso.core.common.TSFile;
import java.util.Optional;
import org.treesitter.TSNode;

public class LocalVariableDeclarationService {
  /**
   * Finds the type node within a local variable declaration that matches a given name.
   *
   * @param declarationNode The TSNode for the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @param typeName The name of the type to find.
   * @return An Optional containing the found TSNode, or empty.
   */
  public Optional<TSNode> getVariableTypeNode(
      TSNode declarationNode, TSFile file, String typeName) {
    if (declarationNode == null
        || !"local_variable_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode typeNode = declarationNode.getChildByFieldName("type");
    if (typeNode != null) {
      String foundTypeName = file.getTextFromRange(typeNode.getStartByte(), typeNode.getEndByte());
      if (typeName.equals(foundTypeName)) {
        return Optional.of(typeNode);
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts the variable name from a local_variable_declaration node.
   *
   * @param declarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @return An Optional containing the variable name node, or empty if not found.
   */
  public Optional<TSNode> getVariableNameNode(TSNode declarationNode, TSFile file) {
    if (declarationNode == null
        || !"local_variable_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = declarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(variableDeclaratorNode.getChildByFieldName("name"));
  }

  /**
   * Extracts the instantiated class name's node from an object_creation_expression within a
   * local_variable_declaration node.
   *
   * @param declarationNode The TSNode representing the local_variable_declaration.
   * @param file The TSFile containing the source code.
   * @param className The name of the class being instantiated.
   * @return An Optional containing the instantiated class name node, or empty if not found.
   */
  public Optional<TSNode> getVariableInstanceNode(
      TSNode declarationNode, TSFile file, String className) {
    if (declarationNode == null
        || !"local_variable_declaration".equals(declarationNode.getType())) {
      return Optional.empty();
    }
    TSNode variableDeclaratorNode = declarationNode.getChildByFieldName("declarator");
    if (variableDeclaratorNode == null) {
      return Optional.empty();
    }
    TSNode objectCreationNode = variableDeclaratorNode.getChildByFieldName("value");
    if (objectCreationNode == null
        || !"object_creation_expression".equals(objectCreationNode.getType())) {
      return Optional.empty();
    }
    TSNode typeIdentifierNode = objectCreationNode.getChildByFieldName("type");
    if (typeIdentifierNode != null) {
      String foundClassName =
          file.getTextFromRange(typeIdentifierNode.getStartByte(), typeIdentifierNode.getEndByte());
      if (className.equals(foundClassName)) {
        return Optional.of(typeIdentifierNode);
      }
    }
    return Optional.empty();
  }
}
