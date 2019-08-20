package com.elastic.support;

import com.elastic.support.config.BaseConfig;
import com.elastic.support.config.ElasticClientInputs;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClientBuilder;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;

public class ElasticClientService extends BaseService {


    protected RestClient createGenericClient(BaseConfig config, ElasticClientInputs inputs) {
        RestClientBuilder builder = new RestClientBuilder();

        return builder
                .setConnectTimeout(config.getRestConfig().get("connectTimeout") * 1000)
                .setRequestTimeout(config.getRestConfig().get("requestTimeout") * 1000)
                .setSocketTimeout(config.getRestConfig().get("socketTimeout") * 1000)
                .setProxyHost(inputs.getProxyUser())
                .setProxPort(inputs.getProxyPort())
                .setProxyUser(inputs.getUser())
                .setProxyPass(inputs.getProxyPassword())
                .build();

    }

    protected RestClient createEsRestClient(BaseConfig config, ElasticClientInputs inputs) {
        RestClientBuilder builder = new RestClientBuilder();
        builder
                .setConnectTimeout(config.getRestConfig().get("connectTimeout") * 1000)
                .setRequestTimeout(config.getRestConfig().get("requestTimeout") * 1000)
                .setSocketTimeout(config.getRestConfig().get("socketTimeout") * 1000)
                .setProxyHost(inputs.getProxyUser())
                .setProxPort(inputs.getProxyPort())
                .setProxyUser(inputs.getUser())
                .setProxyPass(inputs.getProxyPassword())
                .setBypassVerify(inputs.isSkipVerification())
                .setHost(inputs.getHost())
                .setPort(inputs.getPort())
                .setScheme(inputs.getScheme());

        if (inputs.isSecured()) {
            builder.setUser(inputs.getUser())
                    .setPassword(inputs.getPassword());
        }

        if (inputs.isPki()) {
            builder.setPkiKeystore(inputs.getPkiKeystore())
                    .setPkiKeystorePass(inputs.getPkiKeystorePass());
        }

        return builder.build();
    }

    public void checkAuthLevel(String user, boolean isAuth){

        if(StringUtils.isNotEmpty(user) && !isAuth){

            String border = SystemUtils.buildStringFromChar(60, '*');
            logger.warn(SystemProperties.lineSeparator);
            logger.warn(border);
            logger.warn(border);
            logger.warn(border);
            logger.warn("The elasticsearch user entered: {} does not appear to have sufficient authorization to access all collected information", user);
            logger.warn("Some of the calls may not have completed successfully.");
            logger.warn("If you are using a custom role please verify that it has the admin role for versions prior to 5.x or the superuser role for subsequent versions.");
            logger.warn(border);
            logger.warn(border);
            logger.warn(border);

        }

    }


}
