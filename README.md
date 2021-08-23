# jvc-maven-plugin
用于检查需要在哪个jdk版本下编译项目的Maven插件

当前支持两个命令：
#### check-jre-version
检查pom.xml中使用的依赖信息，输出每个依赖使用的编译版本，同时输出编译项目的推荐jdk版本，该命令会同步生成jvc-dependency-tree文件，包含依赖树及依赖使用的编译版本
参数：
- targetVersion：待使用的目标版本，检查依赖使用的编译版本是否小于等于给定版本
- includes：检查哪些依赖，格式为[groupId]:[artifactId]:[type]:[version]，支持*通配符
- excludes：排除哪些依赖，格式为[groupId]:[artifactId]:[type]:[version]，支持*通配符
#### clean
清除生成的jvc-dependency-tree文件
