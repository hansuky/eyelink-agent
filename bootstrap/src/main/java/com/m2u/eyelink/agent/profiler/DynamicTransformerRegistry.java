package com.m2u.eyelink.agent.profiler;

import java.lang.instrument.ClassFileTransformer;

import com.m2u.eyelink.agent.interceptor.DynamicTransformRequestListener;

public interface DynamicTransformerRegistry extends DynamicTransformRequestListener {
	ClassFileTransformer getTransformer(ClassLoader classLoader, String targetClassName);
}
