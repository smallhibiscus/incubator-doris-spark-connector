// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.spark.container.instance;

import org.apache.doris.spark.exception.DorisRuntimeException;

import java.sql.Connection;

public interface ContainerService {
    void startContainer();

    default void restartContainer() {
        throw new DorisRuntimeException("Only doris docker container can implemented.");
    };

    boolean isRunning();

    Connection getQueryConnection();

    Connection getQueryConnection(String database);

    String getJdbcUrl();

    String getInstanceHost();

    Integer getMappedPort(int originalPort);

    String getUsername();

    String getPassword();

    String getFenodes();

    String getBenodes();

    void close();

    int getQueryPort();
}
