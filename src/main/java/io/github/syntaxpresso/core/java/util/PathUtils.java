package io.github.syntaxpresso.core.java.util;

import io.github.syntaxpresso.core.java.common.enums.SourceDirectoryType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PathUtils {

  private static final Logger log = Logger.getLogger(PathUtils.class.getName());

  private static final String SRC_MAIN_JAVA = "src/main/java";
  private static final String SRC_TEST_JAVA = "src/test/java";

  private Optional<Path> findMainSourceDirectory(Path rootDir) {
    log.info("Finding main source directory in: " + rootDir);
    Path srcMainJava = rootDir.resolve(SRC_MAIN_JAVA);
    if (Files.isDirectory(srcMainJava)) {
      log.info("Main source directory found: " + srcMainJava);
      return Optional.of(srcMainJava);
    }
    log.info("Main source directory not found.");
    return Optional.empty();
  }

  private Optional<Path> findTestSourceDirectory(Path rootDir) {
    log.info("Finding test source directory in: " + rootDir);
    Path srcTestJava = rootDir.resolve(SRC_TEST_JAVA);
    if (Files.isDirectory(srcTestJava)) {
      log.info("Test source directory found: " + srcTestJava);
      return Optional.of(srcTestJava);
    }
    log.info("Test source directory not found.");
    return Optional.empty();
  }

  public boolean isJavaProject(Path rootDir) {
    log.info("Checking if " + rootDir + " is a Java project.");
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      log.info("Not a Java project: root directory is null or not a directory.");
      return false;
    }
    boolean isJavaProject =
        Files.exists(rootDir.resolve("build.gradle"))
            || Files.exists(rootDir.resolve("build.gradle.kts"))
            || Files.exists(rootDir.resolve("pom.xml"))
            || Files.isDirectory(rootDir.resolve("src/main/java"));
    log.info("Is Java project: " + isJavaProject);
    return isJavaProject;
  }

  public Optional<Path> findAbsolutePackagePath(
      Path rootDir, String packageName, SourceDirectoryType sourceDirectoryType) {
    log.info(
        "Finding absolute package path for package "
            + packageName
            + " in "
            + rootDir
            + " for source directory "
            + sourceDirectoryType);
    if (rootDir == null || packageName == null || sourceDirectoryType == null) {
      log.info("Cannot find absolute package path due to null parameters.");
      return Optional.empty();
    }
    String packageAsPath = packageName.replace('.', '/');
    Optional<Path> sourceDirOptional =
        (sourceDirectoryType == SourceDirectoryType.MAIN)
            ? findMainSourceDirectory(rootDir)
            : findTestSourceDirectory(rootDir);
    return sourceDirOptional.flatMap(
        sourceDir -> {
          Path fullPackagePath = sourceDir.resolve(packageAsPath);
          try {
            Files.createDirectories(fullPackagePath);
            log.info("Absolute package path found: " + fullPackagePath);
            return Optional.of(fullPackagePath);
          } catch (IOException e) {
            log.severe("Could not create directories for package: " + e.getMessage());
            return Optional.empty();
          }
        });
  }
}
