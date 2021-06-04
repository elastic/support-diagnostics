/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.util;

import com.jcraft.jsch.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteUserInfo implements UserInfo {

    private static final Logger logger = LogManager.getLogger(RemoteUserInfo.class);

    public RemoteUserInfo( String name, String password, String passphrase){

        this.password = password;
        this.passphrase = passphrase;

    }

    private String password = null;
    public String getPassword() {
        return password;
    }

    private String passphrase = null;
    public String getPassphrase() {
        return passphrase;
    }

    public boolean promptPassphrase(String message) {
        return true;
    }

    public boolean promptPassword(String passwordPrompt) {
        return true;
    }

    public boolean promptYesNo(String message) {
        return true;
    }

    public void showMessage(String message) {
        logger.debug(message);
    }

}

