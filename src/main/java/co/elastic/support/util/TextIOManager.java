package co.elastic.support.util;
/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
import jline.console.ConsoleReader;
import org.apache.commons.lang3.StringUtils;
import org.beryx.textio.*;
import org.beryx.textio.jline.JLineTextTerminal;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class TextIOManager implements AutoCloseable {

    public TextIO textIO;

    public StringInputReader standardStringReader;
    public BooleanInputReader standardBooleanReader;
    public StringInputReader  standardPasswordReader;
    public StringInputReader standardFileReader;

    public TextIOManager() {
        textIO = TextIoFactory.getTextIO();
        TextTerminal terminal = textIO.getTextTerminal();

        if(terminal instanceof JLineTextTerminal){
            JLineTextTerminal jltt = (JLineTextTerminal)terminal;
            ConsoleReader reader = jltt.getReader();
            reader.setExpandEvents(false);
        }

        // Input Readers
        // Generic - change the read label only
        // Warning: Setting default values may leak into later prompts if not reset. Better to use a new Reader.
        standardStringReader = textIO.newStringInputReader()
                .withMinLength(0)
                .withInputTrimming(true);
        standardBooleanReader = textIO.newBooleanInputReader();
        standardPasswordReader = textIO.newStringInputReader()
                .withInputMasking(true)
                .withInputTrimming(true)
                .withMinLength(0);
        standardFileReader = textIO.newStringInputReader()
                .withInputTrimming(true)
                .withValueChecker((String val, String propname) -> validateFile(val));
        // End Input Readers
    }

    static public List<String> validateFile(String val) {
        if (StringUtils.isEmpty(val.trim())) {
            return null;
        }

        File file = new File(val);

        if (!file.exists()) {
            return Collections.singletonList(
                    String.format("Specified file [%s] could not be located.", file.getPath())
            );
        }

        return null;
    }

    @Override
    public void close() {
        textIO.dispose();
    }
}
