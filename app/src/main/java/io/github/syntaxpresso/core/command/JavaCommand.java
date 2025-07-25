package io.github.syntaxpresso.core.command;

import io.github.syntaxpresso.core.command.java.CreateNewJavaFileCommand;
import picocli.CommandLine.Command;

@Command(
    name = "java",
    description = "Parent command for all Java-specific operations.",
    subcommands = {CreateNewJavaFileCommand.class})
public class JavaCommand {}
