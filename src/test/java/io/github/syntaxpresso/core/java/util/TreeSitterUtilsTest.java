package io.github.syntaxpresso.core.java.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.treesitter.TSTree;

@DisplayName("TreeSitterUtils Integration Tests")
class TreeSitterUtilsTest {

  private final TreeSitterUtils treeSitterUtils = new TreeSitterUtils();

  @Test
  @DisplayName("parse(Path) should return tree for a valid file")
  void parsePath_whenFileIsValid_shouldReturnTree() throws IOException {
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("test_valid", ".java");
      Files.writeString(tempFile, "public class Valid { }");
      Optional<TSTree> result = treeSitterUtils.parse(tempFile);
      assertTrue(result.isPresent(), "Expected a TSTree for a valid file path");
    } finally {
      if (tempFile != null) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  @Test
  @DisplayName("parse(Path) should return empty for an invalid file")
  void parsePath_whenFileIsInvalid_shouldReturnEmpty() throws IOException {
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("test_invalid", ".java");
      Files.writeString(tempFile, "public class Invalid {"); // Missing closing brace
      Optional<TSTree> result = treeSitterUtils.parse(tempFile);
      assertFalse(result.isPresent(), "Expected empty for an invalid file path");
    } finally {
      if (tempFile != null) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  @Test
  @DisplayName("parse(byte[]) should return tree for valid bytes")
  void parseBytes_whenBytesAreValid_shouldReturnTree() {
    byte[] sourceBytes = "class Valid { }".getBytes(StandardCharsets.UTF_8);
    Optional<TSTree> result = treeSitterUtils.parse(sourceBytes);
    assertTrue(result.isPresent(), "Expected a TSTree for valid bytes");
  }

  @Test
  @DisplayName("parse(String) should return tree for a valid string")
  void parseString_whenStringIsValid_shouldReturnTree() {
    String sourceCode = "class Valid { }";
    Optional<TSTree> result = treeSitterUtils.parse(sourceCode);
    assertTrue(result.isPresent(), "Expected a TSTree for a valid string");
  }

  @Test
  @DisplayName("isSourceCodeValid should return true for valid source")
  void isSourceCodeValid_whenSourceIsValid_shouldReturnTrue() {
    String sourceCode = "class Valid { }";
    assertTrue(treeSitterUtils.isSourceCodeValid(sourceCode), "Expected true for valid source");
  }

  @Test
  @DisplayName("isSourceCodeValid should return false for invalid source")
  void isSourceCodeValid_whenSourceIsInvalid_shouldReturnFalse() {
    String sourceCode = "class Invalid {";
    assertFalse(treeSitterUtils.isSourceCodeValid(sourceCode), "Expected false for invalid source");
  }
}
