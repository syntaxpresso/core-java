package io.github.syntaxpresso.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.util.PathHelper;
import io.github.syntaxpresso.core.util.TSHelper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

@DisplayName("JavaService Tests")
class JavaServiceTest {

  private JavaService javaService;
  private TSHelper tsHelper;

  @BeforeEach
  void setUp() {
    PathHelper pathHelper = new PathHelper();
    this.tsHelper = new TSHelper(new TreeSitterJava(), pathHelper);
    this.javaService = new JavaService(pathHelper, this.tsHelper);
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(this.javaService.isMainClass(tree.get(), sourceCode));
  }

  @Test
  @DisplayName("isMainClass should return false for a class without a main method")
  void isMainClass_whenClassIsNotMain_shouldReturnFalse() {
    String sourceCode = "public class NotMain { }";
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    assertTrue(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    // Note: Tree-sitter may parse this, but our logic should reject it as a main class container.
    assertTrue(tree.isPresent());
    assertFalse(this.javaService.isMainClass(tree.get(), sourceCode));
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    Optional<String> packageName = this.javaService.getPackageName(tree.get(), sourceCode);
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
    Optional<TSTree> tree = this.tsHelper.parse(sourceCode);
    assertTrue(tree.isPresent());
    Optional<String> packageName = this.javaService.getPackageName(tree.get(), sourceCode);
    assertFalse(packageName.isPresent());
  }
}
