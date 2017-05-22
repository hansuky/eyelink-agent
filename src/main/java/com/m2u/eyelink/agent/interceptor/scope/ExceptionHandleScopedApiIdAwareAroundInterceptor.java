package com.m2u.eyelink.agent.interceptor.scope;

import com.m2u.eyelink.agent.interceptor.ApiIdAwareAroundInterceptor;
import com.m2u.eyelink.agent.interceptor.InterceptorInvokerHelper;
import com.m2u.eyelink.logging.PLogger;
import com.m2u.eyelink.logging.PLoggerFactory;

public class ExceptionHandleScopedApiIdAwareAroundInterceptor implements ApiIdAwareAroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    private final boolean debugEnabled = logger.isDebugEnabled();

    private final ApiIdAwareAroundInterceptor delegate;
    private final InterceptorScope scope;
    private final ExecutionPolicy policy;

    public ExceptionHandleScopedApiIdAwareAroundInterceptor(ApiIdAwareAroundInterceptor delegate, InterceptorScope scope, ExecutionPolicy policy) {
        if (delegate == null) {
            throw new NullPointerException("delegate must not be null");
        }
        if (scope == null) {
            throw new NullPointerException("scope must not be null");
        }
        if (policy == null) {
            throw new NullPointerException("policy must not be null");
        }
        this.delegate = delegate;
        this.scope = scope;
        this.policy = policy;
    }

    @Override
    public void before(Object target, int apiId, Object[] args) {
        final InterceptorScopeInvocation transaction = scope.getCurrentInvocation();

        if (transaction.tryEnter(policy)) {
            try {
                this.delegate.before(target, apiId, args);
            } catch (Throwable t) {
                InterceptorInvokerHelper.handleException(t);
            }
        } else {
            if (debugEnabled) {
                logger.debug("tryBefore() returns false: interceptorScopeTransaction: {}, executionPoint: {}. Skip interceptor {}", transaction, policy, delegate.getClass());
            }
        }
    }

    @Override
    public void after(Object target, int apiId, Object[] args, Object result, Throwable throwable) {
        final InterceptorScopeInvocation transaction = scope.getCurrentInvocation();

        if (transaction.canLeave(policy)) {
            try {
                this.delegate.after(target, apiId, args, result, throwable);
            } catch (Throwable t) {
                InterceptorInvokerHelper.handleException(t);
            } finally {
                transaction.leave(policy);
            }
        } else {
            if (debugEnabled) {
                logger.debug("tryAfter() returns false: interceptorScopeTransaction: {}, executionPoint: {}. Skip interceptor {}", transaction, policy, delegate.getClass());
            }
        }
    }
}