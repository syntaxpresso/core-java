package io.github.syntaxpresso.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.github.syntaxpresso.core.service.java.JavaCodeAnalizerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.treesitter.TSTree;

@DisplayName("TreeSitterUtils Integration Tests")
class JavaCodeAnalizerServiceTest {

  private final JavaCodeAnalizerService javaCodeAnalizerService = new JavaCodeAnalizerService();

  @Test
  @DisplayName("parse(Path) should return tree for a valid file")
  void parsePath_whenFileIsValid_shouldReturnTree() throws IOException {
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("test_valid", ".java");
      Files.writeString(tempFile, "public class Valid { }");
      Optional<TSTree> result = javaCodeAnalizerService.parse(tempFile);
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
      Optional<TSTree> result = javaCodeAnalizerService.parse(tempFile);
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
    Optional<TSTree> result = javaCodeAnalizerService.parse(sourceBytes);
    assertTrue(result.isPresent(), "Expected a TSTree for valid bytes");
  }

  @Test
  @DisplayName("parse(String) should return tree for a valid string")
  void parseString_whenStringIsValid_shouldReturnTree() {
    String sourceCode = "class Valid { }";
    Optional<TSTree> result = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(result.isPresent(), "Expected a TSTree for a valid string");
  }

  @Test
  @DisplayName("isSourceCodeValid should return true for valid source")
  void isSourceCodeValid_whenSourceIsValid_shouldReturnTrue() {
    String sourceCode = "class Valid { }";
    assertTrue(javaCodeAnalizerService.isSourceCodeValid(sourceCode), "Expected true for valid source");
  }

  @Test
  @DisplayName("isSourceCodeValid should return false for invalid source")
  void isSourceCodeValid_whenSourceIsInvalid_shouldReturnFalse() {
    String sourceCode = "class Invalid {";
    assertFalse(javaCodeAnalizerService.isSourceCodeValid(sourceCode), "Expected false for invalid source");
  }

  @Test
  @DisplayName("isMainClass should return true for a class with a main method")
  void isMainClass_whenClassIsMain_shouldReturnTrue() {
    String sourceCode =
        """
        public class Main {
          public static void main(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a class without a main method")
  void isMainClass_whenClassIsNotMain_shouldReturnFalse() {
    String sourceCode = "public class NotMain { }";
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return true for a main method with varargs")
  void isMainClass_whenMainMethodHasVarargs_shouldReturnTrue() {
    String sourceCode =
        """
        public class Main {
          public static void main(String... args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return true for a main method with different modifier order")
  void isMainClass_whenMainMethodHasDifferentModifierOrder_shouldReturnTrue() {
    String sourceCode =
        """
        class Main {
          static public void main(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a main method that is not static")
  void isMainClass_whenMainMethodIsNotStatic_shouldReturnFalse() {
    String sourceCode =
        """
        public class NotMain {
          public void main(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a main method that is not public")
  void isMainClass_whenMainMethodIsNotPublic_shouldReturnFalse() {
    String sourceCode =
        """
        public class NotMain {
          private static void main(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a method with wrong name")
  void isMainClass_whenMethodHasWrongName_shouldReturnFalse() {
    String sourceCode =
        """
        public class NotMain {
          public static void main1(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a method with wrong return type")
  void isMainClass_whenMethodHasWrongReturnType_shouldReturnFalse() {
    String sourceCode =
        """
        public class NotMain {
          public static int main(String[] args) {
            return 0;
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a method with wrong parameter type")
  void isMainClass_whenMethodHasWrongParameterType_shouldReturnFalse() {
    String sourceCode =
        """
        public class NotMain {
          public static void main(String args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return true for a main method in a final class")
  void isMainClass_whenMainIsInFinalClass_shouldReturnTrue() {
    String sourceCode =
        """
        public final class Main {
          public static void main(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a main method in an interface")
  void isMainClass_whenMainIsInInterface_shouldReturnFalse() {
    String sourceCode =
        """
        public interface NotMain {
          public static void main(String[] args) {
          }
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(javaCodeAnalizerService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("getPackageName should return package name when declared")
  void getPackageName_whenPackageIsDeclared_shouldReturnPackageName() {
    String sourceCode =
        """
        package com.example.myproject;

        class MyClass {
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    Optional<String> packageName = javaCodeAnalizerService.getPackageName(tree.get(), sourceCode);
    assertTrue(packageName.isPresent());
    assertEquals("com.example.myproject", packageName.get());
  }

  @Test
  @DisplayName("getPackageName should return empty for default package")
  void getPackageName_whenInDefaultPackage_shouldReturnEmpty() {
    String sourceCode =
        """
        class MyClass {
        }
        """;
    Optional<TSTree> tree = javaCodeAnalizerService.parse(sourceCode);
    assertTrue(tree.isPresent());
    Optional<String> packageName = javaCodeAnalizerService.getPackageName(tree.get(), sourceCode);
    assertFalse(packageName.isPresent());
  }

  @Test
  @DisplayName("getSourceCode should return file content for a valid path")
  void getSourceCode_whenPathIsValid_shouldReturnContent() throws IOException {
    String expectedContent = "public class MyTestFile { }";
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("test_source", ".java");
      Files.writeString(tempFile, expectedContent);
      Optional<String> result = javaCodeAnalizerService.getSourceCode(tempFile);
      assertTrue(result.isPresent(), "Expected content for a valid file path");
      assertEquals(expectedContent, result.get());
    } finally {
      if (tempFile != null) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  @Test
  @DisplayName("getSourceCode should return empty for a non-existent path")
  void getSourceCode_whenPathIsInvalid_shouldReturnEmpty() {
    Path nonExistentFile = Path.of("non_existent_file_12345.java");
    Optional<String> result = javaCodeAnalizerService.getSourceCode(nonExistentFile);
    assertFalse(result.isPresent(), "Expected empty for a non-existent file path");
  }
}
