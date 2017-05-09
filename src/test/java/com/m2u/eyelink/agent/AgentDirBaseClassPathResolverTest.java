package com.m2u.eyelink.agent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.m2u.eyelink.common.Version;

public class AgentDirBaseClassPathResolverTest {

    private static final Logger logger = LoggerFactory.getLogger(AgentDirBaseClassPathResolverTest.class);

    private static final String BOOTSTRAP_JAR = "pinpoint-bootstrap-" + Version.VERSION + ".jar";
    private static final String TEST_AGENT_DIR = "testagent";
    private static final String SEPARATOR = File.separator;

    private static final AtomicInteger AGENT_ID_ALLOCATOR = new AtomicInteger();

    private static String agentBuildDir;
    private static String agentBootstrapPath;

    private static AgentDirGenerator agentDirGenerator;

    @BeforeClass
    public static void beforeClass() throws Exception {

        String classLocation = getClassLocation(AgentDirBaseClassPathResolverTest.class);
        logger.debug("buildDir:{}", classLocation);

        agentBuildDir = classLocation + SEPARATOR + TEST_AGENT_DIR + '_' + AGENT_ID_ALLOCATOR.incrementAndGet();

        logger.debug("agentBuildDir:{}", agentBuildDir);

        agentBootstrapPath = agentBuildDir + SEPARATOR + BOOTSTRAP_JAR;

        logger.debug("agentBootstrapPath:{}", agentBootstrapPath);

        createAgentDir(agentBuildDir);


    }

    private static void createAgentDir(String tempAgentDir) throws IOException {

        agentDirGenerator = new AgentDirGenerator(tempAgentDir);
        agentDirGenerator.create();

    }


    @AfterClass
    public static void afterClass() throws Exception {
        if (agentDirGenerator != null) {
            agentDirGenerator.remove();
        }
    }

    @Test
    public void testFindAgentJar() throws Exception {

        logger.debug("TEST_AGENT_DIR:{}", agentBuildDir);
        logger.debug("agentBootstrapPath:{}", agentBootstrapPath);

        AgentDirBaseClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver(agentBootstrapPath);
        Assert.assertTrue("verify agent directory ", classPathResolver.verify());

        boolean findAgentJar = classPathResolver.findAgentJar();
        Assert.assertTrue(findAgentJar);

        String agentJar = classPathResolver.getAgentJarName();
        Assert.assertEquals(BOOTSTRAP_JAR, agentJar);

        String agentPath = classPathResolver.getAgentJarFullPath();
        Assert.assertEquals(agentBootstrapPath, agentPath);

        String agentDirPath = classPathResolver.getAgentDirPath();
        Assert.assertEquals(agentBuildDir, agentDirPath);

        String agentLibPath = classPathResolver.getAgentLibPath();
        Assert.assertEquals(agentBuildDir + File.separator + "lib", agentLibPath);

        ELAgentJarFile bootstrapJarFile = classPathResolver.getELAgentJarFile();
        closeJarFile(bootstrapJarFile);

    }

    private void closeJarFile(ELAgentJarFile bootstrapJarFile) {
        final List<JarFile> jarFileList = bootstrapJarFile.getJarFileList();
        for (JarFile jarFile : jarFileList) {
            try {
                jarFile.close();
            } catch (IOException e) {
                logger.debug(jarFile + " delete fail", e);
            }
        }
    }

    private static String getClassLocation(Class<?> clazz) {
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        URL location = codeSource.getLocation();
        logger.debug("codeSource.getLocation:{}", location);
        File file = FileUtils.toFile(location);
        return file.getPath();
    }


    @Test
    public void findAgentJar() {
        logger.debug("agentBuildDir:{}", agentBuildDir);
        logger.debug("agentBootstrapPath:{}", agentBootstrapPath);

        findAgentJar(agentBootstrapPath);


        findAgentJarAssertFail(agentBuildDir + File.separator + "pinpoint-bootstrap-unknown.jar");
    }

    private void findAgentJar(String path) {
        AgentDirBaseClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver(path);
        boolean agentJar = classPathResolver.findAgentJar();
        Assert.assertTrue(agentJar);
    }

    private void findAgentJarAssertFail(String path) {
        AgentDirBaseClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver(path);
        boolean agentJar = classPathResolver.findAgentJar();
        Assert.assertFalse(agentJar);
    }

}

