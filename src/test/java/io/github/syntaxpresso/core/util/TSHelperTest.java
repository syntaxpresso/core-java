package io.github.syntaxpresso.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

@DisplayName("TSHelper Tests")
class TSHelperTest {

  private TSHelper tsHelper;
  private PathHelper pathHelper;

  @BeforeEach
  void setUp() {
    pathHelper = new PathHelper();
    tsHelper = new TSHelper(new TreeSitterJava(), pathHelper);
  }

  @Nested
  @DisplayName("parse()")
  class ParseTests {
    @Test
    @DisplayName("should return tree for a valid string")
    void parseString_whenStringIsValid_shouldReturnTree() {
      String sourceCode = "class Valid { }";
      Optional<TSTree> result = tsHelper.parse(sourceCode);
      assertTrue(result.isPresent(), "Expected a TSTree for a valid string");
      assertFalse(
          result.get().getRootNode().hasError(), "Tree for valid code should not have errors");
    }

    @Test
    @DisplayName("should return empty for an invalid string")
    void parseString_whenStringIsInvalid_shouldReturnEmpty() {
      String sourceCode = "class Invalid {";
      Optional<TSTree> result = tsHelper.parse(sourceCode);
      assertFalse(result.isPresent(), "Expected empty for an invalid string");
    }

    @Test
    @DisplayName("should return tree for a valid file")
    void parseFile_whenFileIsValid_shouldReturnTree(@TempDir Path tempDir) throws IOException {
      Path tempFile = tempDir.resolve("Valid.java");
      Files.writeString(tempFile, "public class Valid { }");

      Optional<TSTree> result = tsHelper.parse(tempFile.toFile());

      assertTrue(result.isPresent(), "Expected a TSTree for a valid file");
      assertFalse(
          result.get().getRootNode().hasError(), "Tree for valid file should not have errors");
    }

    @Test
    @DisplayName("should return empty for an invalid file")
    void parseFile_whenFileIsInvalid_shouldReturnEmpty(@TempDir Path tempDir) throws IOException {
      Path tempFile = tempDir.resolve("Invalid.java");
      Files.writeString(tempFile, "public class Invalid {");

      Optional<TSTree> result = tsHelper.parse(tempFile.toFile());

      assertFalse(result.isPresent(), "Expected empty for an invalid file");
    }

    @Test
    @DisplayName("should return empty for a non-existent file")
    void parseFile_whenFileDoesNotExist_shouldReturnEmpty() {
      File nonExistentFile = new File("non_existent_file_that_should_not_be_found.java");
      Optional<TSTree> result = tsHelper.parse(nonExistentFile);
      assertFalse(result.isPresent(), "Expected empty for a non-existent file");
    }
  }

  @Nested
  @DisplayName("isSourceCodeValid()")
  class IsSourceCodeValidTests {
    @Test
    @DisplayName("should return true for valid source")
    void isSourceCodeValid_whenSourceIsValid_shouldReturnTrue() {
      String sourceCode = "class Valid { }";
      assertTrue(tsHelper.isSourceCodeValid(sourceCode), "Expected true for valid source");
    }

    @Test
    @DisplayName("should return false for invalid source")
    void isSourceCodeValid_whenSourceIsInvalid_shouldReturnFalse() {
      String sourceCode = "class Invalid {";
      assertFalse(tsHelper.isSourceCodeValid(sourceCode), "Expected false for invalid source");
    }
  }

  @Nested
  @DisplayName("getNodeAtPosition()")
  class GetNodeAtPositionTests {

    private final String sourceCode =
        "public class MyClass {\n"
            + "  public void myMethod(String param) {\n"
            + "    int myVar = 1;\n"
            + "  }\n"
            + "}";
    private TSTree tree;

    @BeforeEach
    void setup() {
      tree = tsHelper.parse(sourceCode).orElseThrow();
    }

    @Test
    @DisplayName("should find class identifier node")
    void getNodeAtPosition_forClassIdentifier_shouldReturnNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 1, 15);
      assertTrue(node.isPresent());
      assertEquals("identifier", node.get().getType());
      assertEquals(
          "MyClass", sourceCode.substring(node.get().getStartByte(), node.get().getEndByte()));
    }

    @Test
    @DisplayName("should find method identifier node")
    void getNodeAtPosition_forMethodIdentifier_shouldReturnNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 2, 15);
      assertTrue(node.isPresent());
      assertEquals("identifier", node.get().getType());
      assertEquals(
          "myMethod", sourceCode.substring(node.get().getStartByte(), node.get().getEndByte()));
    }

    @Test
    @DisplayName("should find parameter identifier node")
    void getNodeAtPosition_forParameterIdentifier_shouldReturnNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 2, 31);
      assertTrue(node.isPresent());
      assertEquals("identifier", node.get().getType());
      assertEquals(
          "param", sourceCode.substring(node.get().getStartByte(), node.get().getEndByte()));
    }

    @Test
    @DisplayName("should find variable identifier node")
    void getNodeAtPosition_forVariableIdentifier_shouldReturnNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 3, 9);
      assertTrue(node.isPresent());
      assertEquals("identifier", node.get().getType());
      assertEquals(
          "myVar", sourceCode.substring(node.get().getStartByte(), node.get().getEndByte()));
    }

    @Test
    @DisplayName("should find parent of keyword node")
    void getNodeAtPosition_forKeyword_shouldReturnParentNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 1, 3);
      assertTrue(node.isPresent());
      assertEquals("modifiers", node.get().getType());
    }

    @Test
    @DisplayName("should find type identifier node")
    void getNodeAtPosition_forTypeIdentifier_shouldReturnNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 2, 25);
      assertTrue(node.isPresent());
      assertEquals("type_identifier", node.get().getType());
    }

    @Test
    @DisplayName("should find parent node when position is on whitespace")
    void getNodeAtPosition_forWhitespace_shouldReturnParentNode() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(tree, 1, 8);
      assertTrue(node.isPresent());
      assertEquals("class_declaration", node.get().getType());
    }

    @Test
    @DisplayName("should return empty when tree is null")
    void getNodeAtPosition_whenTreeIsNull_shouldReturnEmpty() {
      Optional<TSNode> node = tsHelper.getNodeAtPosition(null, 1, 1);
      assertFalse(node.isPresent());
    }
  }
}
