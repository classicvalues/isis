/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.client.kroviz.handler

import org.apache.isis.client.kroviz.core.event.LogEntry

/**
 * Delegates responses (logEntry.response) to handlers, acts as Facade.
 * @See: https://en.wikipedia.org/wiki/Chain-of-responsibility_pattern
 */
object ResponseHandler {
    private var delegate: BaseHandler

    //IMPROVE sequence of handlers should follow frequency of invocation in order to minimize the time taken by unneeded calls to 'canHandle()'
    private var _0 = RestfulHandler()
    private var _1 = MenuBarsHandler()
    private var _2 = ActionHandler()
    private var _3 = ServiceHandler()
    private var _4 = ResultListHandler()
    private var _4a = ResultObjectHandler()
    private var _4b = ResultValueHandler()
    private var _5 = TObjectHandler()
    private var _6 = LayoutHandler()
    private var _6a = LayoutXmlHandler()
    private var _7 = PropertyHandler()
    private var _7a = CollectionHandler()
    private var _8 = MemberHandler()
    private var _9 = HttpErrorHandler()
    private var _9a = Http401ErrorHandler()
    private var _10 = UserHandler()
    private var _11 = VersionHandler()
    private var _12 = DomainTypesHandler()
    private var _13 = DomainTypeHandler()
    private var _14 = DiagramHandler()
    private var _15 = IconHandler()
    private var last = DefaultHandler()

    init {
        delegate = _0
        _0.successor = _1
        _1.successor = _2
        _2.successor = _3
        _3.successor = _4
        _4.successor = _4a
        _4a.successor = _4b
        _4b.successor = _5
        _5.successor = _6
        _6.successor = _6a
        _6a.successor = _7
        _7.successor = _7a
        _7a.successor = _8
        _8.successor = _9
        _9.successor = _9a
        _9a.successor = _10
        _10.successor = _11
        _11.successor = _12
        _12.successor = _13
        _13.successor = _14
        _14.successor = _15
        _15.successor = last
    }

    fun handle(logEntry: LogEntry) {
        delegate.handle(logEntry)
    }

}
