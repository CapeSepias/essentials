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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.MessageEvent;
import org.onehippo.cms7.essentials.dashboard.model.ActionType;
import org.onehippo.cms7.essentials.dashboard.model.BeanWriterLogEntry;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentBean;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentChildNode;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoContentProperty;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.dashboard.utils.code.EssentialsGeneratedMethod;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.onehippo.cms7.services.contenttype.HippoContentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
public class ContentBeansService {

    public static final String HIPPO_GALLERY_IMAGE_SET_BEAN = "HippoGalleryImageSetBean";
    public static final String HIPPO_GALLERY_IMAGE_SET_CLASS = "HippoGalleryImageSet";
    private static Logger log = LoggerFactory.getLogger(ContentBeansService.class);

    private final PluginContext context;
    private final String baseSupertype;
    private static final String BASE_COMPOUND_TYPE = "hippo:compound";
    public static final String MSG_ADDED_METHOD = "@@@ added [{}] method";
    public static final String CONTEXT_BEAN_DATA = BeanWriterUtils.class.getName();
    public static final String CONTEXT_BEAN_IMAGE_SET = BeanWriterUtils.class.getName() + "imageset";

    private final Set<HippoContentBean> contentBeans;
    /**
     * How many loops we run (beans extending none existing beans)
     */
    private static final int MISSING_DEPTH_MAX = 5;
    private int missingBeansDepth = 0;
    final EventBus eventBus;

    public ContentBeansService(final PluginContext context, final EventBus eventBus) {
        this.context = context;
        this.eventBus = eventBus;
        this.baseSupertype = context.getProjectNamespacePrefix() + ':' + "basedocument";
        this.contentBeans = getContentBeans();

    }


    public void createBeans() throws RepositoryException {

        final Map<String, Path> existing = findExitingBeans();
        final List<HippoContentBean> missingBeans = Lists.newArrayList(filterMissingBeans(existing));
        final Iterator<HippoContentBean> missingBeanIterator = missingBeans.iterator();
        for (; missingBeanIterator.hasNext(); ) {
            final HippoContentBean missingBean = missingBeanIterator.next();
            // check if directly extending compound:
            final Set<String> superTypes = missingBean.getSuperTypes();
            if (superTypes.size() == 1 && superTypes.iterator().next().equals(BASE_COMPOUND_TYPE)) {
                createBaseBean(missingBean);
                missingBeanIterator.remove();
            } else {
                final String parent = findExistingParent(missingBean, existing);
                if (parent != null) {
                    log.debug("found parent: {}, {}", parent, missingBean);
                    missingBeanIterator.remove();
                    createBean(missingBean, existing.get(parent));
                }
            }
        }
        // process beans without resolved parent beans
        processMissing(missingBeans);
        processProperties();
        // check if still missing(beans that extended missing beans)
        final Iterator<HippoContentBean> extendedMissing = filterMissingBeans(findExitingBeans());
        final boolean hasNonCreatedBeans = extendedMissing.hasNext();
        if (missingBeansDepth < MISSING_DEPTH_MAX && hasNonCreatedBeans) {
            missingBeansDepth++;
            createBeans();
        } else if (hasNonCreatedBeans) {
            log.error("Not all beans were created: {}", extendedMissing);
        }

    }

    private Iterator<HippoContentBean> filterMissingBeans(final Map<String, Path> existing) {
        final Iterable<HippoContentBean> missingBeans = Iterables.filter(contentBeans, new Predicate<HippoContentBean>() {
            @Override
            public boolean apply(HippoContentBean b) {
                return !existing.containsKey(b.getName());
            }
        });
        // process beans with known (project) supertypes:
        return Lists.newArrayList(missingBeans).iterator();
    }

    private void processProperties() {
        final Map<String, Path> existing = findExitingBeans();
        for (HippoContentBean bean : contentBeans) {
            final Path beanPath = existing.get(bean.getName());
            if (beanPath != null) {
                final String parent = findExistingParent(bean, existing);
                final Path parentPath = existing.get(parent);
                if (parentPath != null) {
                    final ExistingMethodsVisitor parentMethodCollection = JavaSourceUtils.getMethodCollection(parentPath);
                    final ExistingMethodsVisitor ownMethodCollection = JavaSourceUtils.getMethodCollection(beanPath);
                    final Set<String> existingMethods = ownMethodCollection.getMethodInternalNames();
                    existingMethods.addAll(parentMethodCollection.getMethodInternalNames());
                    addMethods(bean, beanPath, existingMethods);
                } else {
                    final ExistingMethodsVisitor ownMethodCollection = JavaSourceUtils.getMethodCollection(beanPath);
                    addMethods(bean, beanPath, ownMethodCollection.getMethodInternalNames());
                }

            }
        }
    }


    private EssentialsGeneratedMethod extractMethod(final String methodName, final Iterable<EssentialsGeneratedMethod> generatedMethods) {
        for (EssentialsGeneratedMethod generatedMethod : generatedMethods) {
            if (generatedMethod.getMethodName().equals(methodName)) {
                return generatedMethod;
            }
        }
        return null;
    }

    private void processMissing(final List<HippoContentBean> missingBeans) {
        for (HippoContentBean missingBean : missingBeans) {
            final SortedSet<String> mySupertypes = missingBean.getContentType().getSuperTypes();
            if (mySupertypes.contains("hippogallery:relaxed")) {
                final Path javaClass = createJavaClass(missingBean);
                JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), missingBean.getName(), missingBean.getName());
                JavaSourceUtils.addExtendsClass(javaClass, HIPPO_GALLERY_IMAGE_SET_CLASS);
                JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_IMAGE_SET_IMPORT);
                addMethods(missingBean, javaClass, new ArrayList<String>());
            }
        }
    }

    private String findExistingParent(final HippoContentBean missingBean, final Map<String, Path> existing) {
        final Set<String> superTypes = missingBean.getSuperTypes();
        if (superTypes.size() == 1 && superTypes.iterator().next().equals(baseSupertype)) {
            return baseSupertype;
        }
        // extends a document
        for (String superType : superTypes) {
            if (!superType.equals(baseSupertype) && existing.containsKey(superType)) {
                // TODO improve nested types
                return superType;
            }
        }
        return null;
    }


    public final Set<HippoContentBean> getContentBeans() {
        try {
            final Set<HippoContentBean> beans = new HashSet<>();
            final Set<ContentType> projectContentTypes = getProjectContentTypes();
            for (ContentType projectContentType : projectContentTypes) {
                final HippoContentBean bean = new HippoContentBean(context, projectContentType);
                beans.add(bean);
            }
            return beans;
        } catch (RepositoryException e) {
            log.error("Error fetching beans", e);
        }
        return Collections.emptySet();
    }


    /**
     * Fetch project content types
     *
     * @return empty collection if no types are found
     * @throws javax.jcr.RepositoryException
     */
    public Set<ContentType> getProjectContentTypes() throws RepositoryException {
        final String namespacePrefix = context.getProjectNamespacePrefix();
        final Set<ContentType> projectContentTypes = new HashSet<>();
        final Session session = context.createSession();
        try {
            final ContentTypeService service = new HippoContentTypeService(session);
            final ContentTypes contentTypes = service.getContentTypes();
            final SortedMap<String, Set<ContentType>> typesByPrefix = contentTypes.getTypesByPrefix();
            for (Map.Entry<String, Set<ContentType>> entry : typesByPrefix.entrySet()) {
                final String key = entry.getKey();
                final Set<ContentType> value = entry.getValue();
                if (key.equals(namespacePrefix)) {
                    projectContentTypes.addAll(value);
                    return projectContentTypes;
                }
            }
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return Collections.emptySet();
    }
    //############################################
    // UTILS
    //############################################


    private Map<String, Path> findExitingBeans() {
        final Path startDir = context.getBeansPackagePath();
        final Map<String, Path> existingBeans = new HashMap<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDir, directories);
        final String pattern = "*.java";
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path path : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(path);
                    if (nodeJcrType != null) {
                        existingBeans.put(nodeJcrType, path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }
        return existingBeans;

    }


    private void createBaseBean(final HippoContentBean bean) {
        final Path javaClass = createJavaClass(bean);
        JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), bean.getName(), bean.getName());
        JavaSourceUtils.addExtendsClass(javaClass, "HippoDocument");
        JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_DOCUMENT_IMPORT);
    }


    private void addMethods(final HippoContentBean bean, final Path beanPath, final Collection<String> existing) {
        final List<HippoContentProperty> properties = bean.getProperties();
        for (HippoContentProperty property : properties) {
            final String name = property.getName();
            if (existing.contains(name)) {
                log.debug("Property already exists {}", name);
                continue;
            }
            final String type = property.getType();
            log.debug("processing missing property, BEAN: {}, PROPERTY: {}", bean.getName(), property.getName());

            if (type == null) {
                log.error("Missing type for property, cannot create method {}", property.getName());
                continue;
            }
            final boolean multiple = property.isMultiple();
            String methodName;
            switch (type) {
                case "String":
                case "Html":
                case "Password":
                case "Docbase":
                case "Text":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodString(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    eventBus.post(new MessageEvent(String.format("Successfully created method: %s", methodName)));
                    break;

                case "Date":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodCalendar(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    eventBus.post(new MessageEvent(String.format("Successfully created method: %s", methodName)));
                    break;
                case "Boolean":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodBoolean(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    eventBus.post(new MessageEvent(String.format("Successfully created method: %s", methodName)));
                    break;
                case "Long":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodLong(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    eventBus.post(new MessageEvent(String.format("Successfully created method: %s", methodName)));
                    break;
                case "Double":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodDouble(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    eventBus.post(new MessageEvent(String.format("Successfully created method: %s", methodName)));
                    break;
                default:

                    final String message = String.format("TODO: Beanwriter: Failed to create getter for property: %s of type: %s", property.getName(), type);
                    JavaSourceUtils.addClassJavaDoc(beanPath, message);
                    log.warn(message);
                    break;
            }
        }
        //############################################
        // NODE TYPES
        //############################################
        final List<HippoContentChildNode> children = bean.getChildren();
        for (HippoContentChildNode child : children) {
            final String name = child.getName();
            if (existing.contains(name)) {
                log.debug("Node method already exists {}", name);
                continue;
            }
            final String type = child.getType();
            log.debug("processing missing node, BEAN: {}, CHILD: {}", bean.getName(), child.getName());

            if (type == null) {
                log.error("Missing type for node, cannot create method {}", child.getName());
                continue;
            }
            final boolean multiple = child.isMultiple();
            String methodName;
            switch (type) {
                case "hippostd:html":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoHtml(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    break;

                case "hippogallerypicker:imagelink":
                    methodName = GlobalUtils.createMethodName(name);
                    final Path path = extractPath();
                    if (path == null) {
                        JavaSourceUtils.addBeanMethodImageLink(beanPath, methodName, name, multiple);
                    } else {
                        final String className = JavaSourceUtils.getClassName(path);
                        final String importName = JavaSourceUtils.getImportName(path);
                        JavaSourceUtils.addBeanMethodInternalImageSet(beanPath, className, importName, methodName, name, multiple);
                    }
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    break;
                case "hippo:mirror":
                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoMirror(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    break;
                case "hippogallery:image":

                    methodName = GlobalUtils.createMethodName(name);
                    JavaSourceUtils.addBeanMethodHippoImage(beanPath, methodName, name, multiple);
                    existing.add(name);
                    context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                    log.debug(MSG_ADDED_METHOD, methodName);
                    break;
                default:
                    // check if project type is used:
                    final String prefix = child.getPrefix();
                    if (prefix.equals(context.getProjectNamespacePrefix())) {
                        final Map<String, Path> existingBeans = findExitingBeans();
                        for (Map.Entry<String, Path> entry : existingBeans.entrySet()) {
                            final Path myBeanPath = entry.getValue();
                            final HippoEssentialsGeneratedObject a = JavaSourceUtils.getHippoGeneratedAnnotation(myBeanPath);
                            if (a != null && a.getInternalName().equals(type)) {
                                final String className = JavaSourceUtils.getClassName(myBeanPath);
                                methodName = GlobalUtils.createMethodName(name);
                                final String importPath = JavaSourceUtils.getImportName(myBeanPath);
                                JavaSourceUtils.addBeanMethodInternalType(beanPath, className, importPath, methodName, name, multiple);
                                context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(beanPath.toString(), methodName, ActionType.CREATED_METHOD));
                                return;
                            }
                        }
                    }
                    final String message = String.format("TODO: Beanwriter: Failed to create getter for node type: %s", type);
                    JavaSourceUtils.addClassJavaDoc(beanPath, message);
                    log.warn(message);
                    break;
            }
        }

    }

    private Path extractPath() {
        final Multimap<String, Object> pluginContextData = context.getPluginContextData();
        final Collection<Object> data = pluginContextData.get(CONTEXT_BEAN_IMAGE_SET);
        Path path = null;
        if (data != null && data.size() == 1) {
            final Object next = data.iterator().next();
            if (next instanceof Path) {
                path = (Path) next;
            }
        }
        return path;
    }


    public Map<String, Path> getExistingImageTypes() {
        final Map<String, Path> existing = findExitingBeans();
        final Map<String, Path> imageTypes = new HashMap<>();
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_CLASS, null);
        for (Path path : existing.values()) {
            final String myClass = JavaSourceUtils.getClassName(path);
            final String extendsClass = JavaSourceUtils.getExtendsClass(path);
            if (!Strings.isNullOrEmpty(extendsClass) && extendsClass.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
                imageTypes.put(myClass, path);
            }

        }
        return imageTypes;
    }

    @SuppressWarnings("rawtypes")
    public void convertImageMethods(final String newImageNamespace) {
        final Map<String, Path> existing = findExitingBeans();
        final Map<String, String> imageTypes = new HashMap<>();
        final Set<Path> imageTypePaths = new HashSet<>();
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_CLASS, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet");
        imageTypes.put(HIPPO_GALLERY_IMAGE_SET_BEAN, "org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean");
        String newReturnType = null;
        for (Path path : existing.values()) {
            final String myClass = JavaSourceUtils.getClassName(path);
            final String extendsClass = JavaSourceUtils.getExtendsClass(path);
            final HippoEssentialsGeneratedObject annotation = JavaSourceUtils.getHippoGeneratedAnnotation(path);
            if (!Strings.isNullOrEmpty(extendsClass) && extendsClass.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
                imageTypes.put(myClass, JavaSourceUtils.getImportName(path));
                imageTypePaths.add(path);
            }
            if (annotation != null && newImageNamespace.equals(annotation.getInternalName())) {
                newReturnType = myClass;
            }
        }
        if (newImageNamespace.equals(HIPPO_GALLERY_IMAGE_SET_BEAN) || newImageNamespace.equals(HIPPO_GALLERY_IMAGE_SET_CLASS)) {
            newReturnType = HIPPO_GALLERY_IMAGE_SET_CLASS;
        }
        if (Strings.isNullOrEmpty(newReturnType)) {
            log.warn("Could not find return type for image set namespace: {}", newImageNamespace);
            return;
        }
        log.info("Converting existing image beans to new type: {}", newReturnType);
        for (Map.Entry<String, Path> entry : existing.entrySet()) {
            // check if image type and skip if so:
            final Path path = entry.getValue();
            if(imageTypePaths.contains(path)){
                continue;
            }
            final ExistingMethodsVisitor methods = JavaSourceUtils.getMethodCollection(path);
            final List<EssentialsGeneratedMethod> generatedMethods = methods.getGeneratedMethods();
            for (EssentialsGeneratedMethod m : generatedMethods) {
                final Type type = m.getMethodDeclaration().getReturnType2();
                if (type.isSimpleType()) {
                    final SimpleType simpleType = (SimpleType) type;
                    final String returnType = simpleType.getName().getFullyQualifiedName();
                    // check if image type and different than new return type
                    if (imageTypes.containsKey(returnType) && !returnType.equals(newReturnType)) {
                        log.info("Found image type: {}", returnType);
                        updateImageMethod(path, returnType, newReturnType, imageTypes.get(newReturnType));
                    }
                } else if (getParameterizedType(type) != null) {
                    final String returnType = getParameterizedType(type);
                    if (imageTypes.containsKey(returnType) && !returnType.equals(newReturnType)) {
                        log.info("Found image type: {}", returnType);
                        updateImageMethod(path, returnType, newReturnType, imageTypes.get(newReturnType));
                    }
                }
            }
        }
    }

    private String getParameterizedType(final Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Type myType = parameterizedType.getType();
        @SuppressWarnings("rawtypes")
        final List myArguments = parameterizedType.typeArguments();
        if (myArguments != null && myArguments.size() == 1
                && myType != null && myType.isSimpleType() && ((SimpleType) myType).getName().getFullyQualifiedName().equals("List")) {
            final Object o = myArguments.get(0);
            if (o instanceof SimpleType) {
                final SimpleType paramClazz = (SimpleType) o;
                return paramClazz.getName().getFullyQualifiedName();

            }
        }
        return null;
    }


    private void updateImageMethod(final Path path, final String oldReturnType, final String newReturnType, final String importStatement) {
        final CompilationUnit deleteUnit = JavaSourceUtils.getCompilationUnit(path);
        final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(path);
        final List<EssentialsGeneratedMethod> generatedMethods = methodCollection.getGeneratedMethods();
        final Map<String, EssentialsGeneratedMethod> deletedMethods = new HashMap<>();

        deleteUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                final String methodName = node.getName().getFullyQualifiedName();
                final Type type = node.getReturnType2();
                if (type.isSimpleType()) {
                    final SimpleType simpleType = (SimpleType) type;
                    final String returnTypeName = simpleType.getName().getFullyQualifiedName();
                    final EssentialsGeneratedMethod method = extractMethod(methodName, generatedMethods);
                    if (method == null) {
                        return super.visit(node);
                    }
                    if (returnTypeName.equals(oldReturnType)) {
                        node.delete();
                        deletedMethods.put(method.getMethodName(), method);
                        return super.visit(node);
                    }
                } else if (getParameterizedType(type) != null) {
                    final String returnTypeName = getParameterizedType(type);
                    final EssentialsGeneratedMethod method = extractMethod(methodName, generatedMethods);
                    if (method == null) {
                        return super.visit(node);
                    }
                    if (returnTypeName.equals(oldReturnType)) {
                        node.delete();
                        deletedMethods.put(method.getMethodName(), method);
                        return super.visit(node);
                    }

                    log.info("oldReturnType {}", oldReturnType);
                }
                return super.visit(node);
            }
        });
        if (deletedMethods.size() > 0) {
            final AST deleteAst = deleteUnit.getAST();
            final String deletedSource = JavaSourceUtils.rewrite(deleteUnit, deleteAst);
            GlobalUtils.writeToFile(deletedSource, path);
            for (Map.Entry<String, EssentialsGeneratedMethod> entry : deletedMethods.entrySet()) {
                final EssentialsGeneratedMethod oldMethod = entry.getValue();
                // Add replacement methods:
                if (newReturnType.equals(HIPPO_GALLERY_IMAGE_SET_CLASS) || newReturnType.equals(HIPPO_GALLERY_IMAGE_SET_BEAN)) {
                    JavaSourceUtils.addBeanMethodHippoImageSet(path, oldMethod.getMethodName(), oldMethod.getInternalName(), oldMethod.isMultiType());
                } else {
                    JavaSourceUtils.addBeanMethodInternalImageSet(path, newReturnType, importStatement, oldMethod.getMethodName(), oldMethod.getInternalName(), oldMethod.isMultiType());
                }
                log.debug("Replaced old method: {} with new return type: {}", oldMethod.getMethodName(), newReturnType);
                context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(path.toString(), oldMethod.getMethodName(), ActionType.MODIFIED_METHOD));
            }
        }
    }

    /**
     * Create a bean for giving parent bean path
     *
     * @param bean       none existing bean
     * @param parentPath existing parent bean
     */

    private void createBean(final HippoContentBean bean, final Path parentPath) {
        final Path javaClass = createJavaClass(bean);
        JavaSourceUtils.createHippoBean(javaClass, context.beansPackageName(), bean.getName(), bean.getName());
        final String extendsName = FilenameUtils.removeExtension(parentPath.toFile().getName());
        JavaSourceUtils.addExtendsClass(javaClass, extendsName);
        JavaSourceUtils.addImport(javaClass, EssentialConst.HIPPO_DOCUMENT_IMPORT);

    }

    private Path createJavaClass(final HippoContentBean bean) {
        String name = bean.getName();
        if (name.indexOf(',') != -1) {
            name = name.split(",")[0];
        }
        final String className = GlobalUtils.createClassName(name);
        final Path path = JavaSourceUtils.createJavaClass(context.getSiteJavaRoot(), className, context.beansPackageName(), null);
        context.addPluginContextData(CONTEXT_BEAN_DATA, new BeanWriterLogEntry(ActionType.CREATED_CLASS, path.toString(), className));

        return path;
    }


}
