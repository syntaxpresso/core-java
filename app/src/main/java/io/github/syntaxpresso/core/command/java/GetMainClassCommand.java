package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.JavaService;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSTree;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "get-main-class", description = "Get Main class")
public class GetMainClassCommand implements Callable<Void> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private File cwd;

  @Override
  public Void call() throws Exception {
    GetMainClassResponse response = new GetMainClassResponse();
    List<File> allFiles = this.javaService.getPathHelper().findFiles(this.cwd, "java");
    for (File file : allFiles) {
      Optional<TSTree> tree = this.javaService.getTsHelper().parse(file);
      if (tree.isEmpty()) {
        continue;
      }
      Optional<String> sourceCode = this.javaService.getPathHelper().getFileSourceCode(file);
      if (sourceCode.isEmpty()) {
        continue;
      }
      boolean isMainClass = this.javaService.isMainClass(tree.get(), sourceCode.get());
      if (isMainClass) {
        Optional<String> packageName =
            this.javaService.getPackageName(tree.get(), sourceCode.get());
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
