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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ActionType;
import org.onehippo.cms7.essentials.dashboard.model.BeanWriterLogEntry;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryProperty;
import org.onehippo.cms7.essentials.dashboard.utils.xml.NodeOrProperty;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * @version "$Id$"
 */
public final class BeanWriterUtils {

    public static final ImmutableSet<String> BUILT_IN_DOCUMENT_TYPES = new ImmutableSet.Builder<String>()
            .add("autoexport").add("brokenlinks").add("editor")
            .add("frontend").add("hippo").add("hippobroadcast")
            .add("hippofacnav").add("hippogallery").add("hippogallerypicker")
            .add("hippohtmlcleaner").add("hippolog").add("hipporeport")
            .add("hipposched").add("hipposcxml").add("hippostd")
            .add("hippostdpubwf").add("hipposys").add("hipposysedit")
            .add("hippotranslation").add("hst").add("hstconfigedit").add("mix")
            .add("nt").add("properties").add("rep").add("reporting")
            .add("resourcebundle").add("selection")
            .build();


    public static final String CONTEXT_DATA_KEY = BeanWriterUtils.class.getName();

    private static Logger log = LoggerFactory.getLogger(BeanWriterUtils.class);


    private BeanWriterUtils() {
    }


    public static void populateBeanwriterMessages(final PluginContext context, final RestfulList<MessageRestful> messages) {
        final Multimap<String, Object> pluginContextData = context.getPluginContextData();
        final Collection<Object> objects = pluginContextData.get(CONTEXT_DATA_KEY);
        for (Object object : objects) {
            final BeanWriterLogEntry entry = (BeanWriterLogEntry) object;
            final ActionType actionType = entry.getActionType();
            if (actionType == ActionType.CREATED_METHOD || actionType == ActionType.MODIFIED_METHOD) {
                messages.add(new MessageRestful(String.format("%s in HST bean: %s", entry.getMessage(), entry.getBeanName())));
            } else if (actionType == ActionType.CREATED_CLASS) {
                messages.add(new MessageRestful(String.format("%s (%s)", entry.getMessage(), entry.getBeanPath())));
            } else {
                messages.add(new MessageRestful(entry.getMessage()));
            }
        }
    }


    /**
     * Builds an in-memory graph by parsing XML namespaces. This graph can be used to write HST beans
     *
     * @param directory       starting directory (where we scan for document templates)
     * @param context         plugin context instance
     * @param sourceExtension extension used for source files e.g. {@code "java"}
     * @return a list of MemoryBeans or empty list if nothing is found
     */


    public static Map<String, Path> mapExitingBeanNames(final PluginContext context, final String sourceFileExtension) {
        final Map<String, Path> retVal = new HashMap<>();
        final List<Path> exitingBeans = findExitingBeans(context, sourceFileExtension);
        for (Path exitingBean : exitingBeans) {
            retVal.put(exitingBean.toFile().getName(), exitingBean);
        }
        // TODO improve
        return retVal;
    }

    /**
     * Find all existing HST beans (which annotated with {@code @Node})
     *
     * @param context             plugin context instance
     * @param sourceFileExtension file extension, e.g. {@code "java"}
     * @return a list of beans or an empty list if nothing was found
     */
    public static List<Path> findExitingBeans(final PluginContext context, final String sourceFileExtension) {
        final Path startDir = context.getBeansPackagePath();
        final List<Path> existingBeans = new ArrayList<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDir, directories);
        final String pattern = "*." + sourceFileExtension;
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path path : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(path);
                    if (nodeJcrType != null) {
                        existingBeans.add(path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }

        return existingBeans;

    }


}
