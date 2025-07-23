package io.github.syntaxpresso.core.java.command.dtos;

import java.io.Serializable;
import java.util.Objects;

public final class CreateNewJavaFileResponse implements Serializable {
  private final String content;

  public CreateNewJavaFileResponse(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (CreateNewJavaFileResponse) obj;
    return Objects.equals(this.content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content);
  }
}
