package org.mri;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtExecutableImpl;
import spoon.support.reflect.declaration.CtMethodImpl;

public class AxonFlowBuilder {

  private final EventHandlerIdentificationStrategy eventHandlerIdentificationStrategy;
  private final Map<CtTypeReference, CtExecutableImpl> commandHandlers;
  private final MethodCallHierarchyBuilder callHierarchyBuilder;
  private List<CtTypeReference> aggregates;

  public AxonFlowBuilder(Map<CtTypeReference, Set<CtTypeReference>> classHierarchy,
    Map<MethodWrapper, List<CtExecutableReference>> callList,
    Map<CtTypeReference, List<CtMethodImpl>> eventHandlers,
    Map<CtTypeReference, CtExecutableImpl> commandHandlers,
    List<CtTypeReference> aggregates, boolean matchEventsByName) {
    this.aggregates = aggregates;
    if (matchEventsByName) {
      this.eventHandlerIdentificationStrategy = new EventHandlerIdentificationByNameStrategy(eventHandlers);
    } else {
      this.eventHandlerIdentificationStrategy = new EventHandlerIdentificationBySignatureStrategy(eventHandlers);
    }
    this.commandHandlers = commandHandlers;
    this.callHierarchyBuilder = new MethodCallHierarchyBuilder(callList, classHierarchy);
  }

  List<AxonNode> buildFlow(ArrayList<CtExecutableReference> methodReferences) {
    List<AxonNode> nodes = new ArrayList<>();
    for (CtExecutableReference each : methodReferences) {
      AxonNode root = new AxonNode(AxonNode.Type.CONTROLLER, each);
      nodes.add(root);
      buildCommandFlow(root);
    }
    return nodes;
  }

  private void buildCommandFlow(AxonNode node) {
    MethodCall methodCall = this.callHierarchyBuilder.buildCalleesMethodHierarchy(node.reference());
    Iterable<MethodCall> allCommandConstructionCalls = Iterables.filter(methodCall.asList(), isCommandPredicate());
    for (MethodCall commandConstruction : allCommandConstructionCalls) {
      CtExecutableImpl commandHandler = commandHandlers.get(commandConstruction.reference().getDeclaringType());
      AxonNode commandConstructionNode = new AxonNode(AxonNode.Type.COMMAND, commandConstruction.reference());
      node.add(commandConstructionNode);
      AxonNode commandHandlerNode = new AxonNode(AxonNode.Type.COMMAND_HANDLER, commandHandler.getReference());
      commandConstructionNode.add(commandHandlerNode);
      buildAggregateFlow(commandHandlerNode);
    }
  }

  private Predicate<MethodCall> isCommandPredicate() {
    return input -> commandHandlers.keySet().contains(input.reference().getDeclaringType());
  }

  private void buildAggregateFlow(AxonNode node) {
    MethodCall methodCall = this.callHierarchyBuilder.buildCalleesMethodHierarchy(node.reference());

    Iterable<MethodCall> aggregateCallInstances =
      Iterables.filter(methodCall.asList(), isAggregatePredicate());
    for (MethodCall aggregateCall : aggregateCallInstances) {
      AxonNode aggregateNode = new AxonNode(AxonNode.Type.AGGREGATE, aggregateCall.reference());
      buildEventFlow(aggregateNode);
      if (aggregateNode.hasChildren()) {
        node.add(aggregateNode);
      }
    }
  }

  private Predicate<? super MethodCall> isAggregatePredicate() {
    return (Predicate<MethodCall>) input -> aggregates.contains(input.reference().getDeclaringType());
  }

  private AxonNode buildEventFlow(AxonNode node) {
    MethodCall methodCall = this.callHierarchyBuilder.buildCalleesMethodHierarchy(node.reference());

    Iterable<MethodCall> eventConstructionInstances =
      Iterables.filter(methodCall.asList(), eventHandlerIdentificationStrategy.isEventPredicate());
    for (MethodCall eventConstruction : eventConstructionInstances) {
      AxonNode eventNode = new AxonNode(AxonNode.Type.EVENT, eventConstruction.reference());
      node.add(eventNode);
      for (CtMethodImpl eventHandler : eventHandlerIdentificationStrategy.findEventHandlers(eventNode.reference().getDeclaringType())) {
        AxonNode eventHandlerNode = new AxonNode(AxonNode.Type.EVENT_LISTENER, eventHandler.getReference());
        eventNode.add(eventHandlerNode);
        buildCommandFlow(eventHandlerNode);
      }
    }
    return node;
  }
}
