package com.m2u.eyelink.agent.profiler.context.module;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

@BindingAnnotation
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface PluginJars {
}

