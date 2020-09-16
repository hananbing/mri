package org.mri.processors;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtFieldImpl;

public class AggregatesFinder {

  private static final String AXON_AGGREGATE_IDENTIFIER_ANNOTATION =
    "org.axonframework.commandhandling.model.AggregateIdentifier";
  private List<CtTypeReference> aggregates = new ArrayList<>();

  private class Processor extends AbstractProcessor<CtFieldImpl> {

    @Override
    public void process(CtFieldImpl field) {
      Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
        field.getAnnotations(),
        signatureEqualTo(AXON_AGGREGATE_IDENTIFIER_ANNOTATION));
      if (annotation.isPresent()) {
        aggregates.add(field.getDeclaringType().getReference());
      }
    }

    private Predicate<CtAnnotation> signatureEqualTo(final String value) {
      return annotation -> annotation.getAnnotationType().getQualifiedName().equals(value);
    }

  }

  public List<CtTypeReference> all(Collection<? extends CtElement> elements, QueueProcessingManager queueProcessingManager) {
    queueProcessingManager.addProcessor(new Processor());
    queueProcessingManager.process(elements);
    return aggregates;
  }
}
