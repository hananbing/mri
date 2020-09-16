package org.mri;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;

import com.google.common.collect.Lists;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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

    printStream.println("@startuml " + LOWER_CAMEL.to(LOWER_HYPHEN, reference.getSimpleName()) + "-flow.png");
    List<String> participants = Lists.newArrayList();
    descendants(participants);
    participants.forEach(printStream::println);

    printStream.println();
    List<String> componentNames = Lists.newArrayList();
    buildPlantUMLComponent(componentNames);

    componentNames.forEach(printStream::println);
    printStream.println("@enduml");
  }

  private void descendants(List<String> all) {
    if (children.isEmpty() || children.size() == 0) {
      return;
    }
    for (AxonNode child : children) {
      String context = "participant \"" + prettyActorName(child.reference) + "\" as " + actorName(child.reference);
//      String context = "participant " + actorName(child.reference);
      if (!all.contains(context)) {
        all.add(context);
      }
      child.descendants(all);
    }
  }

  private void buildPlantUMLComponent(List<String> componentNames) {
    if (children.isEmpty() || children.size() == 0) {
      return;
    }
    for (AxonNode child : children) {
      String context = actorName(this.reference())
        + " "
        + transition()
        + " "
        + actorName(child.reference())
        + ": "
        + methodName(child);
      if (!componentNames.contains(context)) {
        componentNames.add(context);
      }
      child.buildPlantUMLComponent(componentNames);
    }
  }

  private String className(AxonNode node) {
    return node.reference.getDeclaringType().getSimpleName();
  }

  private String prettyActorName(CtExecutableReference reference) {
    return reference.getDeclaringType().getPackage().getSimpleName() + "\\n" + "**" + reference.getDeclaringType().getSimpleName() + "**";
  }

  private String actorName(CtExecutableReference reference) {
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
