package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.util.TreeSitterUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.treesitter.TSTree;
import picocli.CommandLine.Command;

@Command(name = "test")
public class TestCommand implements Callable<Integer> {
  private final TreeSitterUtils treeSitterUtils = new TreeSitterUtils();

  @Override
  public Integer call() throws Exception {
    Path cwd =
        Paths.get(
            "/var/home/andreluis/Documents/projects/syntaxpresso/core-java/src/main/java/io/github/syntaxpresso/core/java/Main.java");
    Optional<TSTree> tree = this.treeSitterUtils.parse(cwd);
    if (tree.isPresent()) {
      Optional<String> sourceCode = this.treeSitterUtils.getSourceCode(cwd);
      if (sourceCode.isPresent()) {
        System.out.println(this.treeSitterUtils.getPackageName(tree.get(), sourceCode.get()));
      }
    }

    return 0;
  }
}
