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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public abstract class BaseEvent implements PluginEvent{

    private static final long serialVersionUID = 1L;

    private boolean canUndo;
    @Override
    public void setCanUndo(final boolean canUndo) {
        this.canUndo = canUndo;
    }

    @Override
    public boolean canUndo() {
        return canUndo;
    }

    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public DisplayLocation getDisplayLocation() {
        return DisplayLocation.SYSTEM;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
