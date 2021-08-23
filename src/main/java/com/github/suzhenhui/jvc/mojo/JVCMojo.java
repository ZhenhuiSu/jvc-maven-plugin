package com.github.suzhenhui.jvc.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.suzhenhui.jvc.*;
import fr.dutra.tools.maven.deptree.core.InputType;
import fr.dutra.tools.maven.deptree.core.Node;
import fr.dutra.tools.maven.deptree.core.Parser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.aether.RepositorySystemSession;

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "check-jre-version", defaultPhase = LifecyclePhase.COMPILE)
public class JVCMojo extends AbstractMojo {
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

    /**
     * A comma-separated list of artifacts to filter the serialized dependency tree by, or <code>null</code> not to
     * filter the dependency tree. The filter syntax is:
     *
     * <pre>
     * [groupId]:[artifactId]:[type]:[version]
     * </pre>
     * <p>
     * where each pattern segment is optional and supports full and partial <code>*</code> wildcards. An empty pattern
     * segment is treated as an implicit wildcard.
     * <p>
     * For example, <code>org.apache.*</code> will match all artifacts whose group id starts with
     * <code>org.apache.</code>, and <code>:::*-SNAPSHOT</code> will match all snapshot artifacts.
     * </p>
     */
    @Parameter(property = "includes")
    private String includes;

    /**
     * A comma-separated list of artifacts to filter from the serialized dependency tree, or <code>null</code> not to
     * filter any artifacts from the dependency tree. The filter syntax is:
     *
     * <pre>
     * [groupId]:[artifactId]:[type]:[version]
     * </pre>
     * <p>
     * where each pattern segment is optional and supports full and partial <code>*</code> wildcards. An empty pattern
     * segment is treated as an implicit wildcard.
     * <p>
     * For example, <code>org.apache.*</code> will match all artifacts whose group id starts with
     * <code>org.apache.</code>, and <code>:::*-SNAPSHOT</code> will match all snapshot artifacts.
     * </p>
     */
    @Parameter(property = "excludes")
    private String excludes;

    /**
     * The target jdk version.
     */
    @Parameter(property = "targetVersion", readonly = true, defaultValue = "2147483647")
    private int targetVersion;

    private final List<NodeJreVersion> nodeJreVersions = new ArrayList<NodeJreVersion>();
    private File localRepositoryDir;

    public void execute() throws MojoExecutionException {
        Logging.setLog(getLog());
        JvcInvoker invoker = new JvcInvoker(project);
        localRepositoryDir = repoSession.getLocalRepository().getBasedir();
        try {
            invoker.invoke("dependency:copy-dependencies");
            String dependencyTreeCmd = "dependency:tree -DoutputFile=" + Constant.DEPENDENCY_TREE_FILE + " -DoutputType=text -Dscope=compile";
            if (includes != null && includes.length() > 0) {
                dependencyTreeCmd += " -Dincludes=" + includes;
            }
            if (excludes != null && excludes.length() > 0) {
                dependencyTreeCmd += " -Dexcludes=" + excludes;
            }
            invoker.invoke(dependencyTreeCmd);
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        File jvcDependencyTreeFile = new File(project.getBasedir().getAbsolutePath() + "/" + Constant.DEPENDENCY_TREE_FILE);
        Node rootNode;
        try {
            FileInputStream fis;
            fis = new FileInputStream(jvcDependencyTreeFile);
            Reader reader = new BufferedReader(new InputStreamReader(fis));
            InputType type = InputType.TEXT;
            Parser parser = type.newParser();
            rootNode = parser.parse(reader);
            if (rootNode == null) {
                throw new MojoExecutionException("dependency-tree output empty");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("parse dependency-tree error:" + e.getMessage());
        }
        if (!localRepositoryDir.exists()) {
            throw new MojoExecutionException("local repository dir:" + localRepositoryDir.getAbsolutePath() + " not exist");
        }
        int minJreVersion = getJreVersion(rootNode.getChildNodes(), true);
        getLog().info("-----------------------------------------------");
        StringBuilder errorSb = new StringBuilder("\ntarget jre version:" + targetVersion + ", has some dependency may not build for current jdk version:\n");
        boolean hasError = false;
        Writer writer = null;
        try {
            writer = new FileWriter(jvcDependencyTreeFile, true);
            writer.write("\n");
            // 输出依赖对应的jre版本
            for (NodeJreVersion nodeJreVersion : nodeJreVersions) {
                Node node = nodeJreVersion.getNode();
                int jreVersion = nodeJreVersion.getJreVersion();
                jreVersion -= Constant.VERSION_DIF_WITH_BYTE_CODE;
                String info = node.getGroupId() + ":"
                        + node.getArtifactId() + ":"
                        + node.getVersion() + "----"
                        + (Constant.UNKNOWN_JRE_VERSION == jreVersion ? "unknown jre version" : "jre " + jreVersion);
                writer.write(info + "\n");
                getLog().info(info);
                if (jreVersion > targetVersion && Constant.UNKNOWN_JRE_VERSION != jreVersion) {
                    hasError = true;
                    errorSb.append(node.getGroupId()).append(":")
                            .append(node.getArtifactId()).append(":")
                            .append(node.getVersion()).append("----")
                            .append("jre ").append(jreVersion)
                            .append("\n");
                }
            }
        } catch (IOException e) {
            getLog().error("can't open jvc-dependency-tree");
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    getLog().error("can't close jvc-dependency-tree");
                }
            }
        }
        getLog().info("-----------------------------------------------");
        getLog().info("you need build project with jdk " + (minJreVersion - Constant.VERSION_DIF_WITH_BYTE_CODE));
        if (hasError) {
            throw new MojoExecutionException(errorSb.toString());
        }
    }

    private int getJreVersion(List<Node> nodeList, boolean firstTime) {
        int jreVersion = Integer.MIN_VALUE;
        for (Node node : nodeList) {
            int tmp = Integer.MAX_VALUE;
            // 构建jar路径
            String filePath = localRepositoryDir.getAbsolutePath() + "/"
                    + node.getGroupId().replaceAll("\\.", "/") + "/"
                    + node.getArtifactId().replaceAll("\\.", "/") + "/"
                    + node.getVersion() + "/";
            filePath += node.getArtifactId() + "-" + node.getVersion() + ".jar";
            JarParse jarParse = null;
            try {
                jarParse = new JarParse(filePath);
            } catch (MalformedURLException e) {
                // jar文件不存在
                getLog().warn("jar file:" + filePath + " not found");
            }
            if (jarParse != null) {
                try {
                    tmp = jarParse.parseJreVersion();
                } catch (Exception e) {
                    getLog().warn("parse jar(" + filePath + ") fail");
                }
                if (Integer.MAX_VALUE != tmp) {
                    if (firstTime) {
                        nodeJreVersions.add(new NodeJreVersion(node, tmp));
                    }
                    jreVersion = Math.max(jreVersion, tmp);
                } else {
                    // jar文件无法解析出对应的jre版本
                    getLog().warn("jar file:" + filePath + " can't parse the jre version, get the jre version from dependencies");
                    tmp = getJreVersionFromDependencies(node);
                    if (Integer.MAX_VALUE != tmp) {
                        if (firstTime) {
                            nodeJreVersions.add(new NodeJreVersion(node, tmp));
                        }
                        jreVersion = Math.max(jreVersion, tmp);
                    }
                }
            } else {
                getLog().info("jar not found, get the jre version from dependencies");
                tmp = getJreVersionFromDependencies(node);
                if (Integer.MAX_VALUE != tmp) {
                    if (firstTime) {
                        nodeJreVersions.add(new NodeJreVersion(node, tmp));
                    }
                    jreVersion = Math.max(jreVersion, tmp);
                }
            }
        }
        return jreVersion;
    }

    private int getJreVersionFromDependencies(Node node) {
        List<Node> childNodes = node.getChildNodes();
        if (childNodes == null || childNodes.isEmpty()) {
            getLog().warn("no found dependencies, can't parse the jre version");
        } else {
            int tmp = getJreVersion(childNodes, false);
            if (Integer.MAX_VALUE != tmp) {
                return tmp;
            } else {
                getLog().warn("can't parse the jre version from dependencies");
            }
        }
        return Integer.MAX_VALUE;
    }
}
