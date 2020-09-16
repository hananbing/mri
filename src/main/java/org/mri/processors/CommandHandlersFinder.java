package org.mri.processors;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtExecutableImpl;

public class CommandHandlersFinder {

  private static final String AXON_COMMAND_HANDLER = "org.axonframework.commandhandling.CommandHandler";

  private Map<CtTypeReference, CtExecutableImpl> methods = new HashMap<>();

  private class Processor extends AbstractProcessor<CtExecutableImpl> {

    public Processor() {
      this.addProcessedElementType(CtExecutableImpl.class);
    }

    @Override
    public void process(CtExecutableImpl method) {
      Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
        method.getAnnotations(),
        signatureEqualTo(AXON_COMMAND_HANDLER));
      if (annotation.isPresent()) {
        methods.put(((CtParameter) method.getParameters().get(0)).getType(), method);
      }
    }
  }

  private Predicate<CtAnnotation> signatureEqualTo(final String value) {
    return annotation -> annotation.getType().getQualifiedName().equals(value);
  }

  public Map<CtTypeReference, CtExecutableImpl> all(Collection<? extends CtElement> elements, QueueProcessingManager queueProcessingManager) {
    queueProcessingManager.addProcessor(new Processor());
    queueProcessingManager.process(elements);
    return methods;
  }
}
