package com.m2u.eyelink.agent;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.m2u.eyelink.common.ELConstants;


public class ELAgentStarter {

	private final ELLogger logger = ELLogger.getLogger(this.getClass()
			.getName());

    public static final String AGENT_TYPE = "AGENT_TYPE";
    public static final String DEFAULT_AGENT = "DEFAULT_AGENT";
    public static final String BOOT_CLASS = "com.m2u.eyelink.agent.profiler.DefaultAgent";

    public static final String PLUGIN_TEST_AGENT = "PLUGIN_TEST";
    public static final String PLUGIN_TEST_BOOT_CLASS = "com.m2u.eyelink.agent.test.PluginTestAgent";

	private final Map<String, String> agentArgs;
	private final ELAgentJarFile agentJarFile;
	private final ClassPathResolver classPathResolver;
	private final Instrumentation instrumentation;

	public ELAgentStarter(Map<String, String> agentArgsMap,
			ELAgentJarFile agentJarFile, ClassPathResolver classPathResolver,
			Instrumentation inst) {
		if (agentArgsMap == null) {
			throw new NullPointerException("agentArgsMap must not be null");
		}
		if (agentJarFile == null) {
			throw new NullPointerException("ELAgentJarFile must not be null");
		}
		if (classPathResolver == null) {
			throw new NullPointerException("classPathResolver must not be null");
		}
		if (inst == null) {
			throw new NullPointerException("instrumentation must not be null");
		}
		this.agentArgs = agentArgsMap;
		this.agentJarFile = agentJarFile;
		this.classPathResolver = classPathResolver;
		this.instrumentation = inst;

	}

	boolean start() {
		// check eyelink.agentId, eyelink.applicationName setting in arguement
		final IdValidator idValidator = new IdValidator();
		final String agentId = idValidator.getAgentId();
		if (agentId == null) {
			return false;
		}
		final String applicationName = idValidator.getApplicationName();
		if (applicationName == null) {
			return false;
		}

		// loading Plugin jar files
		URL[] pluginJars = classPathResolver.resolvePlugins();

		String configPath = getConfigPath(classPathResolver);
		if (configPath == null) {
			return false;
		}

		// TODO skip for next time
		// set the path of log file as a system property
		// saveLogFilePath(classPathResolver);

		// savePinpointVersion();

		try {

			List<URL> libUrlList = resolveLib(classPathResolver);

//			logger.info("pinpoint agent [" + bootClass + "] starting...");

			logger.info("EyeLink agent started normally.");

		} catch (Exception e) {
			logger.warn(ELConstants.ProductName + " start failed.", e);
			return false;
		}
		return true;
	}

	private String getConfigPath(ClassPathResolver classPathResolver) {
		final String configName = ELConstants.ProductName + ".config";
		String agentConfigFormSystemProperty = System.getProperty(configName);
		if (agentConfigFormSystemProperty != null) {
			logger.info(configName + " systemProperty found. "
					+ agentConfigFormSystemProperty);
			return agentConfigFormSystemProperty;
		}

		String classPathAgentConfigPath = classPathResolver
				.getAgentConfigPath();
		if (classPathAgentConfigPath != null) {
			logger.info("classpath " + configName + " found. "
					+ classPathAgentConfigPath);
			return classPathAgentConfigPath;
		}

		logger.info(configName + " file not found.");
		return null;
	}


    private List<URL> resolveLib(ClassPathResolver classPathResolver) {
        // this method may handle only absolute path,  need to handle relative path (./..agentlib/lib)
        String agentJarFullPath = classPathResolver.getAgentJarFullPath();
        String agentLibPath = classPathResolver.getAgentLibPath();
        List<URL> urlList = resolveLib(classPathResolver.resolveLib());
        String agentConfigPath = classPathResolver.getAgentConfigPath();

        if (logger.isInfoEnabled()) {
            logger.info("agent JarPath:" + agentJarFullPath);
            logger.info("agent LibDir:" + agentLibPath);
            for (URL url : urlList) {
                logger.info("agent Lib:" + url);
            }
            logger.info("agent config:" + agentConfigPath);
        }

        return urlList;
    }
    
    private List<URL> resolveLib(List<URL> urlList) {
        if (DEFAULT_AGENT.equals(getAgentType().toUpperCase())) {
            final List<URL> releaseLib = new ArrayList<URL>(urlList.size());
            for (URL url : urlList) {
                //
                if (!url.toExternalForm().contains("eyelink-profiler-test")) {
                    releaseLib.add(url);
                }
            }
            return releaseLib;
        } else {
            logger.info("load " + PLUGIN_TEST_AGENT + " lib");
            // plugin test
            return urlList;
        }
    }

    private String getAgentType() {
        String agentType = agentArgs.get(AGENT_TYPE);
        if (agentType == null) {
            return DEFAULT_AGENT;
        }
        return agentType;

    }
}