package com.m2u.eyelink.agent.profiler.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.m2u.eyelink.agent.profiler.context.id.AsyncIdGenerator;
import com.m2u.eyelink.agent.profiler.context.recorder.RecorderFactory;
import com.m2u.eyelink.agent.profiler.context.recorder.WrappedSpanEventRecorder;
import com.m2u.eyelink.context.AsyncTraceId;
import com.m2u.eyelink.context.DefaultAsyncTraceId;
import com.m2u.eyelink.context.SpanEventRecorder;
import com.m2u.eyelink.context.SpanRecorder;
import com.m2u.eyelink.context.Storage;
import com.m2u.eyelink.context.Trace;
import com.m2u.eyelink.context.TraceId;
import com.m2u.eyelink.context.TraceScope;
import com.m2u.eyelink.context.scope.DefaultTraceScopePool;
import com.m2u.eyelink.exception.ELAgentException;

public final class DefaultTrace implements Trace {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTrace.class.getName());
    private static final boolean isTrace = logger.isTraceEnabled();
    private static final boolean isWarn = logger.isWarnEnabled();

    private final boolean sampling;

    private final long localTransactionId;
    private final TraceId traceId;
    private final CallStack callStack;

    private final Storage storage;

    private final Span span;
    private final SpanRecorder spanRecorder;
    private final WrappedSpanEventRecorder spanEventRecorder;

    private final AsyncIdGenerator asyncIdGenerator;

    private boolean closed = false;

    private Thread bindThread;
    private final DefaultTraceScopePool scopePool = new DefaultTraceScopePool();

    public DefaultTrace(CallStackFactory callStackFactory, Storage storage, TraceId traceId, long localTransactionId, AsyncIdGenerator asyncIdGenerator, boolean sampling,
                        SpanFactory spanFactory, RecorderFactory recorderFactory) {
        if (storage == null) {
            throw new NullPointerException("storage must not be null");
        }
        if (traceId == null) {
            throw new NullPointerException("continueTraceId must not be null");
        }
        if (asyncIdGenerator == null) {
            throw new NullPointerException("asyncIdGenerator must not be null");
        }
        if (spanFactory == null) {
            throw new NullPointerException("spanFactory must not be null");
        }
        if (recorderFactory == null) {
            throw new NullPointerException("recorderFactory must not be null");
        }

        this.storage = storage;
        this.traceId = traceId;
        this.localTransactionId = localTransactionId;
        this.sampling = sampling;
        this.span = spanFactory.newSpan();
        this.span.recordTraceId(traceId);
        this.spanRecorder = recorderFactory.newSpanRecorder(span, traceId.isRoot(), sampling);

        this.spanEventRecorder = recorderFactory.newWrappedSpanEventRecorder();
        this.callStack = callStackFactory.newCallStack(span);

        this.asyncIdGenerator = asyncIdGenerator;
        setCurrentThread();
    }

    private SpanEventRecorder wrappedSpanEventRecorder(SpanEvent spanEvent) {
        final WrappedSpanEventRecorder spanEventRecorder = this.spanEventRecorder;
        spanEventRecorder.setWrapped(spanEvent);
        return spanEventRecorder;
    }

    public Span getSpan() {
        return span;
    }

    @Override
    public SpanEventRecorder traceBlockBegin() {
        return traceBlockBegin(DEFAULT_STACKID);
    }

    @Override
    public SpanEventRecorder traceBlockBegin(final int stackId) {
        // Set properties for the case when stackFrame is not used as part of Span.
        final SpanEvent spanEvent = new SpanEvent(span);
        spanEvent.markStartTime();
        spanEvent.setStackId(stackId);

        if (this.closed) {
            if (isWarn) {
            	ELAgentException exception = new ELAgentException("already closed trace.");
                logger.warn("[DefaultTrace] Corrupted call stack found.", exception);
            }
        } else {
            callStack.push(spanEvent);
        }

        return wrappedSpanEventRecorder(spanEvent);
    }

    @Override
    public void traceBlockEnd() {
        traceBlockEnd(DEFAULT_STACKID);
    }

    @Override
    public void traceBlockEnd(int stackId) {
        if (this.closed) {
            if (isWarn) {
                final ELAgentException exception = new ELAgentException("already closed trace.");
                logger.warn("[DefaultTrace] Corrupted call stack found.", exception);
            }
            return;
        }

        final SpanEvent spanEvent = callStack.pop();
        if (spanEvent == null) {
            if (isWarn) {
            	ELAgentException exception = new ELAgentException("call stack is empty.");
                logger.warn("[DefaultTrace] Corrupted call stack found.", exception);
            }
            return;
        }

        if (spanEvent.getStackId() != stackId) {
            // stack dump will make debugging easy.
            if (isWarn) {
            	ELAgentException exception = new ELAgentException("not matched stack id. expected=" + stackId + ", current=" + spanEvent.getStackId());
                logger.warn("[DefaultTrace] Corrupted call stack found.", exception);
            }
        }

        if (spanEvent.isTimeRecording()) {
            spanEvent.markAfterTime();
        }
        logSpan(spanEvent);
    }

    @Override
    public void close() {
        if (closed) {
            logger.warn("Already closed trace.");
            return;
        }
        closed = true;

        if (!callStack.empty()) {
            if (isWarn) {
            	ELAgentException exception = new ELAgentException("not empty call stack.");
                logger.warn("[DefaultTrace] Corrupted call stack found.", exception);
            }
            // skip
        } else {
            if (span.isTimeRecording()) {
                span.markAfterTime();
            }
            logSpan(span);
        }

        this.storage.close();

    }

    @Override
    public void flush() {
        this.storage.flush();
    }

    /**
     * Get current TraceID. If it was not set this will return null.
     *
     * @return
     */
    @Override
    public TraceId getTraceId() {
        return this.traceId;
    }

    @Override
    public long getId() {
        return this.localTransactionId;
    }

    @Override
    public long getStartTime() {
        return span.getStartTime();
    }

    @Override
    public Thread getBindThread() {
        return bindThread;
    }

    private void setCurrentThread() {
        this.setBindThread(Thread.currentThread());
    }

    private void setBindThread(Thread thread) {
        bindThread = thread;
    }


    public boolean canSampled() {
        return this.sampling;
    }

    public boolean isRoot() {
        return getTraceId().isRoot();
    }

    private void logSpan(SpanEvent spanEvent) {
        if (isTrace) {
            final Thread th = Thread.currentThread();
            logger.trace("[DefaultTrace] Write {} thread{id={}, name={}}", spanEvent, th.getId(), th.getName());
        }
        storage.store(spanEvent);
    }

    private void logSpan(Span span) {
        if (isTrace) {
            final Thread th = Thread.currentThread();
            logger.trace("[DefaultTrace] Write {} thread{id={}, name={}}", span, th.getId(), th.getName());
        }
        this.storage.store(span);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isRootStack() {
        return callStack.empty();
    }

    @Override
    public AsyncTraceId getAsyncTraceId() {
        return getAsyncTraceId(false);
    }

    @Override
    public AsyncTraceId getAsyncTraceId(boolean closeable) {
        // ignored closeable.
        return new DefaultAsyncTraceId(traceId, asyncIdGenerator.nextAsyncId(), span.getStartTime());
    }

    @Override
    public SpanRecorder getSpanRecorder() {
        return spanRecorder;
    }

    @Override
    public SpanEventRecorder currentSpanEventRecorder() {
        SpanEvent spanEvent = callStack.peek();
        if (spanEvent == null) {
            if (isWarn) {
            		ELAgentException exception = new ELAgentException("call stack is empty");
                logger.warn("[DefaultTrace] Corrupted call stack found.", exception);
            }
            // make dummy.
            spanEvent = new SpanEvent(span);
        }

        return wrappedSpanEventRecorder(spanEvent);
    }

    @Override
    public int getCallStackFrameId() {
        final SpanEvent spanEvent = callStack.peek();
        if (spanEvent == null) {
            return ROOT_STACKID;
        } else {
            return spanEvent.getStackId();
        }
    }

    @Override
    public TraceScope getScope(String name) {
        return scopePool.get(name);
    }

    @Override
    public TraceScope addScope(String name) {
        return scopePool.add(name);
    }
}