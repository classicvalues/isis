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
package org.apache.isis.subdomains.xdocreport.applib.service.example.models;

import java.util.List;
import java.util.Map;

import org.apache.isis.commons.internal.collections._Maps;
import org.apache.isis.subdomains.xdocreport.applib.XDocReportService.XDocReportModel;

import lombok.Data;

@Data
public class ProjectDevelopersModel implements XDocReportModel {

    private final Project project;
    private final List<Developer> developers;

    @Override
    public Map<String, Data> getContextData() {
        return _Maps.unmodifiable(
                "project", Data.object(project),
                "developers", Data.list(developers, Developer.class));
    }

}
