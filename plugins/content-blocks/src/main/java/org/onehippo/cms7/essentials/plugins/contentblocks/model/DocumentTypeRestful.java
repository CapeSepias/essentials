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

package org.onehippo.cms7.essentials.plugins.contentblocks.model;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

import java.util.ArrayList;
import java.util.List;

public class DocumentTypeRestful implements Restful {
    private String id;
    private String name;
    private List<ContentBlocksFieldRestful> contentBlocksFields;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(ContentBlocksFieldRestful.class)
    })
    public List<ContentBlocksFieldRestful> getContentBlocksFields() {
        return contentBlocksFields;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(ContentBlocksFieldRestful.class)
    })
    public void setContentBlocksFields(final List<ContentBlocksFieldRestful> contentBlocksFields) {
        this.contentBlocksFields = contentBlocksFields;
    }

    public void addContentBlocksField(final ContentBlocksFieldRestful contentBlocksField) {
        if (contentBlocksFields == null) {
            contentBlocksFields = new ArrayList<>();
        }

        contentBlocksFields.add(contentBlocksField);
    }
}
