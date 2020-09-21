package org.mri;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;

import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import spoon.reflect.reference.CtExecutableReference;

public class AxonNode {

  enum Type {
    CONTROLLER, COMMAND, COMMAND_HANDLER, EVENT, EVENT_LISTENER, AGGREGATE;
  }

  private final Type type;
  private final CtExecutableReference reference;
  private List<AxonNode> children = new ArrayList<>();

  public AxonNode(Type type, CtExecutableReference reference) {
    this.type = type;
    this.reference = reference;
  }

  public void add(AxonNode node) {
    this.children.add(node);
  }

  public boolean hasChildren() {
    return !this.children.isEmpty();
  }

  public CtExecutableReference reference() {
    return reference;
  }

  public void print(PrintStream printStream) {
    if (children.isEmpty()) {
      return;
    }

    print(printStream, "");
  }

  private void print(PrintStream printStream, String indent) {
    printStream.println(indent + "<<" + type + ">>");
    printStream.println(indent + reference.toString());
    for (AxonNode node : children) {
      node.print(printStream, indent.concat("\t"));
    }
  }

  public void printDot(PrintStream printStream) {
    if (children.isEmpty()) {
      return;
    }

    printStream.println("digraph G {");
    printDot(printStream, children);
    printStream.println("}");
  }

  private void printDot(PrintStream printStream, List<AxonNode> children) {
    for (AxonNode child : children) {
      printStream.println(
        "\"" + className(this) + "#" + methodName(this) + "\""
          + " -> "
          + "\"" + className(child) + "#" + methodName(child) + "\"");
      child.printDot(printStream, child.children);
    }
  }

  public void printPlantUML(PrintStream printStream) {
    if (children.isEmpty()) {
      return;
    }

    printStream.println("@startuml " + LOWER_CAMEL.to(LOWER_HYPHEN, this.reference.getSimpleName()) + "-flow.png");
    Set<String> participants = Sets.newLinkedHashSet();
    descendants(participants);
    participants.forEach(printStream::println);

    printStream.println();
    Set<String> componentNames = Sets.newLinkedHashSet();
    buildPlantUMLComponent(componentNames);

    componentNames.forEach(printStream::println);
    printStream.println("@enduml");
  }

  private void descendants(Set<String> participants) {
    if (this.children.isEmpty() || this.children.size() == 0) {
      return;
    }
    for (AxonNode child : this.children) {
//      String context = "participant \"" + prettyActorName(child.reference) + "\" as " + getActorName(child.reference);
      String context = "participant " + getActorName(this.reference);
      participants.add(context);
      child.descendants(participants);
    }
  }

  private void buildPlantUMLComponent(Set<String> componentNames) {
    if (children.isEmpty() || children.size() == 0) {
      return;
    }
    for (AxonNode child : children) {
      String context = getActorName(this.reference())
        + " "
        + transition()
        + " "
        + getActorName(child.reference())
        + ": "
        + methodName(child);
      componentNames.add(context);
      child.buildPlantUMLComponent(componentNames);
    }
  }

  private String className(AxonNode node) {
    return node.reference.getDeclaringType().getSimpleName();
  }

  private String prettyActorName(CtExecutableReference reference) {
    return reference.getDeclaringType().getPackage().getSimpleName() + "\\n" + "**" + reference.getDeclaringType().getSimpleName() + "**";
  }

  private String getActorName(CtExecutableReference reference) {
//        return reference.getDeclaringType().getPackage().getSimpleName() + "." + reference.getDeclaringType().getSimpleName();
    return reference.getDeclaringType().getSimpleName();
  }

  private String transition() {
    switch (type) {
      case CONTROLLER:
      case COMMAND_HANDLER:
      case EVENT_LISTENER:
      case AGGREGATE:
        return "->";
      case COMMAND:
      case EVENT:
        return "-->";
    }
    return "->";
  }

  private String methodName(AxonNode child) {
    switch (type) {
      case CONTROLLER:
      case COMMAND_HANDLER:
      case EVENT_LISTENER:
        return "create";
      case COMMAND:
      case EVENT:
      case AGGREGATE:
        return child.reference.getSimpleName();
    }
    return "<<call>>";
  }

}
