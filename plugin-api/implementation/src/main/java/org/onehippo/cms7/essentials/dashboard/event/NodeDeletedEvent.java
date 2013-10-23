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

package org.onehippo.cms7.essentials.dashboard.event;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class NodeDeletedEvent extends NodeEvent{

    private static final Logger log = LoggerFactory.getLogger(NodeDeletedEvent.class);

    private static final long serialVersionUID = 1L;

    private String message;

    public NodeDeletedEvent(final Item node) {
        try {
            final String path = node.getPath();
            message = "Deleted node: " + path;

        } catch (RepositoryException e) {
            log.error("Error logging event", e);
            message = e.getMessage();
        }
    }

    @Override
    public String getMessage() {
        return message;
    }
}
