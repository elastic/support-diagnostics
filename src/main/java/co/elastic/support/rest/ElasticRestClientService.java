/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.BaseService;
import co.elastic.support.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElasticRestClientService extends BaseService {

    private static final Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

     protected void checkAuthLevel(String user, boolean isAuth){

        if(StringUtils.isNotEmpty(user) && !isAuth){
            String border = SystemUtils.buildStringFromChar(60, '*');
            logger.info(Constants.CONSOLE, SystemProperties.lineSeparator);
            logger.info(Constants.CONSOLE, border);
            logger.info(Constants.CONSOLE, border);
            logger.info(Constants.CONSOLE, border);
            logger.info(Constants.CONSOLE, "The elasticsearch user entered: {} does not appear to have sufficient authorization to access all collected information", user);
            logger.info(Constants.CONSOLE, "Some of the calls may not have completed successfully.");
            logger.info(Constants.CONSOLE, "If you are using a custom role please verify that it has the admin role for versions prior to 5.x or the superuser role for subsequent versions.");
            logger.info(Constants.CONSOLE, border);
            logger.info(Constants.CONSOLE, border);
            logger.info(Constants.CONSOLE, border);
        }

    }
}
