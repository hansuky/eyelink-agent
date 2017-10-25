package com.m2u.eyelink.agent.profiler.plugin;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;

import com.m2u.eyelink.agent.profiler.plugin.jdbc.JdbcUrlParserV2;

public interface PluginContextLoadResult {

    List<ClassFileTransformer> getClassFileTransformer();

    List<ApplicationTypeDetector> getApplicationTypeDetectorList();

    List<JdbcUrlParserV2> getJdbcUrlParserList();

}

