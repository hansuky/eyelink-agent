package com.m2u.eyelink.agent.profiler.instrument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.m2u.eyelink.agent.instrument.ClassFilter;
import com.m2u.eyelink.agent.instrument.InstrumentClass;
import com.m2u.eyelink.agent.instrument.InstrumentContext;
import com.m2u.eyelink.agent.instrument.InstrumentException;
import com.m2u.eyelink.agent.instrument.InstrumentMethod;
import com.m2u.eyelink.agent.instrument.MethodFilter;
import com.m2u.eyelink.agent.instrument.MethodFilters;
import com.m2u.eyelink.agent.instrument.NotFoundInstrumentException;
import com.m2u.eyelink.agent.interceptor.annotation.TargetConstructor;
import com.m2u.eyelink.agent.interceptor.annotation.TargetConstructors;
import com.m2u.eyelink.agent.interceptor.annotation.TargetFilter;
import com.m2u.eyelink.agent.interceptor.annotation.TargetMethod;
import com.m2u.eyelink.agent.interceptor.annotation.TargetMethods;
import com.m2u.eyelink.agent.interceptor.scope.ExecutionPolicy;
import com.m2u.eyelink.agent.interceptor.scope.InterceptorScope;
import com.m2u.eyelink.agent.plugin.ObjectFactory;
import com.m2u.eyelink.agent.profiler.JavaAssistUtils;
import com.m2u.eyelink.agent.profiler.instrument.AccessorAnalyzer.AccessorDetails;
import com.m2u.eyelink.agent.profiler.instrument.GetterAnalyzer.GetterDetails;
import com.m2u.eyelink.agent.profiler.instrument.SetterAnalyzer.SetterDetails;
import com.m2u.eyelink.agent.profiler.instrument.aspect.AspectWeaverClass;
import com.m2u.eyelink.agent.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.m2u.eyelink.agent.profiler.metadata.ApiMetaDataService;
import com.m2u.eyelink.agent.profiler.objectfactory.AutoBindingObjectFactory;
import com.m2u.eyelink.agent.profiler.objectfactory.InterceptorArgumentProvider;
import com.m2u.eyelink.agent.profiler.objectfactory.ObjectBinderFactory;
import com.m2u.eyelink.common.util.Asserts;
import com.m2u.eyelink.exception.ELAgentException;

public class JavassistClass implements InstrumentClass {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final ObjectBinderFactory objectBinderFactory;
    private final InstrumentContext pluginContext;

    private final InterceptorRegistryBinder interceptorRegistryBinder;
    private final ApiMetaDataService apiMetaDataService;
    private final ClassLoader classLoader;

    private final CtClass ctClass;
    private static final String FIELD_PREFIX = "_$PINPOINT$_";
    private static final String SETTER_PREFIX = "_$PINPOINT$_set";
    private static final String GETTER_PREFIX = "_$PINPOINT$_get";


    public JavassistClass(ObjectBinderFactory objectBinderFactory, InstrumentContext pluginContext, InterceptorRegistryBinder interceptorRegistryBinder, ApiMetaDataService apiMetaDataService, ClassLoader classLoader, CtClass ctClass) {
        if (objectBinderFactory == null) {
            throw new NullPointerException("objectBinderFactory must not be null");
        }
        if (pluginContext == null) {
            throw new NullPointerException("pluginContext must not be null");
        }
        if (apiMetaDataService == null) {
            throw new NullPointerException("apiMetaDataService must not be null");
        }
        this.objectBinderFactory = objectBinderFactory;
        this.pluginContext = pluginContext;
        this.ctClass = ctClass;
        this.interceptorRegistryBinder = interceptorRegistryBinder;
        this.apiMetaDataService = apiMetaDataService;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public boolean isInterceptable() {
        return !ctClass.isInterface() && !ctClass.isAnnotation() && !ctClass.isModified();
    }

    @Override
    public boolean isInterface() {
        return this.ctClass.isInterface();
    }

    @Override
    public String getName() {
        return this.ctClass.getName();
    }

    @Override
    public String getSuperClass() {
        return this.ctClass.getClassFile2().getSuperclass();
    }

    @Override
    public String[] getInterfaces() {
        return this.ctClass.getClassFile2().getInterfaces();
    }

    private static CtMethod getCtMethod0(CtClass ctClass, String methodName, String[] parameterTypes) {
        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            final String descriptor = method.getMethodInfo2().getDescriptor();
            if (descriptor.startsWith(jvmSignature)) {
                return method;
            }
        }

        return null;
    }

    @Override
    public InstrumentMethod getDeclaredMethod(String name, String... parameterTypes) {
        CtMethod method = getCtMethod0(ctClass, name, parameterTypes);
        if (method == null) {
            return null;
        }
        return new JavassistMethod(objectBinderFactory, pluginContext, interceptorRegistryBinder, apiMetaDataService, this, method);
    }

    @Override
    public List<InstrumentMethod> getDeclaredMethods() {
        return getDeclaredMethods(MethodFilters.ACCEPT_ALL);
    }

    @Override
    public List<InstrumentMethod> getDeclaredMethods(MethodFilter methodFilter) {
        if (methodFilter == null) {
            throw new NullPointerException("methodFilter must not be null");
        }
        final CtMethod[] declaredMethod = ctClass.getDeclaredMethods();
        final List<InstrumentMethod> candidateList = new ArrayList<InstrumentMethod>(declaredMethod.length);
        for (CtMethod ctMethod : declaredMethod) {
            final InstrumentMethod method = new JavassistMethod(objectBinderFactory, pluginContext, interceptorRegistryBinder, apiMetaDataService, this, ctMethod);
            if (methodFilter.accept(method)) {
                candidateList.add(method);
            }
        }

        return candidateList;
    }

    private CtConstructor getCtConstructor0(String[] parameterTypes) {
        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        // constructor return type is void
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            final String descriptor = constructor.getMethodInfo2().getDescriptor();
            // skip return type check
            if (descriptor.startsWith(jvmSignature) && constructor.isConstructor()) {
                return constructor;
            }
        }

        return null;
    }

    @Override
    public InstrumentMethod getConstructor(String... parameterTypes) {
        CtConstructor constructor = getCtConstructor0(parameterTypes);
        if (constructor == null) {
            return null;
        }

        return new JavassistMethod(objectBinderFactory, pluginContext, interceptorRegistryBinder, apiMetaDataService, this, constructor);
    }

    @Override
    public boolean hasDeclaredMethod(String methodName, String... args) {
        return getCtMethod0(ctClass, methodName, args) != null;
    }

    @Override
    public boolean hasMethod(String methodName, String... parameterTypes) {
        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);

        for (CtMethod method : ctClass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            final String descriptor = method.getMethodInfo2().getDescriptor();
            if (descriptor.startsWith(jvmSignature)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasEnclosingMethod(String methodName, String... parameterTypes) {
        CtBehavior behavior;
        // FIXME BSH
//        try {
//            behavior = ctClass.getEnclosingBehavior();
//        } catch (NotFoundException ignored) {
//            return false;
//        }

//        if(behavior == null) {
//            return false;
//        }
//
//        final MethodInfo methodInfo = behavior.getMethodInfo2();
//        if (!methodInfo.getName().equals(methodName)) {
//            return false;
//        }
//
//        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
//        if (methodInfo.getDescriptor().startsWith(jvmSignature)) {
//            return true;
//        }

        return false;
    }

    @Override
    public boolean hasConstructor(String... parameterTypeArray) {
        final String signature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypeArray, "void");
        try {
            CtConstructor c = ctClass.getConstructor(signature);
            return c != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean hasField(String name, String type) {
    	// FIXME BSH
//        try {
//            String vmType = null;
//            if (type != null) {
//                vmType = JavaAssistUtils.toJvmSignature(type);
//            }
//
//            ctClass.getField(name, vmType);
//        } catch (NotFoundException e) {
//            return false;
//        }

        return true;
    }

    @Override
    public boolean hasField(String name) {
        return hasField(name, null);
    }

    @Override
    public void weave(String adviceClassName) throws InstrumentException {
        pluginContext.injectClass(classLoader, adviceClassName);

        CtClass adviceClass;
        try {
            adviceClass = getCtClass(adviceClassName);
        } catch (NotFoundException e) {
            throw new NotFoundInstrumentException(adviceClassName + " not found. Caused:" + e.getMessage(), e);
        }
        try {
            AspectWeaverClass weaverClass = new AspectWeaverClass();
            weaverClass.weaving(ctClass, adviceClass);
        } catch (CannotCompileException e) {
            throw new InstrumentException("weaving fail. sourceClassName:" + ctClass.getName() + " adviceClassName:" + adviceClassName + " Caused:" + e.getMessage(), e);
        } catch (NotFoundException e) {
            throw new InstrumentException("weaving fail. sourceClassName:" + ctClass.getName() + " adviceClassName:" + adviceClassName + " Caused:" + e.getMessage(), e);
        }
    }

    @Override
    public InstrumentMethod addDelegatorMethod(String methodName, String... paramTypes) throws InstrumentException {
        if (getCtMethod0(ctClass, methodName, paramTypes) != null) {
            throw new InstrumentException(getName() + "already have method(" + methodName + ").");
        }

        try {
            final CtClass superClass = ctClass.getSuperclass();
            CtMethod superMethod = getCtMethod0(superClass, methodName, paramTypes);

            if (superMethod == null) {
                throw new NotFoundInstrumentException(methodName + Arrays.toString(paramTypes) + " is not found in " + superClass.getName());
            }

            CtMethod delegatorMethod = CtNewMethod.delegator(superMethod, ctClass);
            ctClass.addMethod(delegatorMethod);

            return new JavassistMethod(objectBinderFactory, pluginContext, interceptorRegistryBinder, apiMetaDataService, this, delegatorMethod);
        } catch (NotFoundException ex) {
            throw new InstrumentException(getName() + "don't have super class(" + getSuperClass() + "). Cause:" + ex.getMessage(), ex);
        } catch (CannotCompileException ex) {
            throw new InstrumentException(methodName + " addDelegatorMethod fail. Cause:" + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] toBytecode() {
        try {
            byte[] bytes = ctClass.toBytecode();
            ctClass.detach();
            return bytes;
        } catch (IOException e) {
            logger.info("IoException class:{} Caused:{}", ctClass.getName(), e.getMessage(), e);
        } catch (CannotCompileException e) {
            logger.info("CannotCompileException class:{} Caused:{}", ctClass.getName(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void addField(String accessorTypeName) throws InstrumentException {
        addField0(accessorTypeName, null);
    }

    private void addField0(String accessorTypeName, String initValExp) throws InstrumentException {
        try {
            Class<?> accessorType = pluginContext.injectClass(classLoader, accessorTypeName);
            final AccessorAnalyzer accessorAnalyzer = new AccessorAnalyzer();
            final AccessorDetails accessorDetails = accessorAnalyzer.analyze(accessorType);

            Class<?> fieldType = accessorDetails.getFieldType();
            String fieldTypeName = JavaAssistUtils.javaClassNameToObjectName(fieldType.getName());

            final CtField newField = CtField.make("private " + fieldTypeName + " " + FIELD_PREFIX + JavaAssistUtils.javaClassNameToVariableName(accessorTypeName) + ";", ctClass);

            if (initValExp == null) {
                ctClass.addField(newField);
            } else {
                ctClass.addField(newField, initValExp);
            }
            final CtClass accessorInterface = getCtClass(accessorTypeName);
            ctClass.addInterface(accessorInterface);

            CtMethod getterMethod = CtNewMethod.getter(accessorDetails.getGetter().getName(), newField);
            ctClass.addMethod(getterMethod);

            CtMethod setterMethod = CtNewMethod.setter(accessorDetails.getSetter().getName(), newField);
            ctClass.addMethod(setterMethod);
        } catch (Exception e) {
            throw new InstrumentException("Failed to add field with accessor [" + accessorTypeName + "]. Cause:" + e.getMessage(), e);
        }
    }

    @Override
    public void addGetter(String getterTypeName, String fieldName) throws InstrumentException {
        try {

            Class<?> getterType = pluginContext.injectClass(classLoader, getterTypeName);
            GetterAnalyzer getterAnalyzer = new GetterAnalyzer();
            GetterDetails getterDetails = getterAnalyzer.analyze(getterType);

            CtField field = ctClass.getField(fieldName);
            String fieldTypeName = JavaAssistUtils.javaClassNameToObjectName(getterDetails.getFieldType().getName());

            if (!field.getType().getName().equals(fieldTypeName)) {
                throw new IllegalArgumentException("Return type of the getter is different with the field type. getterMethod: " + getterDetails.getGetter() + ", fieldType: " + field.getType().getName());
            }

            CtMethod getterMethod = CtNewMethod.getter(getterDetails.getGetter().getName(), field);

            if (getterMethod.getDeclaringClass() != ctClass) {
                getterMethod = CtNewMethod.copy(getterMethod, ctClass, null);
            }

            ctClass.addMethod(getterMethod);

            CtClass ctInterface = getCtClass(getterTypeName);
            ctClass.addInterface(ctInterface);
        } catch (Exception e) {
            throw new InstrumentException("Failed to add getter: " + getterTypeName, e);
        }
    }

    private CtClass getCtClass(String className) throws NotFoundException {
        final ClassPool classPool = ctClass.getClassPool();
        return classPool.get(className);
    }

    @Override
    public void addSetter(String setterTypeName, String fieldName) throws InstrumentException {
        this.addSetter(setterTypeName, fieldName, false);
    }

    @Override
    public void addSetter(String setterTypeName, String fieldName, boolean removeFinalFlag) throws InstrumentException {
        try {
            Class<?> setterType = pluginContext.injectClass(classLoader, setterTypeName);

            SetterAnalyzer setterAnalyzer = new SetterAnalyzer();
            SetterDetails setterDetails = setterAnalyzer.analyze(setterType);

            CtField field = ctClass.getField(fieldName);
            String fieldTypeName = JavaAssistUtils.javaClassNameToObjectName(setterDetails.getFieldType().getName());

            if (!field.getType().getName().equals(fieldTypeName)) {
                throw new IllegalArgumentException("Argument type of the setter is different with the field type. setterMethod: " + setterDetails.getSetter() + ", fieldType: " + field.getType().getName());
            }

            final int originalModifiers = field.getModifiers();
            if (Modifier.isStatic(originalModifiers)) {
                throw new IllegalArgumentException("Cannot add setter to static fields. setterMethod: " + setterDetails.getSetter().getName() + ", fieldName: " + fieldName);
            }

            boolean finalRemoved = false;
            if (Modifier.isFinal(originalModifiers)) {
                if (!removeFinalFlag) {
                    throw new IllegalArgumentException("Cannot add setter to final field. setterMethod: " + setterDetails.getSetter().getName() + ", fieldName: " + fieldName);
                } else {
                    final int modifiersWithFinalRemoved = Modifier.clear(originalModifiers, Modifier.FINAL);
                    field.setModifiers(modifiersWithFinalRemoved);
                    finalRemoved = true;
                }
            }

            try {
                CtMethod setterMethod = CtNewMethod.setter(setterDetails.getSetter().getName(), field);
                if (setterMethod.getDeclaringClass() != ctClass) {
                    setterMethod = CtNewMethod.copy(setterMethod, ctClass, null);
                }
                ctClass.addMethod(setterMethod);

                CtClass ctInterface = getCtClass(setterTypeName);
                ctClass.addInterface(ctInterface);
            }
            catch (Exception e) {
                if (finalRemoved) {
                    field.setModifiers(originalModifiers);
                }
                throw e;
            }
        } catch (Exception e) {
            throw new InstrumentException("Failed to add setter: " + setterTypeName, e);
        }

    }

    @Override
    public int addInterceptor(String interceptorClassName) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        return addInterceptor0(interceptorClassName, null, null, null);
    }
    @Override
    public int addInterceptor(String interceptorClassName, Object[] constructorArgs) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        return addInterceptor0(interceptorClassName, constructorArgs, null, null);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, String scopeName) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scopeName, "scopeName");
        final InterceptorScope interceptorScope = pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, null, interceptorScope, ExecutionPolicy.BOUNDARY);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, InterceptorScope scope) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scope, "scope");
        return addInterceptor0(interceptorClassName, null, scope, ExecutionPolicy.BOUNDARY);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, String scopeName) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scopeName, "scopeName");
        final InterceptorScope interceptorScope = pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, constructorArgs, interceptorScope, ExecutionPolicy.BOUNDARY);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scope, "scope");
        return addInterceptor0(interceptorClassName, constructorArgs, scope, ExecutionPolicy.BOUNDARY);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, String scopeName, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scopeName, "scopeName");
        Asserts.notNull(executionPolicy, "executionPolicy");
        final InterceptorScope interceptorScope = pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, null, interceptorScope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scope, "scope");
        Asserts.notNull(executionPolicy, "executionPolicy");
        return addInterceptor0(interceptorClassName, null, scope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, String scopeName, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scopeName, "scopeName");
        Asserts.notNull(executionPolicy, "executionPolicy");
        final InterceptorScope interceptorScope = pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, constructorArgs, interceptorScope, executionPolicy);
    }


    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scope, "scope");
        Asserts.notNull(executionPolicy, "executionPolicy");
        return addInterceptor0(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addInterceptor0(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {

        int interceptorId = -1;
        final Class<?> interceptorType = pluginContext.injectClass(classLoader, interceptorClassName);


        final TargetMethods targetMethods = interceptorType.getAnnotation(TargetMethods.class);
        if (targetMethods != null) {
            for (TargetMethod m : targetMethods.value()) {
                interceptorId = addInterceptor0(m, interceptorClassName, constructorArgs, scope, executionPolicy);
            }
        }

        final TargetMethod targetMethod = interceptorType.getAnnotation(TargetMethod.class);
        if (targetMethod != null) {
            interceptorId = addInterceptor0(targetMethod, interceptorClassName, constructorArgs, scope, executionPolicy);
        }

        final TargetConstructors targetConstructors = interceptorType.getAnnotation(TargetConstructors.class);
        if (targetConstructors != null) {
            for (TargetConstructor c : targetConstructors.value()) {
                interceptorId = addInterceptor0(c, interceptorClassName, scope, executionPolicy, constructorArgs);
            }
        }

        final TargetConstructor targetConstructor = interceptorType.getAnnotation(TargetConstructor.class);
        if (targetConstructor != null) {
            interceptorId = addInterceptor0(targetConstructor, interceptorClassName, scope, executionPolicy, constructorArgs);
        }

        final TargetFilter targetFilter = interceptorType.getAnnotation(TargetFilter.class);
        if (targetFilter != null) {
            interceptorId = addInterceptor0(targetFilter, interceptorClassName, scope, executionPolicy, constructorArgs);
        }

        if (interceptorId == -1) {
            throw new ELAgentException("No target is specified. At least one of @Targets, @TargetMethod, @TargetConstructor, @TargetFilter must present. interceptor: " + interceptorClassName);
        }

        return interceptorId;
    }

    private int addInterceptor0(TargetConstructor c, String interceptorClassName, InterceptorScope scope, ExecutionPolicy executionPolicy, Object... constructorArgs) throws InstrumentException {
        final InstrumentMethod constructor = getConstructor(c.value());

        if (constructor == null) {
            throw new NotFoundInstrumentException("Cannot find constructor with parameter types: " + Arrays.toString(c.value()));
        }
        // TODO casting fix
        return ((JavassistMethod)constructor).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addInterceptor0(TargetMethod m, String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        InstrumentMethod method = getDeclaredMethod(m.name(), m.paramTypes());

        if (method == null) {
            throw new NotFoundInstrumentException("Cannot find method " + m.name() + " with parameter types: " + Arrays.toString(m.paramTypes()));
        }
        // TODO casting fix
        return ((JavassistMethod)method).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addInterceptor0(TargetFilter annotation, String interceptorClassName, InterceptorScope scope, ExecutionPolicy executionPolicy, Object[] constructorArgs) throws InstrumentException {
        String filterTypeName = annotation.type();
        Asserts.notNull(filterTypeName, "type of @TargetFilter");

        InterceptorArgumentProvider interceptorArgumentProvider = this.objectBinderFactory.newInterceptorArgumentProvider(this);
        AutoBindingObjectFactory filterFactory = this.objectBinderFactory.newAutoBindingObjectFactory(pluginContext, classLoader, interceptorArgumentProvider);
        final ObjectFactory objectFactory = ObjectFactory.byConstructor(filterTypeName, (Object[]) annotation.constructorArguments());
        MethodFilter filter = (MethodFilter) filterFactory.createInstance(objectFactory);

        boolean singleton = annotation.singleton();
        int interceptorId = -1;

        for (InstrumentMethod m : getDeclaredMethods(filter)) {
            if (singleton && interceptorId != -1) {
                m.addInterceptor(interceptorId);
            } else {
                // TODO casting fix
                interceptorId = ((JavassistMethod)m).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
            }
        }

        if (interceptorId == -1) {
            logger.warn("No methods are intercepted. target: " + ctClass.getName(), ", interceptor: " + interceptorClassName + ", methodFilter: " + filterTypeName);
        }

        return interceptorId;
    }

    @Override
    public int addInterceptor(MethodFilter filter, String interceptorClassName) throws InstrumentException {
        Asserts.notNull(filter, "filter");
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        return addScopedInterceptor0(filter, interceptorClassName, null, null, null);
    }

    @Override
    public int addInterceptor(MethodFilter filter, String interceptorClassName, Object[] constructorArgs) throws InstrumentException {
        Asserts.notNull(filter, "filter");
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        return addScopedInterceptor0(filter, interceptorClassName, constructorArgs, null, null);
    }

    @Override
    public int addScopedInterceptor(MethodFilter filter, String interceptorClassName, String scopeName, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(filter, "filter");
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scopeName, "scopeName");
        Asserts.notNull(executionPolicy, "executionPolicy");
        final InterceptorScope interceptorScope = pluginContext.getInterceptorScope(scopeName);
        return addScopedInterceptor0(filter, interceptorClassName, null, interceptorScope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(MethodFilter filter, String interceptorClassName, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(filter, "filter");
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scope, "scope");
        Asserts.notNull(executionPolicy, "executionPolicy");
        return addScopedInterceptor0(filter, interceptorClassName, null, scope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(MethodFilter filter, String interceptorClassName, Object[] constructorArgs, String scopeName, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(filter, "filter");
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scopeName, "scopeName");
        Asserts.notNull(executionPolicy, "executionPolicy");
        final InterceptorScope interceptorScope = pluginContext.getInterceptorScope(scopeName);
        return addScopedInterceptor0(filter, interceptorClassName, null, interceptorScope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(MethodFilter filter, String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(filter, "filter");
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scope, "scope");
        Asserts.notNull(executionPolicy, "executionPolicy");
        return addScopedInterceptor0(filter, interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addScopedInterceptor0(MethodFilter filter, String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        int interceptorId = -1;

        for (InstrumentMethod m : getDeclaredMethods(filter)) {
            if (interceptorId != -1) {
                m.addInterceptor(interceptorId);
            } else {
                // TODO casting fix
                interceptorId = ((JavassistMethod)m).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
            }
        }

        if (interceptorId == -1) {
            logger.warn("No methods are intercepted. target: " + ctClass.getName(), ", interceptor: " + interceptorClassName + ", methodFilter: " + filter.getClass().getName());
        }

        return interceptorId;
    }

    @Override
    public List<InstrumentClass> getNestedClasses(ClassFilter filter) {
        List<InstrumentClass> list = new ArrayList<InstrumentClass>();
        CtClass[] nestedClasses;
        try {
            nestedClasses = ctClass.getNestedClasses();
        } catch (NotFoundException ex) {
            return list;
        }

        if (nestedClasses == null || nestedClasses.length == 0) {
            return list;
        }

        for (CtClass nested : nestedClasses) {
            final InstrumentClass clazz = new JavassistClass(objectBinderFactory, pluginContext, interceptorRegistryBinder, apiMetaDataService, classLoader, nested);
            if (filter.accept(clazz)) {
                list.add(clazz);
            }
        }

        return list;
    }

}
