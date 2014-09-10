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

package org.onehippo.cms7.essentials.plugins.contentblocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.*;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.utils.*;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.*;
import org.onehippo.cms7.essentials.plugins.contentblocks.updater.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.parsing.combinator.testing.Str;

@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("contentblocks")
public class ContentBlocksResource extends BaseResource {
    private static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype/hipposysedit:nodetype";
    private static final String EDITOR_TEMPLATES_NODE = "editor:templates/_default_";
    private static final String ERROR_MSG = "The Content Blocks plugin encountered an error, check the log messages for more info.";
    private static final String NODE_OPTIONS = "cluster.options";
    private static final String PROP_CAPTION = "caption";
    private static final String PROP_COMPOUNDLIST = "compoundList";
    private static final String PROP_MAXITEMS = "maxitems";
    private static final String PROP_PATH = "hipposysedit:path";
    private static final String PROP_PICKERTYPE = "contentPickerType";

    private static Logger log = LoggerFactory.getLogger(ContentBlocksResource.class);

    @GET
    @Path("/")
    public List<DocumentTypeRestful> getContentBlocks() {
        List<DocumentRestful> documents = ContentTypeServiceUtils.fetchDocumentsFromOwnNamespace(ContentTypeServiceUtils.Type.DOCUMENT);
        List<DocumentTypeRestful> cbDocuments = new ArrayList<>();

        final Session session = GlobalUtils.createSession();
        try {
            for (DocumentRestful documentType : documents) {
                if ("basedocument".equals(documentType.getName())) {
                    continue; // don't expose the base document as you can't instantiate it.
                }
                final String primaryType = documentType.getFullName();
                final DocumentTypeRestful cbDocument = new DocumentTypeRestful();
                cbDocument.setId(primaryType);
                cbDocument.setName(makeDisplayName(session, primaryType));
                populateContentBlocksFields(cbDocument, session);
                cbDocuments.add(cbDocument);
            }
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return cbDocuments;
    }

    /**
     * Process a request to update the content blocks configuration
     *
     * @param docTypes      The per-document type update requests.
     * @param response      for signalling an error.
     * @return              feedback message.
     */
    @POST
    @Path("/")
    public MessageRestful update(List<DocumentTypeRestful> docTypes, @Context HttpServletResponse response) {
        final List<UpdateRequest> updaters = new ArrayList<>();
        int updatersRun = 0;
        final Session session = GlobalUtils.createSession();

        try {
            for (DocumentTypeRestful docType : docTypes) {
                updateDocumentType(docType, session, updaters);
            }
            session.save();
            updatersRun = executeUpdaters(session, updaters);
        } catch (RepositoryException e) {
            log.warn("Problem saving the JCR changes after updating the content blocks fields.", e);
            return createErrorMessage(ERROR_MSG, response);
        } catch (ContentBlocksException e) {
            return createErrorMessage(e.getMessage(), response);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        String message = "Successfully updated content blocks settings.";
        if (updatersRun > 0) {
            message += " " + updatersRun + " content updater" + (updatersRun > 1 ? "s were" : " was") + " executed"
                    + " to adjust your content. You may want to delete content updaters from the history and use them"
                    + " on other environments, too.";
        }

        return new MessageRestful(message);
    }

    @GET
    @Path("/compounds")
    public List<CompoundRestful> getCompounds() {
        List<DocumentRestful> compoundTypes = ContentTypeServiceUtils.fetchDocuments(ContentTypeServiceUtils.Type.COMPOUND, false);
        List<String> compoundTypeNames = new ArrayList<>(Arrays.asList(
                "hippo:mirror", // TODO: avoid hard-coding these. How can I use the content type service to achieve this?
                "hippo:resource",
                "hippostd:html",
                "hippogallerypicker:imagelink"
        ));
        List<CompoundRestful> cbCompounds = new ArrayList<>();

        for (DocumentRestful compoundType : compoundTypes) {
            compoundTypeNames.add(compoundType.getFullName());
        }

        final Session session = GlobalUtils.createSession();
        try {
            for (String primaryType : compoundTypeNames) {
                if ("hippo:compound".equals(primaryType)) {
                    continue; // don't expose the base compound as you don't want to instantiate it.
                }
                final CompoundRestful cbCompound = new CompoundRestful();
                cbCompound.setId(primaryType);
                cbCompound.setName(makeDisplayName(session, primaryType));
                cbCompounds.add(cbCompound);
            }
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return cbCompounds;
    }

    /**
     * For a given document type, check what content blocks fields there are, and build a structure describing those.
     *
     * @param docType representation of the document type.
     * @param session JCR session.
     */
    private void populateContentBlocksFields(final DocumentTypeRestful docType, final Session session) {
        final String primaryType = docType.getId();
        docType.setContentBlocksFields(new ArrayList<ContentBlocksFieldRestful>());
        try {
            final NodeIterator it = findContentBlockFields(primaryType, session);

            while (it.hasNext()) {
                final Node fieldNode = it.nextNode();
                final ContentBlocksFieldRestful field = new ContentBlocksFieldRestful();
                field.setName(fieldNode.getProperty(PROP_CAPTION).getString());
                field.setPickerType(fieldNode.getProperty(PROP_PICKERTYPE).getString());
                if (fieldNode.getNode(NODE_OPTIONS).hasProperty(PROP_MAXITEMS)) {
                    field.setMaxItems(Long.parseLong(fieldNode.getNode(NODE_OPTIONS).getProperty(PROP_MAXITEMS).getString()));
                }
                final String[] compoundNames = fieldNode.getProperty(PROP_COMPOUNDLIST).getString().split(",");
                for (String compoundName : compoundNames) {
                    field.addCompoundRef(compoundName);
                }

                docType.addContentBlocksField(field);
            }
        } catch (RepositoryException e) {
            log.warn("Problem populating content blocks fields for primary type '" + primaryType + "'.", e);
        }
    }

    /**
     * Update the content blocks fields for a specific document type
     *
     * @param docType    document type to adjust
     * @param session    JCR session
     * @throws ContentBlocksException for error message propagation
     */
    private void updateDocumentType(final DocumentTypeRestful docType, final Session session, final List<UpdateRequest> updaters)
            throws ContentBlocksException {
        final String primaryType = docType.getId();

        // Compare existing content blocks fields with requested ones
        try {
            final NodeIterator it = findContentBlockFields(primaryType, session);
            while (it.hasNext()) {
                final Node fieldNode = it.nextNode();
                final String fieldName = fieldNode.getProperty(PROP_CAPTION).getString();
                boolean updated = false;
                for (ContentBlocksFieldRestful field : docType.getContentBlocksFields()) {
                    if (fieldName.equals(field.getOriginalName())) {
                        updated = true;
                        docType.getContentBlocksFields().remove(field);
                        updateField(fieldNode, docType, field, updaters);
                        // the fieldNode may have been renamed (copied), don't use it anymore!
                        break;
                    }
                }
                if (!updated) {
                    deleteField(fieldNode, docType, fieldName, updaters);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Problem retrieving existing content blocks fields for document type '" + primaryType + "'.", e);
            throw new ContentBlocksException(ERROR_MSG);
        }

        // add new content blocks fields
        for (ContentBlocksFieldRestful field : docType.getContentBlocksFields()) {
            createField(field, docType, session);
        }
    }

    /**
     * Create a new content blocks field.
     *
     * @param field   desired field parameters
     * @param docType document type to extend
     * @param session JCR session
     * @throws ContentBlocksException for error message propagation
     */
    private void createField(final ContentBlocksFieldRestful field, final DocumentTypeRestful docType, final Session session)
            throws ContentBlocksException {
        final String newNodeName = makeNodeName(field.getName());
        final String primaryType = docType.getId();
        final String errorMsg = "Failed to create content blocks field '" + field.getName() + "' for document type '"
                              + docType.getName() + "'.";
        InputStream in = null;

        try {
            final Node docTypeNode = getDocTypeNode(session, primaryType);
            final Node nodeTypeNode = getNodeTypeNode(docTypeNode, primaryType);
            final Node editorTemplateNode = getEditorTemplateNode(docTypeNode, primaryType);

            if (editorTemplateNode.hasNode(newNodeName)) {
                throw new ContentBlocksException(
                        "Document type '" + docType.getName() + "' already has field '" + field.getName() + "'.");
            }

            // Determine document type layout
            String fieldType;
            final String pluginClass = editorTemplateNode.getNode("root").getProperty("plugin.class").getString();
            switch (pluginClass) {
                case "org.hippoecm.frontend.service.render.ListViewPlugin":
                    fieldType = "${cluster.id}.field";
                    break;
                case "org.hippoecm.frontend.editor.layout.TwoColumn":
                    fieldType = "${cluster.id}.left.item";
                    break;
                default:
                    log.error("Can't determine layout of document type " + docType.getName() + ".");
                    throw new ContentBlocksException(errorMsg);
            }

            // Build interpolation map
            Map<String, Object> data = new HashMap<>();

            data.put("name", newNodeName);
            data.put("namespace", PluginContextFactory.getContext().getProjectNamespacePrefix());
            data.put("caption", field.getName());
            data.put("pickerType", field.getPickerType());
            data.put("compoundList", makeCompoundList(field));
            data.put("fieldType", fieldType);

            // Import nodetype
            String parsed = TemplateUtils.injectTemplate("content_blocks_nodetype.xml", data, getClass());
            if (parsed == null) {
                log.error("Can't read resource 'content_blocks_nodetype.xml'.");
                throw new ContentBlocksException(errorMsg);
            }
            in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));
            ((HippoSession)session).importDereferencedXML(nodeTypeNode.getPath(), in,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                    ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

            // Import editor template
            parsed = TemplateUtils.injectTemplate("content_blocks_template.xml", data, getClass());
            if (parsed == null) {
                log.error("Can't read resource 'content_blocks_template.xml'.");
                throw new ContentBlocksException(errorMsg);
            }
            in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));
            ((HippoSession)session).importDereferencedXML(editorTemplateNode.getPath(), in,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                    ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

            // Set maxitems
            if (field.getMaxItems() > 0) {
                final Node options = editorTemplateNode.getNode(field.getName() + "/" + NODE_OPTIONS);
                options.setProperty(PROP_MAXITEMS, field.getMaxItems());
            }
        } catch (RepositoryException | IOException e) {
            GlobalUtils.refreshSession(session, false);
            log.error("Error in content bocks plugin", e);
            throw new ContentBlocksException(errorMsg);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Update an existing content blocks field
     *
     * We update a field rather than deleting and re-creating it, such that its (adjusted) location, hints and
     * other parameters get preserved.
     *
     * @param oldFieldNode JCR node representing the content blocks field in the document (type) editor
     * @param docType      Document type to update
     * @param field        desired field parameters
     * @throws ContentBlocksException for error message propagation
     */
    private void updateField(Node oldFieldNode, final DocumentTypeRestful docType,
                             final ContentBlocksFieldRestful field, final List<UpdateRequest> updaters)
            throws ContentBlocksException {
        Node fieldNode = oldFieldNode;
        try {
            final String newNodeName = makeNodeName(field.getName());
            final Node oldNodeTypeNode = getNodeTypeNode(fieldNode);
            Node nodeTypeNode = oldNodeTypeNode;

            if (!newNodeName.equals(fieldNode.getName())) {
                if (fieldNode.getParent().hasNode(newNodeName)) {
                    throw new ContentBlocksException(
                            "Document type '" + docType.getName() + "' already has field '" + field.getName() + "'.");
                }

                final String namespace = PluginContextFactory.getContext().getProjectNamespacePrefix();
                final String oldNodePath = nodeTypeNode.getProperty(PROP_PATH).getString();
                final String newNodePath = namespace + ":" + newNodeName;
                nodeTypeNode = JcrUtils.copy(nodeTypeNode, newNodeName, nodeTypeNode.getParent());
                nodeTypeNode.setProperty(PROP_PATH, newNodePath);
                oldNodeTypeNode.remove();

                final String oldNodeCaption = fieldNode.getProperty(PROP_CAPTION).getString();
                fieldNode = JcrUtils.copy(fieldNode, newNodeName, fieldNode.getParent());
                fieldNode.setProperty("field", newNodeName);
                oldFieldNode.remove();

                // schedule updater to fix existing content
                final Map<String, Object> vars = new HashMap<>();
                vars.put("docType", docType.getId());
                vars.put("docName", docType.getName());
                vars.put("oldNodePath", oldNodePath);
                vars.put("oldNodeName", oldNodeCaption);
                vars.put("newNodePath", newNodePath);
                vars.put("newNodeName", field.getName());
                updaters.add(new UpdateRequest("content-updater.xml", vars));
            }

            fieldNode.setProperty(PROP_CAPTION, field.getName());
            fieldNode.setProperty(PROP_PICKERTYPE, field.getPickerType());
            fieldNode.setProperty(PROP_COMPOUNDLIST, makeCompoundList(field));

            // max items
            final Node clusterOptions = fieldNode.getNode(NODE_OPTIONS);
            if (field.getMaxItems() > 0) {
                clusterOptions.setProperty(PROP_MAXITEMS, field.getMaxItems());
            } else if (clusterOptions.hasProperty(PROP_MAXITEMS)) {
                clusterOptions.getProperty(PROP_MAXITEMS).remove();
            }
        } catch (RepositoryException e) {
            final String msg = "Failed to update content blocks field '" + field.getName() + "' from document type '"
                    + docType.getName() + "'.";
            log.warn(msg, e);
            throw new ContentBlocksException(msg);
        }
    }

    /**
     * Delete an existing content blocks field
     *
     * @param fieldNode   JCR node representing the content blocks field in the document (type) editor
     * @param docType     Document type to update
     * @param fieldName   Name of the to-be deleted field.
     * @throws ContentBlocksException for error message propagation
     */
    private void deleteField(final Node fieldNode, final DocumentTypeRestful docType, final String fieldName,
                             final List<UpdateRequest> updaters) throws ContentBlocksException {
        try {
            final Node nodeTypeNode = getNodeTypeNode(fieldNode);
            final String nodePath = nodeTypeNode.getProperty(PROP_PATH).getString();
            final String nodeName = fieldNode.getProperty(PROP_CAPTION).getString();
            fieldNode.remove();
            nodeTypeNode.remove();

            final Map<String, Object> vars = new HashMap<>();
            vars.put("docType", docType.getId());
            vars.put("docName", docType.getName());
            vars.put("nodePath", nodePath);
            vars.put("nodeName", nodeName);
            updaters.add(new UpdateRequest("content-deleter.xml", vars));
        } catch (RepositoryException e) {
            final String msg = "Failed to remove content blocks field '" + fieldName + "' from document type '"
                    + docType.getName() + "'.";
            log.warn(msg, e);
            throw new ContentBlocksException(msg);
        }
    }

    private int executeUpdaters(final Session session, final List<UpdateRequest> updaters) {
        int updatersRun = 0;
        for (UpdateRequest updater : updaters) {
            final String parsed = TemplateUtils.injectTemplate(updater.getResource(), updater.getVars(), getClass());
            if (parsed != null) {
                InputStream in = null;
                try {
                    in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));
                    ((HippoSession)session).importDereferencedXML("/hippo:configuration/hippo:update/hippo:queue", in,
                            ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                            ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);
                    session.save();
                    updatersRun++;
                } catch (RepositoryException | IOException e) {
                    GlobalUtils.refreshSession(session, false);
                    log.error("Error scheduling updater", e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }
        return updatersRun;
    }

    private String makeDisplayName(final Session session, final String primaryType) {
        String name = primaryType;
        try {
            name = HippoNodeUtils.getDisplayValue(session, primaryType);
        } catch (RepositoryException e) {
            log.warn("Problem retrieving translated name for primary type '" + primaryType + "'.", e);
        }
        return name;
    }

    private String makeNodeName(final String caption) {
        // TODO: I believe this is not good enough, as it supports for example commas...
        return NodeNameCodec.encode(caption);
    }

    private String makeCompoundList(final ContentBlocksFieldRestful field) {
        return StringUtils.join(field.getCompoundRefs(), ",");
    }

    private Node getNodeTypeNode(final Node fieldNode) throws RepositoryException {
        final String nodeTypeName = fieldNode.getProperty("field").getString();
        return fieldNode.getParent().getParent().getParent().getNode(HIPPOSYSEDIT_NODETYPE + "/" + nodeTypeName);
    }

    private NodeIterator findContentBlockFields(final String primaryType, final Session session) throws RepositoryException {
        final String queryString = MessageFormat.format("{0}//element(*, frontend:plugin)[@compoundList]",
                                                        HippoNodeUtils.resolvePath(primaryType).substring(1));
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(queryString, EssentialConst.XPATH);
        final QueryResult execute = query.execute();
        return execute.getNodes();
    }

    private Node getDocTypeNode(final Session session, final String docTypeName) throws ContentBlocksException {
        try {
            if (docTypeName.contains(":")) {
                return session.getNode("/hippo:namespaces/" + docTypeName.replace(':', '/'));
            } else {
                return session.getNode("/hippo:namespaces/system/" + docTypeName);
            }
        } catch (RepositoryException e) {
            log.error("Problem retrieving the document type node for '" + docTypeName + "'.", e);
            throw new ContentBlocksException(ERROR_MSG);
        }
    }

    private Node getNodeTypeNode(final Node docTypeNode, final String docTypeName) throws ContentBlocksException {
        try {
            return docTypeNode.getNode(HIPPOSYSEDIT_NODETYPE);
        } catch (RepositoryException e) {
            log.error("Document type " + docTypeName + " is missing nodetype node'.");
            throw new ContentBlocksException(ERROR_MSG);
        }
    }

    private Node getEditorTemplateNode(final Node docTypeNode, final String docTypeName) throws ContentBlocksException {
        try {
            return docTypeNode.getNode(EDITOR_TEMPLATES_NODE);
        } catch (RepositoryException e) {
            log.error("Document type " + docTypeName + " is missing editor template node.");
            throw new ContentBlocksException(ERROR_MSG);
        }
    }
}
