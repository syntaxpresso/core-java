package io.github.syntaxpresso.core.util;

import io.github.syntaxpresso.core.common.enums.SourceDirectoryType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PathUtils {
  private static final String SRC_MAIN_JAVA = "src/main/java";
  private static final String SRC_TEST_JAVA = "src/test/java";

  private Optional<Path> findMainSourceDirectory(Path rootDir) {
    Path srcMainJava = rootDir.resolve(SRC_MAIN_JAVA);
    if (Files.isDirectory(srcMainJava)) {
      return Optional.of(srcMainJava);
    }
    return Optional.empty();
  }

  private Optional<Path> findTestSourceDirectory(Path rootDir) {
    Path srcTestJava = rootDir.resolve(SRC_TEST_JAVA);
    if (Files.isDirectory(srcTestJava)) {
      return Optional.of(srcTestJava);
    }
    return Optional.empty();
  }

  public boolean isJavaProject(Path rootDir) {
    if (rootDir == null || !Files.isDirectory(rootDir)) {
      return false;
    }
    boolean isJavaProject =
        Files.exists(rootDir.resolve("build.gradle"))
            || Files.exists(rootDir.resolve("build.gradle.kts"))
            || Files.exists(rootDir.resolve("pom.xml"))
            || Files.isDirectory(rootDir.resolve("src/main/java"));
    return isJavaProject;
  }

  public Optional<Path> findAbsolutePackagePath(
      Path rootDir, String packageName, SourceDirectoryType sourceDirectoryType) {
    if (rootDir == null || packageName == null || sourceDirectoryType == null) {
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
            return Optional.of(fullPackagePath);
          } catch (IOException e) {
            return Optional.empty();
          }
        });
  }
}
