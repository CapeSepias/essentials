package org.onehippo.cms7.essentials.site.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="hippoplugins:textdocument")
public class TextDocument extends BaseDocument{
    

    public String getSummary() {
        return getProperty("hippoplugins:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("hippoplugins:body");    
    }

}
