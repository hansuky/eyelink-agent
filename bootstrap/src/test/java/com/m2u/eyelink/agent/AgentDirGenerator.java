package com.m2u.eyelink.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.m2u.eyelink.common.Version;

public class AgentDirGenerator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private static final String bootStrapJar = "pinpoint-bootstrap-" + Version.VERSION + ".jar";

    private static final String commons = "pinpoint-commons-" + Version.VERSION + ".jar";
    private static final String bootStrapCoreJar = "pinpoint-bootstrap-core-" + Version.VERSION + ".jar";
    private static final String bootStrapCoreOptionalJar = "pinpoint-bootstrap-core-optional-" + Version.VERSION + ".jar";
    private static final String annotations = "pinpoint-annotations-" + Version.VERSION + ".jar";

    private final String agentDirPath;

    public AgentDirGenerator(String agentDirPath) {
        if (agentDirPath == null) {
            throw new NullPointerException("agentDirPath must not be null");
        }
        this.agentDirPath = agentDirPath;
    }

    public void create() throws IOException {

        final File agentDir = createDir(agentDirPath);

        // create dummy bootstrap
        createJarFile(agentDir, bootStrapJar);

        File boot = createChildDir(agentDir, "boot");

        createJarFile(boot, commons);
        createJarFile(boot, bootStrapCoreJar);
        createJarFile(boot, bootStrapCoreOptionalJar);
        createJarFile(boot, annotations);
    }

    private File createChildDir(File agentDir, String childDir) {
        String childDirPath = agentDir.getAbsolutePath() + File.separator + childDir;
        return createDir(childDirPath);
    }

    private File createDir(String dirPath) {
        logger.debug("create dir:{}", dirPath);

        final File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean mkdir = dir.mkdirs();
            Assert.assertTrue(dir + " create fail", mkdir);
        }
        Assert.assertTrue(dirPath + " not a directory", dir.isDirectory());

        Assert.assertTrue(dir.canWrite());

        return dir;
    }


    private void createFile(File parentDir, String filepath) throws IOException {
        logger.debug("create file : {}/{}",  parentDir, filepath);

        final File file = new File(parentDir, filepath);
        boolean newFile = file.createNewFile();
        Assert.assertTrue(filepath + " create fail", newFile);


    }

    private void createJarFile(File parentDir, String filepath) throws IOException {
        final String jarPath = parentDir.getPath() + File.separator + filepath;
        logger.debug("create jar:{}", jarPath);

        JarOutputStream jos = null;
        try {
            Manifest manifest = new Manifest();
            FileOutputStream out = new FileOutputStream(jarPath);
            jos = new JarOutputStream(out, manifest);
        } finally {
            IOUtils.closeQuietly(jos);
        }

    }

    public void remove() throws IOException {
        File file = new File(agentDirPath);
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            logger.warn("unable to deleteDirectory. path:{}", file.getPath(), e);
            // Boot directory is not deleted.
            // Perhaps JarFile is not closed.
            FileUtils.forceDeleteOnExit(file);
        }
    }

}