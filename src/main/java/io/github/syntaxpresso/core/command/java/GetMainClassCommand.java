package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.GetMainClassResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.common.TSFile;
import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import io.github.syntaxpresso.core.service.JavaService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "get-main-class", description = "Get Main class")
public class GetMainClassCommand implements Callable<Void> {
  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private Path cwd;

  @Override
  public Void call() throws Exception {
    GetMainClassResponse response = new GetMainClassResponse();
    List<TSFile> allFiles =
        this.javaService.getPathHelper().findFilesByExtention(this.cwd, SupportedLanguage.JAVA);
    for (TSFile file : allFiles) {
      boolean isMainClass = this.javaService.isMainClass(file);
      if (isMainClass) {
        Optional<String> packageName = this.javaService.getPackageName(file);
        if (packageName.isEmpty()) {
          System.out.println(DataTransferObject.error("Package name not found"));
          return null;
        }
        response.setFilePath(file.getFile().getAbsolutePath());
        response.setPackageName(packageName.get());
        System.out.println(DataTransferObject.success(response));
        return null;
      }
    }
    return null;
  }
}
