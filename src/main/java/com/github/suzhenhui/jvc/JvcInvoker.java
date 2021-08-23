package com.github.suzhenhui.jvc;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

import java.util.Collections;

public class JvcInvoker {
    private final Invoker invoker;
    private final MavenProject project;

    public JvcInvoker(MavenProject project) {
        this.invoker = new DefaultInvoker();
        this.project = project;
    }

    public void invoke(String command) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(project.getFile());
        request.setGoals(Collections.singletonList(command));
        invoker.execute(request);
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public MavenProject getProject() {
        return project;
    }
}
