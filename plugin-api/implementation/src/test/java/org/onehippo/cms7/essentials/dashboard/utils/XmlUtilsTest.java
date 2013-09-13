package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id: XmlUtilsTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class XmlUtilsTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(XmlUtilsTest.class);

    @Test
    public void testFindingDocuments() throws Exception {

        final List<XmlNode> templateDocuments = XmlUtils.findTemplateDocuments(getProjectRoot(), getContext());
        assertEquals("expected " + NAMESPACES_TEST_SET.size() + " templates", NAMESPACES_TEST_SET.size(), templateDocuments.size());
    }

    @Test
    public void testConvertToString() throws Exception {
        final List<XmlNode> templateDocuments = XmlUtils.findTemplateDocuments(getProjectRoot(), getContext());

        final XmlNode xmlNode = findPluginNode(templateDocuments);
        final String xml = XmlUtils.xmlNodeToString(xmlNode);
        assertNotNull(xml);
        final Collection<XmlNode> prototypeNode = xmlNode.getTemplates();
        for (XmlNode node : prototypeNode) {

            log.info("node {}", node.getName());
        }
        final XmlProperty supertypeProperty = xmlNode.getSupertypeProperty();
        assertNotNull(supertypeProperty);
        final Collection<String> values = supertypeProperty.getValues();
        assertTrue(values.contains("hippoplugins:basedocument"));
        assertTrue(values.contains("hippostd:relaxed"));
        assertTrue(values.contains("hippotranslation:translated"));

    }

    //plugin-api/implementation/src/test/resources/project/content/namespaces/hippoplugins/plugin.xml
    private XmlNode findPluginNode(final Iterable<XmlNode> templateDocuments) {
        for (XmlNode templateDocument : templateDocuments) {
            if (templateDocument.getName().equals("plugin")) {
                return templateDocument;
            }
        }
        return null;
    }
}
