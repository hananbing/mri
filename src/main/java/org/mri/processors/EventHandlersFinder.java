package org.mri.processors;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtMethodImpl;

public class EventHandlersFinder {

  private static final String AXON_EVENT_HANDLER = "org.axonframework.eventhandling.EventHandler";
  private static final String AXON_EVENT_SOURCING_HANDLER = "org.axonframework.eventsourcing.EventSourcingHandler";
  private static final String AXON_SAGA_HANDLER = "org.axonframework.eventhandling.saga.SagaEventHandler";

  private Map<CtTypeReference, List<CtMethodImpl>> methodMap = new HashMap<>();

  private class Processor extends AbstractProcessor<CtMethodImpl> {

    @Override
    public void process(CtMethodImpl method) {
      Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
        method.getAnnotations(),
        signatureContainsOneOf(
          Arrays.asList(
            AXON_EVENT_HANDLER,
            AXON_EVENT_SOURCING_HANDLER,
            AXON_SAGA_HANDLER)));
      if (annotation.isPresent()) {
        CtTypeReference parameterType = ((CtParameter) method.getParameters().get(0)).getType();
        List<CtMethodImpl> methods = methodMap.get(parameterType);
        if (methods == null) {
          methods = new ArrayList<>();
          methodMap.put(parameterType, methods);
        }
        methods.add(method);
      }
    }
  }

  private Predicate<CtAnnotation> signatureContainsOneOf(final List<String> list) {
    return annotation -> list.contains(annotation.getType().getQualifiedName());
  }

  public Map<CtTypeReference, List<CtMethodImpl>> all(Collection<? extends CtElement> elements, QueueProcessingManager queueProcessingManager) {
    queueProcessingManager.addProcessor(new Processor());
    queueProcessingManager.process(elements);
    return methodMap;
  }
}
