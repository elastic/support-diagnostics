package com.elastic.support.diagnostics.timed;

import com.elastic.support.*;
import com.elastic.support.diagnostics.DiagnosticApp;
import com.elastic.support.diagnostics.DiagnosticService;
import com.elastic.support.diagnostics.ShowHelpException;
import com.elastic.support.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.*;

public class TimedExecutionDiagnosticApp extends DiagnosticApp {

    private static final Logger logger = LogManager.getLogger(TimedExecutionDiagnosticApp.class);

    public static int execCount = 1;

    public static void main(String[] args) {

        try {
            logger.warn(Constants.CONSOLE,"Retries have been disabled due to timed execution run.");
            Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            TimedExecutionDiagConfig config = new TimedExecutionDiagConfig(diagMap);
            TimedExecutionDiagnosticInputs inputs = new TimedExecutionDiagnosticInputs(config.delimiter);
            initInputs(args, inputs);
            elasticsearchConnection(inputs, config);
            githubConnection(config);
            DiagnosticService diag = new DiagnosticService(inputs, config);
            runTimedExecs(diag, config, inputs);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,"Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
            logger.error( e);
        } finally {
            ResourceUtils.closeAll();
        }
    }

    private static void runTimedExecs(DiagnosticService service, TimedExecutionDiagConfig config,TimedExecutionDiagnosticInputs inputs ) throws Exception{

        CountDownLatch lock = new CountDownLatch(inputs.executions);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            logger.error(Constants.CONSOLE,"Running timed execution {} of {}", execCount++, inputs.executions);
            runServiceSequence(inputs, config, service, inputs.diagType);
            logger.error(Constants.CONSOLE,"Execution completed.");
            lock.countDown();
        }, 1, inputs.interval, TimeUnit.SECONDS);

        lock.await();
        future.cancel(true);
        executor.shutdown();

    }

}

