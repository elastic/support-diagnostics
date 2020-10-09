package com.elastic.support.util;

import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClient;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.jline.JLineTextTerminal;


public class ResourceCache {

    public static final TextIO textIO = TextIoFactory.getTextIO();
    public static final TextTerminal terminal = textIO.getTextTerminal();
    private static final Logger logger = LogManager.getLogger(ResourceCache.class);
    public static RestClient restClient;
    public static SystemCommand systemCommand;

    static {
        if (terminal instanceof JLineTextTerminal) {
            JLineTextTerminal jltt = (JLineTextTerminal) terminal;
            ConsoleReader reader = jltt.getReader();
            reader.setExpandEvents(false);
        }
    }

    // Centralized method for cleaning up console, ssh and http clients
    public static void closeAll() {
        try {
            restClient.close();
        } catch (Exception e) {
            logger.error("Failed to close Http Client.");
        }
        try {
            systemCommand.close();
        } catch (Exception e) {
            logger.error("Failed to System command console.");
        }
        textIO.dispose();
    }
}
