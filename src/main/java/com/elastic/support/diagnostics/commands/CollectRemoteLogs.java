package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectRemoteLogs extends BaseLogCollection  implements Command {

    /**
     * Collects logs on the host the diagnostic is run on. It will collect
     * the current log, plus the previous 3 rollovers. We got the Log directory
     * from the nodes output previously. If that call did not succeed this won't
     * be possible however.
     */
    private final Logger logger = LogManager.getLogger(CollectRemoteLogs.class);

    public void execute(DiagnosticContext context) {

    }

}
