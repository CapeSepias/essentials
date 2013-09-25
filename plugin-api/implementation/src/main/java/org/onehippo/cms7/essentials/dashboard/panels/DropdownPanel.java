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

package org.onehippo.cms7.essentials.dashboard.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class DropdownPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DropdownPanel.class);
    private final Collection<EventListener<String>> listeners = new CopyOnWriteArrayList<>();
    private final ListChoice<String> dropDown;
    @SuppressWarnings("UnusedDeclaration")
    private String selectedItem;
    private List<String> items;

    public DropdownPanel(final String id, final String title, final Form<?> form, final Collection<String> model, final EventListener<String> listener) {
        this(id, title, form, model);
        addListener(listener);
    }

    public DropdownPanel(final String id, final String title, final Form<?> form, final Collection<String> model, final Collection<EventListener<String>> listeners) {
        this(id, title, form, model);
        listeners.addAll(listeners);
    }

    public DropdownPanel(final String id, final String title, final Form<?> form, final Collection<String> model) {
        super(id);
        items = new ArrayList<>();
        items.addAll(model);
        final PropertyModel<String> siteModel = new PropertyModel<>(this, "selectedItem");
        dropDown = new ListChoice<>("dropDown", siteModel, items);
        dropDown.setNullValid(false);
        dropDown.setOutputMarkupId(true);

        dropDown.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                final String selectedInput = dropDown.getInput();
                if (Strings.isNullOrEmpty(selectedInput)) {
                    log.debug("No site selected");
                    return;
                }
                selectedItem = dropDown.getChoices().get(Integer.valueOf(selectedInput));
                log.info("#selected item: {}", selectedItem);
                for (EventListener<String> listener : listeners) {
                    final Collection<String> selectedItems = new ArrayList<>();
                    selectedItems.add(selectedItem);
                    listener.onSelected(target, selectedItems);
                }
            }
        });
        //############################################
        //  ADD COMPONENTS
        //############################################
        dropDown.setOutputMarkupId(true);
        add(dropDown);
        add(new Label("title", title));
        form.add(this);
    }


    public void changeModel(final AjaxRequestTarget target, final Collection<String> newModel) {
        items.clear();
        if (newModel != null) {
            items.addAll(newModel);
        }
        target.add(this);
    }

    public void removeListener(final EventListener<String> listener) {
        log.debug("@removing event listener {}", listener);
        listeners.remove(listener);
    }

    public void addListener(final EventListener<String> listener) {
        log.debug("@adding listener {}", listener);
        listeners.add(listener);
    }

    public String getSelectedItem() {
        return selectedItem;
    }
}
