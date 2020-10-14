package com.elastic.support.scrub;


import com.elastic.support.BaseConfig;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ScrubConfig extends BaseConfig {

    private static final Logger logger = LogManager.getLogger((ScrubConfig.class));

    private Vector<String> remove = new Vector<String>();
    private Vector<ScrubTokenEntry> tokens = new  Vector<>();
    private Vector<String> autoScrub = new Vector<>();

    public ScrubConfig(Map configuration){

        super(configuration);

    }


}
