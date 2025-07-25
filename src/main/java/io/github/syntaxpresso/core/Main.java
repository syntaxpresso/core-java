package io.github.syntaxpresso.core;

import io.github.syntaxpresso.core.command.CreateNewJavaFileCommand;
import io.github.syntaxpresso.core.command.TestCommand;
import picocli.CommandLine;

@CommandLine.Command(subcommands = {CreateNewJavaFileCommand.class, TestCommand.class})
public class Main {
  public static void main(String[] args) {
    System.exit(new CommandLine(new Main()).execute(args));
  }
}
