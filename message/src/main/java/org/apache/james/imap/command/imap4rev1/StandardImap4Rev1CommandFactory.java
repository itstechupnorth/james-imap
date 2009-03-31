/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.imap.command.imap4rev1;

import org.apache.james.imap.api.Imap4Rev1CommandFactory;
import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;

public class StandardImap4Rev1CommandFactory implements Imap4Rev1CommandFactory {

    private final ImapCommand append = ImapCommand
            .authenticatedStateCommand(ImapConstants.APPEND_COMMAND_NAME);

    private final ImapCommand authenticate = ImapCommand
            .nonAuthenticatedStateCommand(ImapConstants.AUTHENTICATE_COMMAND_NAME);

    private final ImapCommand capability = ImapCommand
            .anyStateCommand(ImapConstants.CAPABILITY_COMMAND_NAME);

    private final ImapCommand check = ImapCommand
            .selectedStateCommand(ImapConstants.CHECK_COMMAND_NAME);

    private final ImapCommand close = ImapCommand
            .selectedStateCommand(ImapConstants.CLOSE_COMMAND_NAME);

    private final ImapCommand copy = ImapCommand
            .selectedStateCommand(ImapConstants.COPY_COMMAND_NAME);

    private final ImapCommand create = ImapCommand
            .authenticatedStateCommand(ImapConstants.CREATE_COMMAND_NAME);

    private final ImapCommand delete = ImapCommand
            .authenticatedStateCommand(ImapConstants.DELETE_COMMAND_NAME);

    private final ImapCommand examine = ImapCommand
            .authenticatedStateCommand(ImapConstants.EXAMINE_COMMAND_NAME);

    private final ImapCommand expunge = ImapCommand
            .selectedStateCommand(ImapConstants.EXPUNGE_COMMAND_NAME);

    private final ImapCommand fetch = ImapCommand
            .selectedStateCommand(ImapConstants.FETCH_COMMAND_NAME);

    private final ImapCommand list = ImapCommand
            .authenticatedStateCommand(ImapConstants.LIST_COMMAND_NAME);

    private final ImapCommand login = ImapCommand
            .nonAuthenticatedStateCommand(ImapConstants.LOGIN_COMMAND_NAME);

    private final ImapCommand logout = ImapCommand
            .anyStateCommand(ImapConstants.LOGOUT_COMMAND_NAME);

    private final ImapCommand lsub = ImapCommand
            .authenticatedStateCommand(ImapConstants.LSUB_COMMAND_NAME);

    private final ImapCommand noop = ImapCommand
            .anyStateCommand(ImapConstants.NOOP_COMMAND_NAME);

    private final ImapCommand rename = ImapCommand
            .authenticatedStateCommand(ImapConstants.RENAME_COMMAND_NAME);

    private final ImapCommand search = ImapCommand
            .selectedStateCommand(ImapConstants.SEARCH_COMMAND_NAME);

    private final ImapCommand select = ImapCommand
            .authenticatedStateCommand(ImapConstants.SELECT_COMMAND_NAME);

    private final ImapCommand status = ImapCommand
            .authenticatedStateCommand(ImapConstants.STATUS_COMMAND_NAME);

    private final ImapCommand store = ImapCommand
            .selectedStateCommand(ImapConstants.STORE_COMMAND_NAME);

    private final ImapCommand subscribe = ImapCommand
            .authenticatedStateCommand(ImapConstants.SUBSCRIBE_COMMAND_NAME);

    private final ImapCommand uid = ImapCommand
            .selectedStateCommand(ImapConstants.UID_COMMAND_NAME);

    private final ImapCommand unsubscribe = ImapCommand
            .authenticatedStateCommand(ImapConstants.UNSUBSCRIBE_COMMAND_NAME);

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getAppend()
     */
    public ImapCommand getAppend() {
        return append;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getAuthenticate()
     */
    public ImapCommand getAuthenticate() {
        return authenticate;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getCapability()
     */
    public ImapCommand getCapability() {
        return capability;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getCheck()
     */
    public ImapCommand getCheck() {
        return check;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getClose()
     */
    public ImapCommand getClose() {
        return close;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getCopy()
     */
    public ImapCommand getCopy() {
        return copy;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getCreate()
     */
    public ImapCommand getCreate() {
        return create;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getDelete()
     */
    public ImapCommand getDelete() {
        return delete;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getExamine()
     */
    public ImapCommand getExamine() {
        return examine;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getExpunge()
     */
    public ImapCommand getExpunge() {
        return expunge;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getFetch()
     */
    public ImapCommand getFetch() {
        return fetch;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getList()
     */
    public ImapCommand getList() {
        return list;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getLogin()
     */
    public ImapCommand getLogin() {
        return login;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getLogout()
     */
    public ImapCommand getLogout() {
        return logout;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getLsub()
     */
    public ImapCommand getLsub() {
        return lsub;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getNoop()
     */
    public ImapCommand getNoop() {
        return noop;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getRename()
     */
    public ImapCommand getRename() {
        return rename;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getSearch()
     */
    public ImapCommand getSearch() {
        return search;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getSelect()
     */
    public ImapCommand getSelect() {
        return select;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getStatus()
     */
    public ImapCommand getStatus() {
        return status;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getStore()
     */
    public ImapCommand getStore() {
        return store;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getSubscribe()
     */
    public ImapCommand getSubscribe() {
        return subscribe;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getUid()
     */
    public ImapCommand getUid() {
        return uid;
    }

    /**
     * @see org.apache.james.imap.api.Imap4Rev1CommandFactory#getUnsubscribe()
     */
    public ImapCommand getUnsubscribe() {
        return unsubscribe;
    }
}
