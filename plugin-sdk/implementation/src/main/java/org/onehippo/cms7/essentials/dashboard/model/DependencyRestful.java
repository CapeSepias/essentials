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

package org.onehippo.cms7.essentials.dashboard.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.maven.model.Dependency;

/**
 * @version "$Id: DependencyRestful.java 174870 2013-08-23 13:56:24Z mmilicevic $"
 */
@XmlRootElement(name = "dependency")
public class DependencyRestful implements EssentialsDependency, Restful {

    private static final long serialVersionUID = 1L;
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String type;
    private String targetPom;


    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public void setScope(final String scope) {
        this.scope = scope;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getTargetPom() {
        return targetPom;
    }

    @Override
    public void setTargetPom(final String targetPom) {
        this.targetPom = targetPom;
    }

    @Override
    public TargetPom getDependencyTargetPom() {
        return TargetPom.pomForName(targetPom);

    }

    @Override
    public Dependency createMavenDependency() {
        final Dependency mavenDependency = new Dependency();
        mavenDependency.setArtifactId(getArtifactId());
        mavenDependency.setGroupId(getGroupId());
        mavenDependency.setVersion(getVersion());
        mavenDependency.setScope(getScope());
        mavenDependency.setType(getType());
        return mavenDependency;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DependencyRestful{");
        sb.append("groupId='").append(groupId).append('\'');
        sb.append(", artifactId='").append(artifactId).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
