package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.java.JavaCodeAnalizerService;
import io.github.syntaxpresso.core.service.java.JavaProjectStructureService;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.treesitter.TSTree;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-main-class", description = "Get Main class")
public class GetMainClassCommand implements Callable<Void> {
  private final JavaCodeAnalizerService javaCodeAnalizerService = new JavaCodeAnalizerService();
  private final JavaProjectStructureService javaProjectStructureService =
      new JavaProjectStructureService();

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private File cwd;

  @Override
  public Void call() throws Exception {
    GetMainClassResponse response = new GetMainClassResponse();
    List<File> allFiles = this.javaProjectStructureService.findFilesByExtension(this.cwd, "java");
    for (File file : allFiles) {
      Optional<TSTree> tree = this.javaCodeAnalizerService.parse(file.toPath());
      if (tree.isEmpty()) {
        continue;
      }
      Optional<String> sourceCode = this.javaCodeAnalizerService.getSourceCode(file.toPath());
      if (sourceCode.isEmpty()) {
        continue;
      }
      boolean isMainClass = this.javaCodeAnalizerService.isMainClass(tree.get(), sourceCode.get());
      if (isMainClass) {
        Optional<String> packageName =
            this.javaCodeAnalizerService.getPackageName(tree.get(), sourceCode.get());
        if (packageName.isEmpty()) {
          System.out.println(DataTransferObject.error("Package name not found"));
          return null;
        }
        response.setFilePath(file.getAbsolutePath());
        response.setPackageName(packageName.get());
        System.out.println(DataTransferObject.success(response));
        return null;
      }
    }
    return null;
  }
}
