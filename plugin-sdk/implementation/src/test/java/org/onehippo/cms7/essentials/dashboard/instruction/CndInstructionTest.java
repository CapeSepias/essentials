/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.instruction;

import javax.inject.Inject;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class CndInstructionTest extends BaseRepositoryTest {

    public static final String TEST_URI = "http://www.test.com";
    public static final String TEST_PREFIX = "test";

    @Inject
    private InstructionExecutor executor;
    @Inject
    private CndInstruction cndInstruction;

    @Test
    public void testProcess() throws Exception {

        final Session session = getSession();
        session.getRootNode().addNode(HippoNodeType.NAMESPACES_PATH);
        session.save();
        CndUtils.registerNamespace(getContext(), TEST_PREFIX, TEST_URI);
        assertTrue("CndUtils.registerNamespaceUri", true);
        CndUtils.createHippoNamespace(getContext(), TEST_PREFIX);
        assertTrue("CndUtils.createHippoNamespace", true);
        boolean exists = CndUtils.namespaceUriExists(getContext(), TEST_URI);
        assertTrue(exists);

        cndInstruction.setDocumentType("newsdocument");
        getContext().setProjectNamespacePrefix(TEST_PREFIX);
        final InstructionSet instructionSet = new PluginInstructionSet();
        instructionSet.addInstruction(cndInstruction);
        InstructionStatus status = executor.execute(instructionSet, getContext());
        assertTrue("Expected success but got: " + status, status == InstructionStatus.SUCCESS);
        // this should node throw exists exception
        status = executor.execute(instructionSet, getContext());
        assertTrue("Expected success but got: " + status, status == InstructionStatus.FAILED);
        // test prefix:
        final String testingPrefix = "testingprefix";
        cndInstruction.setNamespacePrefix(testingPrefix);
        assertEquals("testingprefix", cndInstruction.getNamespacePrefix());

        session.logout();
    }
}
