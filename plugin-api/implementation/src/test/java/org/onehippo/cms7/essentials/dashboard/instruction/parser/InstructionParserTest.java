/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction.parser;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.instruction.FileInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.dashboard.instruction.XmlInstruction;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class InstructionParserTest {

    private static Logger log = LoggerFactory.getLogger(InstructionParserTest.class);

    @Test
    public void testParseInstructions() throws Exception {
        final Instructions value = new PluginInstructions();
        final HashSet<InstructionSet> instructionSets = new HashSet<>();
        final PluginInstructionSet instructionSet = new PluginInstructionSet();
        addInstructions(instructionSet);

        instructionSets.add(instructionSet);
        value.setInstructionSets(instructionSets);
        final JAXBContext context = JAXBContext.newInstance(PluginInstructions.class);
        final Marshaller m = context.createMarshaller();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter writer = new StringWriter();
        m.marshal(value, writer);
        final String s = writer.toString();
        log.info("s {}", s);
        final Instructions instructions = InstructionParser.parseInstructions(s);
        assertTrue(instructions != null);
        final InstructionSet set = instructions.getInstructionSets().iterator().next();
        final Set<Instruction> mySet = set.getInstructions();
        // test ordering
        final Iterator<Instruction> iterator = mySet.iterator();
        for (int i = 0; i < mySet.size(); i++) {


            final Instruction next = iterator.next();
            final int isMod3 = i % 3;
            if (isMod3 == 0) {
                assertTrue(next.getMessage().equals(String.format("XML%d", i)));
            } else {
                assertTrue(next.getMessage().equals(String.format("FILE%d", i)));
            }

        }

    }

    private void addInstructions(final PluginInstructionSet pluginInstructionSet) {
        for (int i = 0; i < 100; i++) {
            final int isMod3 = i % 3;
            final Instruction instruction = isMod3 == 0 ? new XmlInstruction() : new FileInstruction();
            if (isMod3 == 0) {
                instruction.setMessage(String.format("XML%d", i));
            } else {
                instruction.setMessage(String.format("FILE%d", i));
            }
            pluginInstructionSet.addInstruction(instruction);
        }


    }
}
