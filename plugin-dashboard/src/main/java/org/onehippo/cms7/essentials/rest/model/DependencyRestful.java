package org.onehippo.cms7.essentials.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: DependencyRestful.java 174870 2013-08-23 13:56:24Z mmilicevic $"
 */
@XmlRootElement(name = "dependency")
public class DependencyRestful implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DependencyRestful.class);
    private String groupId;
    private String artifactId;
    private String repositoryId;
    private String repositoryUrl;
    private String version;
    private String scope;


    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(final String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(final String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DependencyRestful{");
        sb.append("groupId='").append(groupId).append('\'');
        sb.append(", artifactId='").append(artifactId).append('\'');
        sb.append(", repositoryId='").append(repositoryId).append('\'');
        sb.append(", repositoryUrl='").append(repositoryUrl).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
