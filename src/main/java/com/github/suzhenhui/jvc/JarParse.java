package com.github.suzhenhui.jvc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarParse {
    private final File file;

    public JarParse(File file) {
        this.file = file;
    }

    public JarParse(String filePath) throws MalformedURLException {
        this.file = new File(filePath);
    }

    public int parseJreVersion() {
        ClassLoader cl;
        try {
            URL url = file.toURI().toURL();
            cl = new URLClassLoader(new URL[]{url});
        } catch (MalformedURLException e) {
            Logging.getLog().warn("create urlClassLoader error, use maven running classLoader");
            cl = this.getClass().getClassLoader();
        }
        return parseJreVersion(cl);
    }

    public int parseJreVersion(ClassLoader cl) {
        int jreVersion = Integer.MAX_VALUE;
        try {
            if (!file.exists() || !file.isFile()) {
                throw new RuntimeException("file(" + file.getAbsolutePath() + ") not found");
            }
            jreVersion = getJreVersion(new JarFile(file), cl);
        } catch (IOException e) {
            Logging.getLog().error(e.getMessage());
        }
        if (jreVersion < 44) {
            Logging.getLog().error("unknown version");
            return Integer.MAX_VALUE;
        }
        return jreVersion;
    }

    /**
     * 遍历jarFile中的class和jar，获取运行需要的jre版本
     * jarFile中存在class时，获取该class对应的编译版本，否则获取内部jar包的最大编译版本
     *
     * @param jarFile jarFile
     * @param cl      类加载器
     * @return jdk版本
     */
    public int getJreVersion(JarFile jarFile, ClassLoader cl) throws IOException {
        boolean existJdkVersion = false;
        int jreVersion = Integer.MAX_VALUE;
        if (jarFile == null || cl == null) {
            return jreVersion;
        }
        File file;
        List<JarFile> minorJars = new ArrayList<JarFile>();
        Enumeration<JarEntry> es = jarFile.entries();
        // 读取包内的class和jar，存在class时并解析出jdk版本时退出
        while (es.hasMoreElements()) {
            JarEntry jarEntry = es.nextElement();
            if (jarEntry.isDirectory()) {
                continue;
            }
            String fileName = jarEntry.getName();
            if (fileName.length() <= 0) {
                continue;
            }
            if (fileName.endsWith(".class")) {
                int jdkVersion4JarEntry = getJreVersionFromJarEntry(jarFile, jarEntry, cl);
                if (Integer.MAX_VALUE != jdkVersion4JarEntry) {
                    existJdkVersion = true;
                    jreVersion = jdkVersion4JarEntry;
                    break;
                }
            }
            if (fileName.endsWith(".jar")) {
                file = new File(fileName);
                minorJars.add(new JarFile(file));
            }
        }
        // 主jar包没有class，获取内部jar包的最大版本
        if (!existJdkVersion && !minorJars.isEmpty()) {
            int minorJdkVersion = Integer.MIN_VALUE;
            for (JarFile minorJar : minorJars) {
                int jdkVersion4minorJar = getJreVersion(minorJar, cl);
                if (Integer.MAX_VALUE != jdkVersion4minorJar) {
                    minorJdkVersion = Math.max(minorJdkVersion, jdkVersion4minorJar);
                }
            }
            jreVersion = Integer.MIN_VALUE == minorJdkVersion ? jreVersion : minorJdkVersion;
        }
        return jreVersion;
    }

    private int getJreVersionFromJarEntry(JarFile jarFile, JarEntry jarEntry, ClassLoader loader) {
        int jreVersion = Integer.MAX_VALUE;
        if (jarEntry == null) {
            return jreVersion;
        }
        String fileName = jarEntry.getName();
        if (!fileName.endsWith(".class")) {
            return jreVersion;
        }
        String className = fileName.replace("/", ".").substring(0, fileName.length() - 6);
        if (className.contains("$")) {
            // 不解析内部类
            return jreVersion;
        }
        Class<?> clazz = null;
        try {
            clazz = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Logging.getLog().warn("class:" + className + " not found");
        }
        if (clazz != null) {
            // class verity success
            File file = new File(fileName);
            InputStream is = null;
            try {
                is = jarFile.getInputStream(jarEntry);
                byte[] bytes = new byte[8];
                //noinspection ResultOfMethodCallIgnored
                is.read(bytes, 0, bytes.length);
                jreVersion = bytes[bytes.length - 1];
            } catch (Exception e) {
                throw new RuntimeException("read class's jreVersion(" + className + ") error");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return jreVersion;
    }
}
