/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.process.impl;

import org.jbpm.process.instance.impl.DefaultProcessInstanceManager;
import org.junit.jupiter.api.Test;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultProcessInstanceManagerTest {

    @Test
    public void testCreateProcessInstance() {
        DefaultProcessInstanceManager pim = mock(DefaultProcessInstanceManager.class);
        pim.lock(false);
        KogitoProcessInstance kpi = mock(KogitoProcessInstance.class);
        when(pim.getProcessInstance(any())).thenReturn(kpi);
        verify(pim, never()).addProcessInstance(any());
        verify(pim, never()).removeProcessInstance(kpi);
        pim.lock(true);
        kpi = mock(KogitoProcessInstance.class);
        when(pim.getProcessInstance(any())).thenReturn(kpi);
        verify(pim, never()).addProcessInstance(any());
        verify(pim, never()).removeProcessInstance(kpi);
    }

}
