package io.github.syntaxpresso.core.java;

import io.github.syntaxpresso.core.java.command.CreateNewJavaFileCommand;
import picocli.CommandLine;

@CommandLine.Command(subcommands = {CreateNewJavaFileCommand.class})
public class Main {
  public static void main(String[] args) {
    System.exit(new CommandLine(new Main()).execute(args));
  }
}
