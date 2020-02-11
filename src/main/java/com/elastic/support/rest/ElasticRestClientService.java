package com.elastic.support.rest;

import com.elastic.support.BaseService;
import com.elastic.support.BaseConfig;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElasticRestClientService extends BaseService {

    private static final Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

     protected void checkAuthLevel(String user, boolean isAuth){

        if(StringUtils.isNotEmpty(user) && !isAuth){
            String border = SystemUtils.buildStringFromChar(60, '*');
            logger.info(SystemProperties.lineSeparator);
            logger.info(border);
            logger.info(border);
            logger.info(border);
            logger.info("The elasticsearch user entered: {} does not appear to have sufficient authorization to access all collected information", user);
            logger.info("Some of the calls may not have completed successfully.");
            logger.info("If you are using a custom role please verify that it has the admin role for versions prior to 5.x or the superuser role for subsequent versions.");
            logger.info(border);
            logger.info(border);
            logger.info(border);
        }

    }
}
