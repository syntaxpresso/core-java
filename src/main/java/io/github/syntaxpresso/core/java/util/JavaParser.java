package io.github.syntaxpresso.core.java.util;

import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TreeSitterJava;

/**
 * A singleton for TSParser. A utility class providing a thread-safe, reusable Tree-sitter parser
 * specifically configured for the Java language.
 *
 * <p>This class uses a {@link ThreadLocal} to ensure that each thread gets its own instance of
 * {@link TSParser}, avoiding concurrency issues while maintaining efficiency. The {@link
 * TSLanguage} object is thread-safe and is shared across all threads.
 */
final class JavaParser {

  // The Java language grammar. It's thread-safe and can be shared.
  private static final TSLanguage JAVA_LANGUAGE = new TreeSitterJava();

  // A ThreadLocal to hold a TSParser instance for each thread.
  private static final ThreadLocal<TSParser> PARSER =
      ThreadLocal.withInitial(
          () -> {
            TSParser parser = new TSParser();
            parser.setLanguage(JAVA_LANGUAGE);
            return parser;
          });

  /** Private constructor to prevent instantiation. */
  private JavaParser() {
    // This class is not meant to be instantiated.
  }

  /**
   * Retrieves the parser instance for the current thread, pre-configured for the Java language.
   *
   * @return A {@link TSParser} configured for Java.
   */
  public static TSParser get() {
    return PARSER.get();
  }
}
