package com.elastic.support.diagnostics.timed;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.DiagnosticService;
import com.elastic.support.diagnostics.ShowHelpException;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TimedExecutionDiagnosticApp {

    private static final Logger logger = LogManager.getLogger(TimedExecutionDiagnosticApp.class);

    public static int execCount = 1;

    public static void main(String[] args) {

        try {
            TimedExecutionDiagnosticInputs inputs = new TimedExecutionDiagnosticInputs();

            if (args.length == 0) {
                logger.info(Constants.CONSOLE, Constants.interactiveMsg);

                inputs.interactive = true;
                inputs.runInteractive();
            } else {
                List<String> errors = inputs.parseInputs(args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.error(Constants.CONSOLE, err);
                    }
                    inputs.usage();
                    SystemUtils.quitApp();
                }
            }

            inputs.bypassRetry = true;
            logger.warn(Constants.CONSOLE,"Retries have been disabled due to timed execution run.");
            Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            TimedExecutionDiagConfig config = new TimedExecutionDiagConfig(diagMap);
            DiagnosticService diag = new DiagnosticService();
            ResourceCache.terminal.dispose();
            runTimedExecs(diag, config, inputs);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,"Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
            logger.error( e);
        } finally {
            ResourceCache.closeAll();
        }
    }

    public static void runTimedExecs(DiagnosticService service, TimedExecutionDiagConfig config,TimedExecutionDiagnosticInputs inputs ) throws Exception{

        CountDownLatch lock = new CountDownLatch(inputs.executions);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            logger.error(Constants.CONSOLE,"Running timed execution {} of {}", execCount++, inputs.executions);
            service.exec(inputs, config);
            logger.error(Constants.CONSOLE,"Execution completed.");
            lock.countDown();
        }, 1, inputs.interval, TimeUnit.SECONDS);

        lock.await();
        future.cancel(true);
        executor.shutdown();

    }
}

