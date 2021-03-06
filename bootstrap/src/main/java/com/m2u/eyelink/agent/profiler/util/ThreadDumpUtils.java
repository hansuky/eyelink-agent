package com.m2u.eyelink.agent.profiler.util;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Collections;

import com.m2u.eyelink.common.util.ThreadMXBeanUtils;
import com.m2u.eyelink.thrift.TMonitorInfo;
import com.m2u.eyelink.thrift.TThreadDump;
import com.m2u.eyelink.thrift.TThreadState;

public class ThreadDumpUtils {

    public static TThreadDump createTThreadDump(Thread thread) {
        ThreadInfo threadInfo = ThreadMXBeanUtils.findThread(thread);
        if (threadInfo == null) {
            return null;
        }

        return createTThreadDump(threadInfo);
    }

    public static TThreadDump createTThreadDump(Thread thread, int stackTraceMaxDepth) {
        ThreadInfo threadInfo = ThreadMXBeanUtils.findThread(thread, stackTraceMaxDepth);
        if (threadInfo == null) {
            return null;
        }

        return createTThreadDump(threadInfo);
    }

    public static TThreadDump createTThreadDump(ThreadInfo threadInfo) {
        TThreadDump threadDump = new TThreadDump();
        setThreadInfo(threadDump, threadInfo);
        setThreadStatus(threadDump, threadInfo);
        setStackTrace(threadDump, threadInfo);
        setMonitorInfo(threadDump, threadInfo);
        setLockInfo(threadDump, threadInfo);

        return threadDump;
    }

    public static TThreadState toTThreadState(Thread.State threadState) {
        if (threadState == null) {
            throw new NullPointerException("threadState may not be null");
        }

        String threadStateName = threadState.name();
        for (TThreadState state : TThreadState.values()) {
            if (state.name().equalsIgnoreCase(threadStateName)) {
                return state;
            }
        }
        return TThreadState.UNKNOWN;
    }

    private static void setThreadInfo(TThreadDump threadDump, ThreadInfo threadInfo) {
        threadDump.setThreadName(threadInfo.getThreadName());
        threadDump.setThreadId(threadInfo.getThreadId());
        threadDump.setBlockedTime(threadInfo.getBlockedTime());
        threadDump.setBlockedCount(threadInfo.getBlockedCount());
        threadDump.setWaitedTime(threadInfo.getWaitedTime());
        threadDump.setWaitedCount(threadInfo.getWaitedCount());
    }

    private static void setThreadStatus(TThreadDump threadDump, ThreadInfo threadInfo) {
        threadDump.setInNative(threadInfo.isInNative());
        threadDump.setSuspended(threadInfo.isSuspended());
        threadDump.setThreadState(getThreadState(threadInfo));
    }

    private static void setStackTrace(TThreadDump threadDump, ThreadInfo threadInfo) {
        StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
        if (stackTraceElements != null) {
            for (StackTraceElement element : stackTraceElements) {
                if (element == null) {
                    continue;
                }
                threadDump.addToStackTrace(element.toString());
            }
        } else {
            threadDump.setStackTrace(Collections.<String>emptyList());
        }
    }

    private static void setMonitorInfo(TThreadDump threadDump, ThreadInfo threadInfo) {
        MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();
        if (monitorInfos != null) {
            for (MonitorInfo each : monitorInfos) {
                if (each == null) {
                    continue;
                }
                TMonitorInfo tMonitorInfo = new TMonitorInfo();

                tMonitorInfo.setStackDepth(each.getLockedStackDepth());
                tMonitorInfo.setStackFrame(each.getLockedStackFrame().toString());

                threadDump.addToLockedMonitors(tMonitorInfo);
            }
        } else {
            threadDump.setLockedMonitors(Collections.<TMonitorInfo>emptyList());
        }
    }

    private static void setLockInfo(TThreadDump threadDump, ThreadInfo threadInfo) {
        threadDump.setLockName(threadInfo.getLockName());
        threadDump.setLockOwnerId(threadInfo.getLockOwnerId());
        threadDump.setLockOwnerName(threadInfo.getLockOwnerName());

        LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();

        if (lockInfos != null) {
            for (LockInfo lockInfo : lockInfos) {
                if (lockInfo == null) {
                    continue;
                }
                threadDump.addToLockedSynchronizers(lockInfo.toString());
            }
        } else {
            threadDump.setLockedSynchronizers(Collections.<String>emptyList());
        }
    }

    private static TThreadState getThreadState(ThreadInfo info) {
        return toTThreadState(info.getThreadState());
    }

}
