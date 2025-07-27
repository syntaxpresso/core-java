package io.github.syntaxpresso.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.service.extra.ScopeType;
import io.github.syntaxpresso.core.util.PathHelper;
import io.github.syntaxpresso.core.util.TSHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.treesitter.TSNode;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

@DisplayName("JavaService Tests")
class JavaServiceTest {

  private JavaService javaService;
  private TSHelper tsHelper;
  private PathHelper pathHelper;
  private String sourceCodeA;

  @BeforeEach
  void setUp() {
    pathHelper = new PathHelper();
    this.tsHelper = new TSHelper(new TreeSitterJava(), pathHelper);
    this.javaService = new JavaService(pathHelper, this.tsHelper);
  }

  @Nested
  @DisplayName("isMainClass()")
  class IsMainClassTests {

    @Test
    @DisplayName("should return true for a class with a main method")
    void isMainClass_whenClassIsMain_shouldReturnTrue() {
      String sourceCode =
          """
          public class Main {
            public static void main(String[] args) {
            }
          }
          """;
      Optional<TSTree> tree = tsHelper.parse(sourceCode);
      assertTrue(tree.isPresent());
      assertTrue(javaService.isMainClass(tree.get(), sourceCode));
    }

    @Test
    @DisplayName("should return false for a class without a main method")
    void isMainClass_whenClassIsNotMain_shouldReturnFalse() {
      String sourceCode = "public class NotMain { }";
      Optional<TSTree> tree = tsHelper.parse(sourceCode);
      assertTrue(tree.isPresent());
      assertFalse(javaService.isMainClass(tree.get(), sourceCode));
    }

    @Test
    @DisplayName("should return true for a main method with varargs")
    void isMainClass_whenMainMethodHasVarargs_shouldReturnTrue() {
      String sourceCode =
          """
          public class Main {
            public static void main(String... args) {
            }
          }
          """;
      Optional<TSTree> tree = tsHelper.parse(sourceCode);
      assertTrue(tree.isPresent());
      assertTrue(javaService.isMainClass(tree.get(), sourceCode));
    }
  }

  @Nested
  @DisplayName("getPackageName()")
  class GetPackageNameTests {

    @Test
    @DisplayName("should return package name when declared")
    void getPackageName_whenPackageIsDeclared_shouldReturnPackageName() {
      String sourceCode =
          """
          package com.example.myproject;

          class MyClass {
          }
          """;
      Optional<TSTree> tree = tsHelper.parse(sourceCode);
      assertTrue(tree.isPresent());
      Optional<String> packageName = javaService.getPackageName(tree.get(), sourceCode);
      assertTrue(packageName.isPresent());
      assertEquals("com.example.myproject", packageName.get());
    }

    @Test
    @DisplayName("should return empty for default package")
    void getPackageName_whenInDefaultPackage_shouldReturnEmpty() {
      String sourceCode =
          """
          class MyClass {
          }
          """;
      Optional<TSTree> tree = tsHelper.parse(sourceCode);
      assertTrue(tree.isPresent());
      Optional<String> packageName = javaService.getPackageName(tree.get(), sourceCode);
      assertFalse(packageName.isPresent());
    }
  }

  @Nested
  @DisplayName("findDeclarationNode()")
  class FindDeclarationNodeTests {
    private File tempFile;
    private String sourceCode;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
      tempFile = tempDir.resolve("MyClass.java").toFile();
      sourceCode =
          """
          package com.example;

          public class MyClass {
            private String myField = "test";

            public void myMethod(String param) {
              int myVar = 10;
              myVar++; // line 8
              System.out.println(param); // line 9
              System.out.println(myField); // line 10
            }
          }
          """;
      Files.writeString(tempFile.toPath(), sourceCode);
    }

    @Test
    @DisplayName("should find local variable declaration from its usage")
    void findDeclaration_forLocalVariable_shouldSucceed() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 8, 7);
      assertTrue(declarationNode.isPresent(), "Declaration for 'myVar' should be found");
      assertEquals("local_variable_declaration", declarationNode.get().getType());
      String declarationText =
          sourceCode.substring(
              declarationNode.get().getStartByte(), declarationNode.get().getEndByte());
      assertEquals("int myVar = 10;", declarationText.trim());
    }

    @Test
    @DisplayName("should find method parameter declaration from its usage")
    void findDeclaration_forMethodParameter_shouldSucceed() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 9, 26);
      assertTrue(declarationNode.isPresent(), "Declaration for 'param' should be found");
      assertEquals("formal_parameter", declarationNode.get().getType());
      String declarationText =
          sourceCode.substring(
              declarationNode.get().getStartByte(), declarationNode.get().getEndByte());
      assertEquals("String param", declarationText.trim());
    }

    @Test
    @DisplayName("should find class field declaration from its usage in a method")
    void findDeclaration_forClassField_shouldSucceed() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 10, 26);
      assertTrue(declarationNode.isPresent(), "Declaration for 'myField' should be found");
      assertEquals("field_declaration", declarationNode.get().getType());
      String declarationText =
          sourceCode.substring(
              declarationNode.get().getStartByte(), declarationNode.get().getEndByte());
      assertEquals("private String myField = \"test\";", declarationText.trim());
    }

    @Test
    @DisplayName("should find declaration when starting from the declaration itself")
    void findDeclaration_fromDeclaration_shouldSucceed() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 7, 11);
      assertTrue(
          declarationNode.isPresent(),
          "Should find the declaration when starting on the declaration itself");
      assertEquals("local_variable_declaration", declarationNode.get().getType());
    }

    @Test
    @DisplayName("should return empty for a non-existent file")
    void findDeclaration_forNonExistentFile_shouldReturnEmpty() {
      File nonExistentFile = new File("non_existent_file.java");
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(nonExistentFile, 1, 1);
      assertFalse(declarationNode.isPresent());
    }
  }

  @Nested
  @DisplayName("getNodeScope()")
  class GetNodeScopeTests {
    private File tempFile;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
      tempFile = tempDir.resolve("MyClass.java").toFile();
      String sourceCode =
          """
          package com.example;

          public class MyClass { // line 3
            private String myField; // line 4
            public int publicField; // line 5

            void myMethod(String param) { // line 7
              int myVar = 10; // line 8
            }

            public void myPublicMethod() {} // line 11
          }
          """;
      Files.writeString(tempFile.toPath(), sourceCode);
    }

    @Test
    @DisplayName("should identify LOCAL scope for a local variable")
    void getNodeScope_forLocalVariable_shouldReturnLocal() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 8, 11);
      assertTrue(declarationNode.isPresent());
      Optional<ScopeType> scope = javaService.getNodeScope(declarationNode.get());
      assertTrue(scope.isPresent());
      assertEquals(ScopeType.LOCAL, scope.get());
    }

    @Test
    @DisplayName("should identify LOCAL scope for a method parameter")
    void getNodeScope_forMethodParameter_shouldReturnLocal() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 7, 26);
      assertTrue(declarationNode.isPresent());
      Optional<ScopeType> scope = javaService.getNodeScope(declarationNode.get());
      assertTrue(scope.isPresent());
      assertEquals(ScopeType.LOCAL, scope.get());
    }

    @Test
    @DisplayName("should identify CLASS scope for a private field")
    void getNodeScope_forPrivateField_shouldReturnClass() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 4, 20);
      assertTrue(declarationNode.isPresent());
      Optional<ScopeType> scope = javaService.getNodeScope(declarationNode.get());
      assertTrue(scope.isPresent());
      assertEquals(ScopeType.CLASS, scope.get());
    }

    @Test
    @DisplayName("should identify PROJECT scope for a public field")
    void getNodeScope_forPublicField_shouldReturnProject() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 5, 22);
      assertTrue(declarationNode.isPresent());
      Optional<ScopeType> scope = javaService.getNodeScope(declarationNode.get());
      assertTrue(scope.isPresent());
      assertEquals(ScopeType.PROJECT, scope.get());
    }

    @Test
    @DisplayName("should identify PROJECT scope for a public class")
    void getNodeScope_forPublicClass_shouldReturnProject() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(tempFile, 3, 15);
      assertTrue(declarationNode.isPresent());
      Optional<ScopeType> scope = javaService.getNodeScope(declarationNode.get());
      assertTrue(scope.isPresent());
      assertEquals(ScopeType.PROJECT, scope.get());
    }
  }

  @Nested
  @DisplayName("findUsages()")
  class FindUsagesTests {
    private File projectDir;
    private File classAFile;
    private File classBFile;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
      projectDir = tempDir.toFile();
      classAFile = tempDir.resolve("ClassA.java").toFile();
      classBFile = tempDir.resolve("ClassB.java").toFile();
      sourceCodeA =
          """
          public class ClassA {           // line 1
            private int privateField = 1; // line 2
            public void publicMethod() {  // line 3
              int localVar = 0;         // line 4
              localVar++;                 // line 5
              privateField++;             // line 6
            }
          }
          """;
      String sourceB =
          """
          public class ClassB {
            public void anotherMethod() {
              ClassA a = new ClassA();
              a.publicMethod();
              a.publicMethod();
            }
          }
          """;
      Files.writeString(classAFile.toPath(), sourceCodeA);
      Files.writeString(classBFile.toPath(), sourceB);
    }

    @Test
    @DisplayName("should find usages of a local variable only within its method")
    void findUsages_forLocalVariable_shouldReturnLocalUsages() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(classAFile, 4, 11);
      assertTrue(declarationNode.isPresent());
      assertEquals("local_variable_declaration", declarationNode.get().getType());
      List<TSNode> usages = javaService.findUsages(classAFile, declarationNode.get(), projectDir);
      assertEquals(2, usages.size());
      assertEquals(
          "localVar",
          sourceCodeA.substring(usages.get(0).getStartByte(), usages.get(0).getEndByte()));
    }

    @Test
    @DisplayName("should find usages of a private field only within its class")
    void findUsages_forPrivateField_shouldReturnClassUsages() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(classAFile, 2, 19);
      assertTrue(declarationNode.isPresent());
      assertEquals("field_declaration", declarationNode.get().getType());
      List<TSNode> usages = javaService.findUsages(classAFile, declarationNode.get(), projectDir);
      assertEquals(2, usages.size());
    }

    @Test
    @DisplayName("should find usages of a public method across the project")
    void findUsages_forPublicMethod_shouldReturnProjectUsages() {
      Optional<TSNode> declarationNode = javaService.findDeclarationNode(classAFile, 3, 19);
      assertTrue(declarationNode.isPresent());
      assertEquals("method_declaration", declarationNode.get().getType());
      List<TSNode> usages = javaService.findUsages(classAFile, declarationNode.get(), projectDir);
      assertEquals(3, usages.size());
    }
  }
}
