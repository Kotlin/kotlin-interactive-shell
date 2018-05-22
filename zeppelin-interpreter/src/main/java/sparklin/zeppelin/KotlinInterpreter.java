package sparklin.zeppelin;

import com.intellij.openapi.util.Disposer;
import kotlin.Pair;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.util.InterpreterOutputStream;
import org.apache.zeppelin.scheduler.Job;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.utils.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sparklin.kshell.KShell;
import sparklin.kshell.configuration.Configuration;
import sparklin.kshell.wrappers.ResultWrapper;
import static sparklin.kshell.wrappers.ResultWrapper.Status.*;

import java.io.File;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.util.*;

public class KotlinInterpreter extends Interpreter {
    private static Logger log = LoggerFactory.getLogger(KotlinInterpreter.class);
    private KShell repl;
    private Configuration configuration;
    private InterpreterOutputStream out;

    public KotlinInterpreter(Properties props) {
        super(props);
        log.info("Creating Kotlin interpreter !!!");
    }

    @Override
    public void open() {
        log.info("Open interpreter");
        out = new InterpreterOutputStream(log);
        configuration = new InterpreterConfiguration(getProperty());
        System.setOut(new PrintStream(out));
        repl = createRepl(configuration);
        repl.initEngine();
    }

    private KShell createRepl(Configuration configuration) {
        MessageCollector messageCollector = new PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false);
        List<File> classpath = new ArrayList<File>();
        // add kotlin stdlib jar
        classpath.add(PathUtil.getResourcePathForClass(Pair.class));
        return new KShell(Disposer.newDisposable(),
                configuration,
                messageCollector,
                classpath,
                "zeppelin",
                new URLClassLoader(getClassloaderUrls(), getClass().getClassLoader()));
    }

    @Override
    public void close() {
        log.info("Close interpreter");
        repl.cleanUp();
    }

    private Job getRunningJob(String paragraphId) {
        Job foundJob = null;
        Collection<Job> jobsRunning = getScheduler().getJobsRunning();
        for (Job job : jobsRunning) {
            if (job.getId().equals(paragraphId)) {
                foundJob = job;
            }
        }
        return foundJob;
    }

    @Override
    public InterpreterResult interpret(String line, InterpreterContext interpreterContext) {
        try {
            log.info("Interpreting...");
            Job runningJob = getRunningJob(interpreterContext.getParagraphId());
            runningJob.info()
                    .put("CURRENT_THREAD", Thread.currentThread()); //to be able to terminate thread

            out.setInterpreterOutput(interpreterContext.out);
            ResultWrapper result = repl.compileAndEval(line);
            ResultWrapper.Status status = result.getStatus();
            if (status == ERROR || status == INCOMPLETE) {
                Exception e = result.getErrorCause();
                if (e != null) e.printStackTrace(new PrintStream(out));
                return new InterpreterResult(InterpreterResult.Code.ERROR, result.getMessageOrEmpty());
            } else {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, result.getMessageOrEmpty());
            }
        } catch (Exception e) {
            String msg = e.toString() + "\n at " + e.getStackTrace()[0];
            log.error("Failed to run script: " + e);
            return new InterpreterResult(InterpreterResult.Code.ERROR, msg);
        }
    }

    @Override
    public void cancel(InterpreterContext interpreterContext) {
        log.info("Canceling...");
        Job runningJob = getRunningJob(interpreterContext.getParagraphId());
        if (runningJob != null) {
            Map<String, Object> info = runningJob.info();
            Object object = info.get("CURRENT_THREAD");
            if (object instanceof Thread) {
                try {
                    Thread t = (Thread) object;
                    t.interrupt();
                } catch (Throwable t) {
                    log.error("Failed to cancel script: " + t, t);
                }
            }
        }
    }

    @Override
    public FormType getFormType() {
        return FormType.NATIVE;
    }

    @Override
    public int getProgress(InterpreterContext interpreterContext) {
        return 0;
    }
}