package io.github.syntaxpresso.core.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.syntaxpresso.core.java.common.enums.SourceDirectoryType;
import io.github.syntaxpresso.core.java.util.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Robust tests for the {@link PathUtils} class. Uses a temporary directory to ensure tests are
 * isolated and do not affect the project structure.
 */
class PathUtilsTest {

  private PathUtils pathUtils;

  // JUnit 5 will create a temporary directory for each test method
  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    pathUtils = new PathUtils();
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Tests for isJavaProject
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  @Test
  @DisplayName("isJavaProject should return true for a directory with build.gradle")
  void isJavaProject_shouldReturnTrue_forGradleProject() throws IOException {
    Files.createFile(tempDir.resolve("build.gradle"));
    assertTrue(pathUtils.isJavaProject(tempDir));
  }

  @Test
  @DisplayName("isJavaProject should return true for a directory with build.gradle.kts")
  void isJavaProject_shouldReturnTrue_forGradleKtsProject() throws IOException {
    Files.createFile(tempDir.resolve("build.gradle.kts"));
    assertTrue(pathUtils.isJavaProject(tempDir));
  }

  @Test
  @DisplayName("isJavaProject should return true for a directory with pom.xml")
  void isJavaProject_shouldReturnTrue_forMavenProject() throws IOException {
    Files.createFile(tempDir.resolve("pom.xml"));
    assertTrue(pathUtils.isJavaProject(tempDir));
  }

  @Test
  @DisplayName("isJavaProject should return true for a directory with src/main/java")
  void isJavaProject_shouldReturnTrue_forStandardDirectoryStructure() throws IOException {
    Files.createDirectories(tempDir.resolve("src/main/java"));
    assertTrue(pathUtils.isJavaProject(tempDir));
  }

  @Test
  @DisplayName("isJavaProject should return false for an empty directory")
  void isJavaProject_shouldReturnFalse_forEmptyDirectory() {
    assertFalse(pathUtils.isJavaProject(tempDir));
  }

  @Test
  @DisplayName("isJavaProject should return false for a non-existent directory")
  void isJavaProject_shouldReturnFalse_forNonExistentDirectory() {
    assertFalse(pathUtils.isJavaProject(tempDir.resolve("non-existent")));
  }

  @Test
  @DisplayName("isJavaProject should return false for a null path")
  void isJavaProject_shouldReturnFalse_forNullPath() {
    assertFalse(pathUtils.isJavaProject(null));
  }

  @Test
  @DisplayName("isJavaProject should return false for a directory with unrelated files")
  void isJavaProject_shouldReturnFalse_forUnrelatedFiles() throws IOException {
    Files.createFile(tempDir.resolve("README.md"));
    Files.createDirectory(tempDir.resolve("docs"));
    assertFalse(pathUtils.isJavaProject(tempDir));
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Tests for findAbsolutePackagePath
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  @Test
  @DisplayName("findAbsolutePackagePath should create and return the correct path for MAIN sources")
  void findAbsolutePackagePath_shouldCreateAndReturnPath_forMainSources() throws IOException {
    // Setup: Create the main source directory
    Path srcMainJava = tempDir.resolve("src/main/java");
    Files.createDirectories(srcMainJava);

    String packageName = "com.example.app";
    Optional<Path> result =
        pathUtils.findAbsolutePackagePath(tempDir, packageName, SourceDirectoryType.MAIN);

    assertTrue(result.isPresent(), "The resulting path should be present");
    Path expectedPath = srcMainJava.resolve("com/example/app");
    assertEquals(expectedPath, result.get());
    assertTrue(Files.isDirectory(expectedPath), "The package directory should have been created");
  }

  @Test
  @DisplayName("findAbsolutePackagePath should create and return the correct path for TEST sources")
  void findAbsolutePackagePath_shouldCreateAndReturnPath_forTestSources() throws IOException {
    // Setup: Create the test source directory
    Path srcTestJava = tempDir.resolve("src/test/java");
    Files.createDirectories(srcTestJava);

    String packageName = "com.example.app.tests";
    Optional<Path> result =
        pathUtils.findAbsolutePackagePath(tempDir, packageName, SourceDirectoryType.TEST);

    assertTrue(result.isPresent());
    Path expectedPath = srcTestJava.resolve("com/example/app/tests");
    assertEquals(expectedPath, result.get());
    assertTrue(Files.isDirectory(expectedPath));
  }

  @Test
  @DisplayName("findAbsolutePackagePath should return empty if source directory does not exist")
  void findAbsolutePackagePath_shouldReturnEmpty_whenSourceDirMissing() {
    // No src/main/java or src/test/java is created
    String packageName = "com.example.app";
    Optional<Path> mainResult =
        pathUtils.findAbsolutePackagePath(tempDir, packageName, SourceDirectoryType.MAIN);
    Optional<Path> testResult =
        pathUtils.findAbsolutePackagePath(tempDir, packageName, SourceDirectoryType.TEST);

    assertFalse(mainResult.isPresent(), "Should be empty when src/main/java is missing");
    assertFalse(testResult.isPresent(), "Should be empty when src/test/java is missing");
  }

  @Test
  @DisplayName("findAbsolutePackagePath should handle existing package directories gracefully")
  void findAbsolutePackagePath_shouldHandleExistingDirectories() throws IOException {
    Path packageDir = tempDir.resolve("src/main/java/com/example/app");
    Files.createDirectories(packageDir);

    String packageName = "com.example.app";
    Optional<Path> result =
        pathUtils.findAbsolutePackagePath(tempDir, packageName, SourceDirectoryType.MAIN);

    assertTrue(result.isPresent());
    assertEquals(packageDir, result.get());
  }

  @Test
  @DisplayName("findAbsolutePackagePath should return empty for null inputs")
  void findAbsolutePackagePath_shouldReturnEmpty_forNullInputs() {
    String packageName = "com.example.app";
    assertFalse(
        pathUtils.findAbsolutePackagePath(null, packageName, SourceDirectoryType.MAIN).isPresent());
    assertFalse(
        pathUtils.findAbsolutePackagePath(tempDir, null, SourceDirectoryType.MAIN).isPresent());
    assertFalse(pathUtils.findAbsolutePackagePath(tempDir, packageName, null).isPresent());
  }
}
