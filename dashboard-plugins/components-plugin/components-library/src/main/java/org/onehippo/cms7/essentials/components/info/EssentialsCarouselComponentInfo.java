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

package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */
public interface EssentialsCarouselComponentInfo {


    @Parameter(name = "document", required = false, displayName = "Carousel item 1")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem1();

    @Parameter(name = "document", required = false, displayName = "Carousel item 2")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem2();

    @Parameter(name = "document", required = false, displayName = "Carousel item 3")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem3();

    @Parameter(name = "document", required = false, displayName = "Carousel item 4")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem4();

    @Parameter(name = "document", required = false, displayName = "Carousel item 5")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem5();

    @Parameter(name = "document", required = false, displayName = "Carousel item 6")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem6();

    @Parameter(name = "document", required = false, displayName = "Carousel item 7")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem7();

    @Parameter(name = "document", required = false, displayName = "Carousel item 8")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem8();

    @Parameter(name = "document", required = false, displayName = "Carousel item 9")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem9();

    @Parameter(name = "document", required = false, displayName = "Carousel item 10")
    @DocumentLink(allowCreation = false, docLocation = "/content/documents", docType = "hippo:document")
    String getCarouselItem10();

}
