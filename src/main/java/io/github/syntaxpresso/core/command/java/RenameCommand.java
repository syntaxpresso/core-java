package io.github.syntaxpresso.core.command.java;

import io.github.syntaxpresso.core.command.java.dto.RenameResponse;
import io.github.syntaxpresso.core.common.DataTransferObject;
import io.github.syntaxpresso.core.service.JavaService;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.treesitter.TSNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@RequiredArgsConstructor
@Command(name = "rename", description = "Rename a symbol and all its usages.")
public class RenameCommand implements Callable<Void> {

  private final JavaService javaService;

  @Option(names = "--cwd", description = "Current Working Directory", required = true)
  private File cwd;

  @Option(names = "--file-path", description = "The path to the file", required = true)
  private File filePath;

  @Option(names = "--line", description = "The line number of the symbol", required = true)
  private int line;

  @Option(names = "--column", description = "The column number of the symbol", required = true)
  private int column;

  @Option(names = "--new-name", description = "The new name for the symbol", required = true)
  private String newName;

  @Override
  public Void call() {
    Optional<TSNode> declarationNodeOpt = javaService.findDeclarationNode(filePath, line, column);
    if (declarationNodeOpt.isEmpty()) {
      System.out.println(
          DataTransferObject.error("Could not find the declaration for the symbol."));
      return null;
    }
    TSNode declarationNode = declarationNodeOpt.get();
    List<TSNode> usages = javaService.findUsages(filePath, declarationNode, cwd);
    if (usages.isEmpty()) {
      System.out.println(DataTransferObject.error("Could not find any usages for the symbol."));
      return null;
    }
    Collections.reverse(usages);
    for (TSNode usage : usages) {
      javaService.getTsHelper().renameNode(filePath, usage, newName);
    }
    RenameResponse response =
        RenameResponse.builder()
            .filePath(filePath.getAbsolutePath())
            .renamedNodes(usages.size())
            .newName(newName)
            .build();
    System.out.println(DataTransferObject.success(response));
    return null;
  }
}
