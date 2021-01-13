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

package org.kie.kogito.persistence.transaction;

import java.util.concurrent.Callable;

public class BaseTransactionManager implements TransactionManager {

    private TransactionExecutor executor;

    @Override
    public void setExecutor(TransactionExecutor executor) {
        this.executor = executor;
    }

    @Override
    public <T> T execute(Callable<T> callable) throws Exception {
        return this.executor != null ? this.executor.execute(callable) : callable.call();
    }
}
