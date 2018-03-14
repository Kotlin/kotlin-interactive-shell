package sparklin.zeppelin;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.util.InterpreterOutputStream;
import org.apache.zeppelin.scheduler.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sparklin.kshell.EvalResult;
import sparklin.kshell.KShell;
import sparklin.kshell.KotlinScriptDefinitionEx;
import sparklin.kshell.configuration.Configuration;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

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
        KotlinScriptDefinitionEx kotlinScriptDefinitionEx = new KotlinScriptDefinitionEx();
        repl = new KShell(configuration, Collections.<File>emptyList(), kotlinScriptDefinitionEx, false);
        repl.initEngine();
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
//            Job runningJob = getRunningJob(interpreterContext.getParagraphId());
//            runningJob.info()
//                    .put("CURRENT_THREAD", Thread.currentThread()); //to be able to terminate thread

            out.setInterpreterOutput(interpreterContext.out);
            EvalResult res = repl.compileAndEval(line, false);
            if (res.isSuccess()) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS);
            } else {
                return new InterpreterResult(InterpreterResult.Code.ERROR, res.getMessage());
            }
        } catch (Exception e) {
            String msg = e.toString() + "\n at " + e.getStackTrace()[0];
            log.error("Failed to run script: " + e);
            return new InterpreterResult(InterpreterResult.Code.ERROR, msg);
        }
    }

    @Override
    public void cancel(InterpreterContext interpreterContext) {
//        log.info("Canceling...");
//        Job runningJob = getRunningJob(interpreterContext.getParagraphId());
//        if (runningJob != null) {
//            Map<String, Object> info = runningJob.info();
//            Object object = info.get("CURRENT_THREAD");
//            if (object instanceof Thread) {
//                try {
//                    Thread t = (Thread) object;
////                    t.dumpStack();
//                    t.interrupt();
//                } catch (Throwable t) {
//                    log.error("Failed to cancel script: " + t, t);
//                }
//            }
//        }
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