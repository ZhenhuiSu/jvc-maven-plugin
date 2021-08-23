package com.github.suzhenhui.jvc;

import fr.dutra.tools.maven.deptree.core.Node;

public class NodeJreVersion {
    private final Node node;
    private final Integer jreVersion;

    public NodeJreVersion(Node node, Integer jreVersion) {
        this.node = node;
        this.jreVersion = jreVersion;
    }

    public Node getNode() {
        return node;
    }

    public Integer getJreVersion() {
        return jreVersion;
    }
}
