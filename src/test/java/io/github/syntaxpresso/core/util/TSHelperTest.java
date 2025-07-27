package io.github.syntaxpresso.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

@DisplayName("TSHelper Tests")
class TSHelperTest {

  private TSHelper tsHelper;

  @BeforeEach
  void setUp() {
    PathHelper pathHelper = new PathHelper();
    tsHelper = new TSHelper(new TreeSitterJava(), pathHelper);
  }

  @Test
  @DisplayName("parse(String) should return tree for a valid string")
  void parseString_whenStringIsValid_shouldReturnTree() {
    String sourceCode = "class Valid { }";
    Optional<TSTree> result = tsHelper.parse(sourceCode);
    assertTrue(result.isPresent(), "Expected a TSTree for a valid string");
    assertFalse(
        result.get().getRootNode().hasError(), "Tree for valid code should not have errors");
  }

  @Test
  @DisplayName("parse(String) should return empty for an invalid string")
  void parseString_whenStringIsInvalid_shouldReturnEmpty() {
    String sourceCode = "class Invalid {"; // Missing closing brace
    Optional<TSTree> result = tsHelper.parse(sourceCode);
    assertFalse(result.isPresent(), "Expected empty for an invalid string");
  }

  @Test
  @DisplayName("parse(File) should return tree for a valid file")
  void parseFile_whenFileIsValid_shouldReturnTree(@TempDir Path tempDir) throws IOException {
    Path tempFile = tempDir.resolve("Valid.java");
    Files.writeString(tempFile, "public class Valid { }");

    Optional<TSTree> result = tsHelper.parse(tempFile.toFile());

    assertTrue(result.isPresent(), "Expected a TSTree for a valid file");
    assertFalse(
        result.get().getRootNode().hasError(), "Tree for valid file should not have errors");
  }

  @Test
  @DisplayName("parse(File) should return empty for an invalid file")
  void parseFile_whenFileIsInvalid_shouldReturnEmpty(@TempDir Path tempDir) throws IOException {
    Path tempFile = tempDir.resolve("Invalid.java");
    Files.writeString(tempFile, "public class Invalid {"); // Syntax error

    Optional<TSTree> result = tsHelper.parse(tempFile.toFile());

    assertFalse(result.isPresent(), "Expected empty for an invalid file");
  }

  @Test
  @DisplayName("parse(File) should return empty for a non-existent file")
  void parseFile_whenFileDoesNotExist_shouldReturnEmpty() {
    File nonExistentFile = new File("non_existent_file_that_should_not_be_found.java");
    Optional<TSTree> result = tsHelper.parse(nonExistentFile);
    assertFalse(result.isPresent(), "Expected empty for a non-existent file");
  }

  @Test
  @DisplayName("isSourceCodeValid should return true for valid source")
  void isSourceCodeValid_whenSourceIsValid_shouldReturnTrue() {
    String sourceCode = "class Valid { }";
    assertTrue(tsHelper.isSourceCodeValid(sourceCode), "Expected true for valid source");
  }

  @Test
  @DisplayName("isSourceCodeValid should return false for invalid source")
  void isSourceCodeValid_whenSourceIsInvalid_shouldReturnFalse() {
    String sourceCode = "class Invalid {";
    assertFalse(tsHelper.isSourceCodeValid(sourceCode), "Expected false for invalid source");
  }
}
