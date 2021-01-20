/*
* Copyright 2021 Red Hat, Inc. and/or its affiliates.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.kie.kogito.mongodb;

public class MongoDBVersionConcurrencyException  extends RuntimeException {
    
    private static final long serialVersionUID = -707257541887240381L;

    public MongoDBVersionConcurrencyException(Throwable cause) {
        super(cause);
    }

    public MongoDBVersionConcurrencyException(String msg) {
        super(msg);
    }
}
