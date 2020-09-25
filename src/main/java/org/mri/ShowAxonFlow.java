package org.mri;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.mri.processors.AggregatesFinder;
import org.mri.processors.ClassHierarchyBuilder;
import org.mri.processors.CommandHandlersFinder;
import org.mri.processors.EventHandlersFinder;
import org.mri.processors.MethodExecutionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.MavenLauncher.SOURCE_TYPE;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtExecutableImpl;
import spoon.support.reflect.declaration.CtMethodImpl;

public class ShowAxonFlow {

  private static Logger logger = LoggerFactory.getLogger(ShowAxonFlow.class);

  @Option(name = "-s", aliases = "--source-maven-project-folder", metaVar = "MAVEN_PROJECT",
    usage = "source folder(s) for the maven project",
    handler = StringArrayOptionHandler.class,
    required = true)
  private String mavenProject;

  @Option(name = "-m", aliases = "--method-name", metaVar = "METHOD_NAME",
    usage = "method name to print call hierarchy",
    required = true)
  private String methodName;

  private PrintStream printStream;

  public static void main(String[] args) throws Exception {
    ShowAxonFlow.parse(args).doMain();
  }

  private static ShowAxonFlow parse(String[] args) {
    ShowAxonFlow showAxonFlow = new ShowAxonFlow(System.out);
    CmdLineParser parser = new CmdLineParser(showAxonFlow, ParserProperties.defaults().withUsageWidth(120));
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.print("Usage: java -jar <CHP_JAR_PATH>" + parser.printExample(OptionHandlerFilter.REQUIRED));
      System.err.println();
      System.err.println();
      System.err.println("Options:");
      parser.printUsage(System.err);
      System.exit(1);
    }
    return showAxonFlow;
  }

  public ShowAxonFlow(PrintStream printStream) {
    this.printStream = printStream;
  }

  public void doMain() throws Exception {
    MavenLauncher launcher = new MavenLauncher(mavenProject, SOURCE_TYPE.ALL_SOURCE);
    launcher.buildModel();
    printCallHierarchy(launcher, printStream);
  }

  private void printCallHierarchy(Launcher launcher, PrintStream printStream) throws Exception {
    QueueProcessingManager queueProcessingManager = new QueueProcessingManager(launcher.getFactory());
    List<CtType<?>> ctTypeList = launcher.getFactory().Class().getAll();

    Map<CtTypeReference, Set<CtTypeReference>> classHierarchy =
      new ClassHierarchyBuilder().build(ctTypeList, queueProcessingManager);

    Map<MethodWrapper, List<CtExecutableReference>> callList =
      new MethodExecutionBuilder().build(ctTypeList, queueProcessingManager);

    final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers =
      new EventHandlersFinder().all(ctTypeList, queueProcessingManager);

    final Map<CtTypeReference, CtExecutableImpl> commandHandlers =
      new CommandHandlersFinder().all(ctTypeList, queueProcessingManager);
    List<CtTypeReference> aggregates = new AggregatesFinder().all(ctTypeList, queueProcessingManager);

    ArrayList<CtExecutableReference> methodReferences = MethodCallHierarchyBuilder.forMethodName(methodName, callList, classHierarchy);
    if (methodReferences.isEmpty()) {
      printStream.println("No method containing `" + methodName + "` found.");
    }
    AxonFlowBuilder axonFlowBuilder = new AxonFlowBuilder(classHierarchy, callList, eventHandlers, commandHandlers, aggregates, false);
    List<AxonNode> axonNodes = axonFlowBuilder.buildFlow(methodReferences);
    for (AxonNode axonNode : axonNodes) {
      axonNode.printPlantUML(printStream);
    }
  }

}
