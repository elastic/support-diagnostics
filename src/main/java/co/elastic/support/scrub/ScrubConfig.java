/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.scrub;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Vector;

public class ScrubConfig {

    private static final Logger logger = LogManager.getLogger((ScrubConfig.class));

    private Vector<String> remove = new Vector<String>();
    private Vector<ScrubTokenEntry> tokens = new  Vector<>();
    private Vector<String> autoScrub = new Vector<>();

    public ScrubConfig(){





    }


}
