package com.m2u.eyelink.agent.profiler.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.m2u.eyelink.agent.profiler.monitor.codahale.ActiveTraceMetricCollector;
import com.m2u.eyelink.agent.profiler.monitor.codahale.CpuLoadCollector;
import com.m2u.eyelink.agent.profiler.monitor.codahale.DataSourceCollector;
import com.m2u.eyelink.agent.profiler.monitor.codahale.TransactionMetricCollector;
import com.m2u.eyelink.context.thrift.TActiveTrace;
import com.m2u.eyelink.context.thrift.TAgentStat;
import com.m2u.eyelink.context.thrift.TAgentStatBatch;
import com.m2u.eyelink.context.thrift.TCpuLoad;
import com.m2u.eyelink.context.thrift.TDataSourceList;
import com.m2u.eyelink.context.thrift.TJvmGc;
import com.m2u.eyelink.context.thrift.TTransaction;
import com.m2u.eyelink.sender.DataSender;
import com.m2u.eyelink.util.ELAgentThreadFactory;

public class AgentStatMonitor {

    private static final long DEFAULT_COLLECTION_INTERVAL_MS = 1000 * 5;
    private static final int DEFAULT_NUM_COLLECTIONS_PER_SEND = 6;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isTrace = logger.isTraceEnabled();
    private final long collectionIntervalMs;
    private final int numCollectionsPerBatch;

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, new ELAgentThreadFactory("Pinpoint-stat-monitor", true));

    private final DataSender dataSender;
    private final String agentId;
    private final AgentStatCollectorFactory agentStatCollectorFactory;
    private final long agentStartTime;

    public AgentStatMonitor(DataSender dataSender, String agentId, long startTime, AgentStatCollectorFactory agentStatCollectorFactory) {
        this(dataSender, agentId, startTime, agentStatCollectorFactory, DEFAULT_COLLECTION_INTERVAL_MS, DEFAULT_NUM_COLLECTIONS_PER_SEND);
    }

    public AgentStatMonitor(DataSender dataSender, String agentId, long startTime, AgentStatCollectorFactory agentStatCollectorFactory, long collectionInterval, int numCollectionsPerBatch) {
        if (dataSender == null) {
            throw new NullPointerException("dataSender must not be null");
        }
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }
        if (agentStatCollectorFactory == null) {
            throw new NullPointerException("agentStatCollectorFactory must not be null");
        }
        this.dataSender = dataSender;
        this.agentId = agentId;
        this.agentStartTime = startTime;
        this.agentStatCollectorFactory = agentStatCollectorFactory;
        this.collectionIntervalMs = collectionInterval;
        this.numCollectionsPerBatch = numCollectionsPerBatch;
    }

    public void start() {
        CollectJob job = new CollectJob(this.numCollectionsPerBatch);
        executor.scheduleAtFixedRate(job, this.collectionIntervalMs, this.collectionIntervalMs, TimeUnit.MILLISECONDS);
        logger.info("AgentStat monitor started");
    }

    public void stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("AgentStat monitor stopped");
    }

    // NotThreadSafe
    private class CollectJob implements Runnable {

        private final GarbageCollector garbageCollector;
        private final CpuLoadCollector cpuLoadCollector;
        private final TransactionMetricCollector transactionMetricCollector;
        private final ActiveTraceMetricCollector activeTraceMetricCollector;
        private final DataSourceCollector dataSourceCollector;

        // Not thread safe. For use with single thread ONLY
        private final int numStatsPerBatch;
        private int collectCount = 0;
        private long prevCollectionTimestamp = System.currentTimeMillis();
        private List<TAgentStat> agentStats;

        private CollectJob(int numStatsPerBatch) {
            this.garbageCollector = agentStatCollectorFactory.getGarbageCollector();
            this.cpuLoadCollector = agentStatCollectorFactory.getCpuLoadCollector();
            this.transactionMetricCollector = agentStatCollectorFactory.getTransactionMetricCollector();
            this.activeTraceMetricCollector = agentStatCollectorFactory.getActiveTraceMetricCollector();
            this.dataSourceCollector = agentStatCollectorFactory.getDataSourceCollector();
            this.numStatsPerBatch = numStatsPerBatch;
            this.agentStats = new ArrayList<TAgentStat>(this.numStatsPerBatch);
        }

        @Override
        public void run() {
            final long currentCollectionTimestamp = System.currentTimeMillis();
            final long collectInterval = currentCollectionTimestamp - this.prevCollectionTimestamp;
            try {
                final TAgentStat agentStat = collectAgentStat();
                agentStat.setTimestamp(currentCollectionTimestamp);
                agentStat.setCollectInterval(collectInterval);
                this.agentStats.add(agentStat);
                if (++this.collectCount >= this.numStatsPerBatch) {
                    sendAgentStats();
                    this.collectCount = 0;
                }
            } catch (Exception ex) {
                logger.warn("AgentStat collect failed. Caused:{}", ex.getMessage(), ex);
            } finally {
                this.prevCollectionTimestamp = currentCollectionTimestamp;
            }
        }

        private TAgentStat collectAgentStat() {
            final TAgentStat agentStat = new TAgentStat();
            final TJvmGc gc = garbageCollector.collect();
            agentStat.setGc(gc);
            final TCpuLoad cpuLoad = cpuLoadCollector.collect();
            agentStat.setCpuLoad(cpuLoad);
            final TTransaction transaction = transactionMetricCollector.collect();
            agentStat.setTransaction(transaction);
            final TActiveTrace activeTrace = activeTraceMetricCollector.collect();
            agentStat.setActiveTrace(activeTrace);
             final TDataSourceList dataSourceList = dataSourceCollector.collect();
             agentStat.setDataSourceList(dataSourceList);

            return agentStat;
        }

        private void sendAgentStats() {
            // prepare TAgentStat object.
            // TODO multi thread issue.
            // If we reuse TAgentStat, there could be concurrency issue because data sender runs in a different thread.
            final TAgentStatBatch agentStatBatch = new TAgentStatBatch();
            agentStatBatch.setAgentId(agentId);
            agentStatBatch.setStartTimestamp(agentStartTime);
            agentStatBatch.setAgentStats(this.agentStats);
            // If we reuse agentStats list, there could be concurrency issue because data sender runs in a different
            // thread.
            // So create new list.
            this.agentStats = new ArrayList<TAgentStat>(this.numStatsPerBatch);
            if (isTrace) {
                logger.trace("collect agentStat:{}", agentStatBatch);
            }
            dataSender.send(agentStatBatch);
        }
    }

}