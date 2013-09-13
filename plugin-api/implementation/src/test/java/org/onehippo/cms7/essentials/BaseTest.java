/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id: BaseTest.java 173107 2013-08-07 15:19:38Z mmilicevic $"
 */
public class BaseTest {
    public static final int NONE_EXISTING_BEANS_SIZE = 2;
    public static final String HIPPOPLUGINS_NAMESPACE = "hippoplugins";
    public static final String PROJECT_NAMESPACE_TEST = "testnamespace";
    public static final Set<String> NAMESPACES_TEST_SET = new ImmutableSet.Builder<String>()
            .add("hippoplugins:extendingnews")
            .add("hippoplugins:extendedbase")
            .add("hippoplugins:textdocument")
            .add("hippoplugins:basedocument")
            .add("hippoplugins:plugin")
            .add("hippoplugins:vendor")
            .add("hippoplugins:newsdocument")
            .add("hippoplugins:version")
            .add("hippoplugins:dependency")
            .build();
    private static Logger log = LoggerFactory.getLogger(BaseTest.class);
    private String oldSystemDir;
    private PluginContext context;
    private Path projectRoot;

    @After
    public void tearDown() throws Exception {
        if (System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            oldSystemDir = System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
        }
    }

    @Before
    public void setUp() throws Exception {
        // reset system property
        if (oldSystemDir != null) {
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, oldSystemDir);
        }

        final URL resource = getClass().getResource("/project");
        final String path = resource.getPath();
        projectRoot = new File(path).toPath();
    }

    /**
     * Plugin context with file system support
     *
     * @return PluginContext with file system initialized (so no JCR session)
     */
    private PluginContext getPluginContextFile() {
        if (context == null) {
            final URL resource = getClass().getResource("/project");
            final String basePath = resource.getPath();
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, basePath);
            context = new TestPluginContext(null, null, null);
            context.setProjectNamespacePrefix(PROJECT_NAMESPACE_TEST);
            context.setBeansPackageName("org.onehippo.cms7.essentials.dashboard.test.beans");
            context.setComponentsPackageName("org.onehippo.cms7.essentials.dashboard.test.components");
        }
        return context;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public PluginContext getContext() {
        if (context == null) {
            return getPluginContextFile();
        }
        return context;
    }
}
