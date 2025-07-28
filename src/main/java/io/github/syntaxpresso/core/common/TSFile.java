package io.github.syntaxpresso.core.common;

import io.github.syntaxpresso.core.common.extra.SupportedLanguage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.treesitter.TSParser;
import org.treesitter.TSTree;

public class TSFile {
  private final TSParser parser;
  private File file;
  private TSTree tree;
  private String sourceCode;

  /**
   * Creates a TSFile instance from a given programming language and source code string.
   *
   * @param supportedLanguage The language of the source code.
   * @param sourceCode The source code content.
   */
  public TSFile(SupportedLanguage supportedLanguage, String sourceCode) {
    this.parser = ParserFactory.get(supportedLanguage);
    this.setData(sourceCode);
  }

  /**
   * Creates a TSFile instance from a given programming language and a file.
   *
   * @param supportedLanguage The language of the file.
   * @param file The file to parse.
   * @throws IOException If the file cannot be read.
   */
  public TSFile(SupportedLanguage supportedLanguage, File file) throws IOException {
    this.parser = ParserFactory.get(supportedLanguage);
    this.file = file;
    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    this.setData(content);
  }

  /**
   * Internal method to parse source code and set the tree and sourceCode fields.
   *
   * @param sourceCode The source code to parse.
   */
  private void setData(String sourceCode) {
    if (this.parser == null) {
      throw new IllegalStateException("Parser is not initialized.");
    }
    this.tree = this.parser.parseString(null, sourceCode);
    if (this.tree.getRootNode().hasError()) {
      // Throw an exception if parsing results in an error.
      throw new IllegalArgumentException("Unable to parse the source code due to syntax errors.");
    }
    this.sourceCode = sourceCode;
  }

  /**
   * Updates the source code and re-parses the content.
   *
   * @param newSourceCode The new source code.
   */
  public void updateSourceCode(String newSourceCode) {
    this.setData(newSourceCode);
  }

  /**
   * Updates a specific range of the source code and re-parses the content.
   *
   * @param start The starting index of the text to replace.
   * @param end The ending index of the text to replace.
   * @param newText The new text to insert.
   */
  public void updateSourceCode(int start, int end, String newText) {
    if (this.sourceCode == null) {
      throw new IllegalStateException("Source code has not been initialized.");
    }
    String newContent = new StringBuilder(this.sourceCode).replace(start, end, newText).toString();
    this.setData(newContent);
  }

  /**
   * Saves the current source code to the original file path.
   *
   * @throws IOException If the file cannot be written.
   * @throws IllegalStateException If the original file path is not known.
   */
  public void save() throws IOException {
    if (this.file == null) {
      throw new IllegalStateException("File path is not set. Use saveAs(path) instead.");
    }
    Files.writeString(this.file.toPath(), this.sourceCode, StandardCharsets.UTF_8);
  }

  /**
   * Saves the current source code to a new file path.
   *
   * @param path The path to save the file to.
   * @throws IOException If the file cannot be written.
   */
  public void saveAs(Path path) throws IOException {
    Files.writeString(path, this.sourceCode, StandardCharsets.UTF_8);
    this.file = path.toFile();
  }

  /**
   * Moves the file to a new destination.
   *
   * @param destination The destination directory or full file path.
   * @return true if the move is successful.
   * @throws IOException If an I/O error occurs.
   * @throws IllegalStateException If the file has not been saved to disk yet.
   */
  public boolean move(File destination) throws IOException {
    if (this.file == null) {
      throw new IllegalStateException("Cannot move a file that has not been saved yet.");
    }
    Path targetPath = destination.toPath();
    // If the destination is a directory, keep the original file name.
    if (Files.isDirectory(targetPath)) {
      targetPath = targetPath.resolve(this.file.getName());
    }
    Files.move(this.file.toPath(), targetPath);
    this.file = targetPath.toFile();
    return true;
  }

  /**
   * Renames the file in its current directory.
   *
   * @param newName The new name for the file.
   * @return true if the rename is successful.
   * @throws IOException If an I/O error occurs.
   * @throws IllegalStateException If the file has not been saved to disk yet.
   */
  public boolean rename(String newName) throws IOException {
    if (this.file == null) {
      throw new IllegalStateException("Cannot rename a file that has not been saved yet.");
    }
    Path parentDir = this.file.toPath().getParent();
    if (parentDir == null) {
      throw new IOException("Cannot determine the parent directory of the file to rename.");
    }
    Path targetPath = parentDir.resolve(newName);
    Files.move(this.file.toPath(), targetPath);
    this.file = targetPath.toFile();
    return true;
  }

  /**
   * Returns the file associated with this object.
   *
   * @return The file.
   * @throws IllegalStateException if the file has not been set.
   */
  public File getFile() {
    if (file == null) {
      throw new IllegalStateException("File is not set.");
    }
    return file;
  }

  /**
   * Returns the syntax tree of the source code.
   *
   * @return The TSTree object.
   * @throws IllegalStateException if the tree has not been generated.
   */
  public TSTree getTree() {
    if (tree == null) {
      throw new IllegalStateException("Tree is not set.");
    }
    return tree;
  }

  /**
   * Returns the source code as a string.
   *
   * @return The source code.
   * @throws IllegalStateException if the source code has not been set.
   */
  public String getSourceCode() {
    if (sourceCode == null) {
      throw new IllegalStateException("Source code is not set.");
    }
    return sourceCode;
  }

  public TSParser getParser() {
    return parser;
  }
}
