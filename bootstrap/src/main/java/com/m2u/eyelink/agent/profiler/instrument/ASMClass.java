package com.m2u.eyelink.agent.profiler.instrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
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
import com.m2u.eyelink.agent.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.m2u.eyelink.agent.profiler.metadata.ApiMetaDataService;
import com.m2u.eyelink.agent.profiler.objectfactory.AutoBindingObjectFactory;
import com.m2u.eyelink.agent.profiler.objectfactory.InterceptorArgumentProvider;
import com.m2u.eyelink.agent.profiler.objectfactory.ObjectBinderFactory;
import com.m2u.eyelink.common.util.Asserts;
import com.m2u.eyelink.exception.ELAgentException;


public class ASMClass implements InstrumentClass {
    private static final String FIELD_PREFIX = "_$PINPOINT$_";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ObjectBinderFactory objectBinderFactory;
    private final InstrumentContext pluginContext;
    private final InterceptorRegistryBinder interceptorRegistryBinder;
    private final ApiMetaDataService apiMetaDataService;
    private final ClassLoader classLoader;

    private final ASMClassNodeAdapter classNode;
    private boolean modified = false;
    private String name;

    public ASMClass(ObjectBinderFactory objectBinderFactory, final InstrumentContext pluginContext, final InterceptorRegistryBinder interceptorRegistryBinder, ApiMetaDataService apiMetaDataService, final ClassLoader classLoader, final ClassNode classNode) {
        this(objectBinderFactory, pluginContext, interceptorRegistryBinder, apiMetaDataService, classLoader, new ASMClassNodeAdapter(pluginContext, classLoader, classNode));
    }

    public ASMClass(ObjectBinderFactory objectBinderFactory, final InstrumentContext pluginContext, final InterceptorRegistryBinder interceptorRegistryBinder, ApiMetaDataService apiMetaDataService, final ClassLoader classLoader, final ASMClassNodeAdapter classNode) {
        if (objectBinderFactory == null) {
            throw new NullPointerException("objectBinderFactory must not be null");
        }

//        if (pluginContext == null) {
//            throw new NullPointerException("pluginContext must not be null");
//        }
        if (apiMetaDataService == null) {
            throw new NullPointerException("apiMetaDataService must not be null");
        }

        this.objectBinderFactory = objectBinderFactory;
        this.pluginContext = pluginContext;
        this.interceptorRegistryBinder = interceptorRegistryBinder;
        this.apiMetaDataService = apiMetaDataService;
        this.classLoader = classLoader;
        this.classNode = classNode;
        // for performance.
        this.name = classNode.getName();
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public boolean isInterceptable() {
        return !isInterface() && !isAnnotation() && !isModified();
    }

    @Override
    public boolean isInterface() {
        return this.classNode.isInterface();
    }

    private boolean isAnnotation() {
        return this.classNode.isAnnotation();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSuperClass() {
        return this.classNode.getSuperClassName();
    }

    @Override
    public String[] getInterfaces() {
        return this.classNode.getInterfaceNames();
    }

    @Override
    public InstrumentMethod getDeclaredMethod(final String name, final String... parameterTypes) {
        final String desc = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        final ASMMethodNodeAdapter methodNode = this.classNode.getDeclaredMethod(name, desc);
        if (methodNode == null) {
            return null;
        }

        return new ASMMethod(this.objectBinderFactory, this.pluginContext, this.interceptorRegistryBinder, apiMetaDataService, this, methodNode);
    }

    @Override
    public List<InstrumentMethod> getDeclaredMethods() {
        return getDeclaredMethods(MethodFilters.ACCEPT_ALL);
    }

    @Override
    public List<InstrumentMethod> getDeclaredMethods(final MethodFilter methodFilter) {
        if (methodFilter == null) {
            throw new NullPointerException("methodFilter must not be null");
        }

        final List<InstrumentMethod> candidateList = new ArrayList<InstrumentMethod>();
        for (ASMMethodNodeAdapter methodNode : this.classNode.getDeclaredMethods()) {
            final InstrumentMethod method = new ASMMethod(this.objectBinderFactory, this.pluginContext, this.interceptorRegistryBinder, apiMetaDataService, this, methodNode);
            if (methodFilter.accept(method)) {
                candidateList.add(method);
            }
        }

        return candidateList;
    }

    @Override
    public InstrumentMethod getConstructor(final String... parameterTypes) {
        return getDeclaredMethod("<init>", parameterTypes);
    }

    @Override
    public boolean hasDeclaredMethod(final String methodName, final String... parameterTypes) {
        final String desc = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        return this.classNode.hasDeclaredMethod(methodName, desc);
    }

    @Override
    public boolean hasMethod(final String methodName, final String... parameterTypes) {
        final String desc = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        return this.classNode.hasMethod(methodName, desc);
    }

    @Override
    public boolean hasEnclosingMethod(final String methodName, final String... parameterTypes) {
        final String desc = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        return this.classNode.hasOutClass(methodName, desc);
    }

    @Override
    public boolean hasConstructor(final String... parameterTypeArray) {
        return getConstructor(parameterTypeArray) == null ? false : true;
    }

    @Override
    public boolean hasField(String name, String type) {
        final String desc = type == null ? null : JavaAssistUtils.toJvmSignature(type);
        return this.classNode.getField(name, desc) != null;
    }

    @Override
    public boolean hasField(String name) {
        return hasField(name, null);
    }

    @Override
    public void weave(final String adviceClassName) throws InstrumentException {
        if (adviceClassName == null) {
            throw new NotFoundInstrumentException("advice class name must not be null");
        }

        final ASMClassNodeAdapter adviceClassNode = ASMClassNodeAdapter.get(this.pluginContext, this.classLoader, JavaAssistUtils.javaNameToJvmName(adviceClassName));
        if (adviceClassNode == null) {
            throw new NotFoundInstrumentException(adviceClassName + " not found.");
        }

        final ASMAspectWeaver aspectWeaver = new ASMAspectWeaver();
        aspectWeaver.weaving(this.classNode, adviceClassNode);
        setModified(true);
    }

    @Override
    public InstrumentMethod addDelegatorMethod(final String methodName, final String... paramTypes) throws InstrumentException {
        // check duplicated method.
        if (getDeclaredMethod(methodName, paramTypes) != null) {
            throw new InstrumentException(getName() + " already have method(" + methodName + ").");
        }

        final ASMClassNodeAdapter superClassNode = ASMClassNodeAdapter.get(this.pluginContext, this.classLoader, this.classNode.getSuperClassInternalName());
        if (superClassNode == null) {
            throw new NotFoundInstrumentException(getName() + " not found super class(" + this.classNode.getSuperClassInternalName() + ")");
        }

        final String desc = JavaAssistUtils.javaTypeToJvmSignature(paramTypes);
        final ASMMethodNodeAdapter superMethodNode = superClassNode.getDeclaredMethod(methodName, desc);
        if (superMethodNode == null) {
            throw new NotFoundInstrumentException(methodName + desc + " is not found in " + superClassNode.getInternalName());
        }

        final ASMMethodNodeAdapter methodNode = this.classNode.addDelegatorMethod(superMethodNode);
        setModified(true);
        return new ASMMethod(this.objectBinderFactory, this.pluginContext, this.interceptorRegistryBinder, apiMetaDataService, this, methodNode);
    }

    @Override
    public void addField(final String accessorTypeName) throws InstrumentException {
        try {
            final Class<?> accessorType = this.pluginContext.injectClass(this.classLoader, accessorTypeName);
            final AccessorAnalyzer accessorAnalyzer = new AccessorAnalyzer();
            final AccessorAnalyzer.AccessorDetails accessorDetails = accessorAnalyzer.analyze(accessorType);

            final ASMFieldNodeAdapter fieldNode = this.classNode.addField(FIELD_PREFIX + JavaAssistUtils.javaClassNameToVariableName(accessorTypeName), accessorDetails.getFieldType());
            this.classNode.addInterface(accessorTypeName);
            this.classNode.addGetterMethod(accessorDetails.getGetter().getName(), fieldNode);
            this.classNode.addSetterMethod(accessorDetails.getSetter().getName(), fieldNode);
            setModified(true);
        } catch (Exception e) {
            throw new InstrumentException("Failed to add field with accessor [" + accessorTypeName + "]. Cause:" + e.getMessage(), e);
        }
    }

    @Override
    public void addGetter(final String getterTypeName, final String fieldName) throws InstrumentException {
        try {
            final Class<?> getterType = this.pluginContext.injectClass(this.classLoader, getterTypeName);
            final GetterAnalyzer.GetterDetails getterDetails = new GetterAnalyzer().analyze(getterType);
            final ASMFieldNodeAdapter fieldNode = this.classNode.getField(fieldName, null);
            if (fieldNode == null) {
                throw new IllegalArgumentException("Not found field. name=" + fieldName);
            }

            final String fieldTypeName = JavaAssistUtils.javaClassNameToObjectName(getterDetails.getFieldType().getName());
            if (!fieldNode.getClassName().equals(fieldTypeName)) {
                throw new IllegalArgumentException("different return type. return=" + fieldTypeName + ", field=" + fieldNode.getClassName());
            }

            this.classNode.addGetterMethod(getterDetails.getGetter().getName(), fieldNode);
            this.classNode.addInterface(getterTypeName);
            setModified(true);
        } catch (Exception e) {
            throw new InstrumentException("Failed to add getter: " + getterTypeName, e);
        }
    }

    @Override
    public void addSetter(String setterTypeName, String fieldName) throws InstrumentException {
        this.addSetter(setterTypeName, fieldName, false);
    }

    @Override
    public void addSetter(String setterTypeName, String fieldName, boolean removeFinal) throws InstrumentException {
        try {
            final Class<?> setterType = this.pluginContext.injectClass(this.classLoader, setterTypeName);
            final SetterAnalyzer.SetterDetails setterDetails = new SetterAnalyzer().analyze(setterType);
            final ASMFieldNodeAdapter fieldNode = this.classNode.getField(fieldName, null);
            if (fieldNode == null) {
                throw new IllegalArgumentException("Not found field. name=" + fieldName);
            }

            final String fieldTypeName = JavaAssistUtils.javaClassNameToObjectName(setterDetails.getFieldType().getName());
            if (!fieldNode.getClassName().equals(fieldTypeName)) {
                throw new IllegalArgumentException("Argument type of the setter is different with the field type. setterMethod: " + fieldTypeName + ", fieldType: " + fieldNode.getClassName());
            }

            if (fieldNode.isStatic()) {
                throw new IllegalArgumentException("Cannot add setter to static fields. setterMethod: " + setterDetails.getSetter().getName() + ", fieldName: " + fieldName);
            }

            final int original = fieldNode.getAccess();
            boolean finalRemoved = false;
            if (fieldNode.isFinal()) {
                if (!removeFinal) {
                    throw new IllegalArgumentException("Cannot add setter to final field. setterMethod: " + setterDetails.getSetter().getName() + ", fieldName: " + fieldName);
                } else {
                    final int removed = original & ~Opcodes.ACC_FINAL;
                    fieldNode.setAccess(removed);
                    finalRemoved = true;
                }
            }

            try {
                this.classNode.addSetterMethod(setterDetails.getSetter().getName(), fieldNode);
                this.classNode.addInterface(setterTypeName);
                setModified(true);
            } catch (Exception e) {
                if (finalRemoved) {
                    fieldNode.setAccess(original);
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
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
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
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
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
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
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
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
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
        final Class<?> interceptorType = this.pluginContext.injectClass(this.classLoader, interceptorClassName);

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
        return ((ASMMethod) constructor).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addInterceptor0(TargetMethod m, String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        InstrumentMethod method = getDeclaredMethod(m.name(), m.paramTypes());

        if (method == null) {
            throw new NotFoundInstrumentException("Cannot find method " + m.name() + " with parameter types: " + Arrays.toString(m.paramTypes()));
        }
        // TODO casting fix
        return ((ASMMethod) method).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addInterceptor0(TargetFilter annotation, String interceptorClassName, InterceptorScope scope, ExecutionPolicy executionPolicy, Object[] constructorArgs) throws InstrumentException {
        final String filterTypeName = annotation.type();
        Asserts.notNull(filterTypeName, "type of @TargetFilter");

        final InterceptorArgumentProvider interceptorArgumentProvider = objectBinderFactory.newInterceptorArgumentProvider(this);
        final AutoBindingObjectFactory filterFactory = objectBinderFactory.newAutoBindingObjectFactory(pluginContext, classLoader, interceptorArgumentProvider);
        final ObjectFactory objectFactory = ObjectFactory.byConstructor(filterTypeName, (Object[]) annotation.constructorArguments());
        final MethodFilter filter = (MethodFilter) filterFactory.createInstance(objectFactory);

        boolean singleton = annotation.singleton();
        int interceptorId = -1;

        for (InstrumentMethod m : getDeclaredMethods(filter)) {
            if (singleton && interceptorId != -1) {
                m.addInterceptor(interceptorId);
            } else {
                // TODO casting fix
                interceptorId = ((ASMMethod) m).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
            }
        }

        if (interceptorId == -1) {
            logger.warn("No methods are intercepted. target: " + this.classNode.getInternalName(), ", interceptor: " + interceptorClassName + ", methodFilter: " + filterTypeName);
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
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
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
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
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
                interceptorId = ((ASMMethod) m).addInterceptorInternal(interceptorClassName, constructorArgs, scope, executionPolicy);
            }
        }

        if (interceptorId == -1) {
            logger.warn("No methods are intercepted. target: " + this.classNode.getInternalName(), ", interceptor: " + interceptorClassName + ", methodFilter: " + filter.getClass().getName());
        }

        return interceptorId;
    }

    @Override
    public List<InstrumentClass> getNestedClasses(ClassFilter filter) {
        final List<InstrumentClass> nestedClasses = new ArrayList<InstrumentClass>();
        for (ASMClassNodeAdapter innerClassNode : this.classNode.getInnerClasses()) {
            final ASMNestedClass nestedClass = new ASMNestedClass(objectBinderFactory, this.pluginContext, this.interceptorRegistryBinder, apiMetaDataService, this.classLoader, innerClassNode);
            if (filter.accept(nestedClass)) {
                nestedClasses.add(nestedClass);
            }
        }

        return nestedClasses;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public byte[] toBytecode() {
        return classNode.toByteArray();
    }
}