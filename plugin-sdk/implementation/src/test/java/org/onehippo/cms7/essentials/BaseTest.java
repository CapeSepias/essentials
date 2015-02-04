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

package org.onehippo.cms7.essentials;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.onehippo.cms7.essentials.dashboard.utils.inject.PropertiesModule;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id$"
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {ApplicationModule.class, PropertiesModule.class})
public abstract class BaseTest {


    @Inject
    protected AutowireCapableBeanFactory injector;

    public static final String HIPPOPLUGINS_NAMESPACE = "hippoplugins";
    public static final String PROJECT_NAMESPACE_TEST = "testnamespace";
    public static final String PROJECT_NAMESPACE_TEST_URI = "http://www.onehippo.org/tesnamespace/nt/1.0";
    public static final Set<String> NAMESPACES_TEST_SET = new ImmutableSet.Builder<String>()
            .add("hippoplugins:extendingnews")
            .add("myproject:newsdocument")
            .add("hippoplugins:extendedbase")
            .add("hippoplugins:textdocument")
            .add("hippoplugins:basedocument")
            .add("hippoplugins:plugin")
            .add("hippoplugins:vendor")
            .add("hippoplugins:newsdocument")
            .add("hippoplugins:version")
            .add("hippoplugins:dependency")
            .build();
    private String oldSystemDir;
    private PluginContext context;
    private Path projectRoot;


    public void setProjectRoot(final Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void setContext(final PluginContext context) {
        this.context = context;
    }

    @After
    public void tearDown() throws Exception {
        // reset system property
        if (oldSystemDir != null) {
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, oldSystemDir);
        }
        // delete project files:
        if (projectRoot != null) {
            FileUtils.deleteDirectory(projectRoot.toFile());
        }
    }

    @Before
    public void setUp() throws Exception {

        // create temp dir:
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final File root = new File(tmpDir);
        final File projectRootDir = new File(root.getAbsolutePath() + File.separator + "project");
        if (!projectRootDir.exists()) {
            projectRootDir.mkdir();
        }
        projectRoot = projectRootDir.toPath();
        context = getPluginContextFile();
    }

    /**
     * Plugin context with file system support
     *
     * @return PluginContext with file system initialized (so no JCR session)
     */
    protected PluginContext getPluginContextFile() {
        if (context == null) {


            final String basePath = projectRoot.toString();
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, basePath);
            final File file = new File(basePath);
            if (file.exists()) {
                final File cmsFolder = new File(basePath + File.separator + "cms");
                if (!cmsFolder.exists()) {
                    cmsFolder.mkdir();
                }
                final File siteFolder = new File(basePath + File.separator + "site");
                if (!siteFolder.exists()) {
                    siteFolder.mkdir();
                }
                final File essentialsFolder = new File(basePath + File.separator + "essentials");
                if (!essentialsFolder.exists()) {
                    essentialsFolder.mkdir();
                }
            }
            context = new TestPluginContext(null);
            context.setProjectNamespacePrefix(PROJECT_NAMESPACE_TEST);
            context.setBeansPackageName("org.onehippo.cms7.essentials.dashboard.test.beans");
            context.setComponentsPackageName("org.onehippo.cms7.essentials.dashboard.test.components");
            context.setRestPackageName("org.onehippo.cms7.essentials.dashboard.test.rest");
            context.setRestPackageName("org.onehippo.cms7.essentials.dashboard.test.rest");
            final ProjectSettingsBean projectSettings = new ProjectSettingsBean();
            context.setProjectSettings(projectSettings);

        }
        return context;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public PluginContext getContext() {

        if (context == null) {
            context = getPluginContextFile();
        }
        return context;
    }


}
