package com.github.suzhenhui.jvc.mojo;

import com.github.suzhenhui.jvc.Constant;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "clean")
public class CleanMojo extends AbstractMojo {
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() {
        File jvcDependencyTreeFile = new File(project.getBasedir().getAbsolutePath() + "/" + Constant.DEPENDENCY_TREE_FILE);
        if (!jvcDependencyTreeFile.exists()) {
            getLog().info("jvc-dependency-tree not exist");
        } else {
            jvcDependencyTreeFile.deleteOnExit();
            getLog().info("clean jvc-dependency-tree success");
        }
    }
}
