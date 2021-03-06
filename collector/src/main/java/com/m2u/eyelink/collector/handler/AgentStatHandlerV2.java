package com.m2u.eyelink.collector.handler;

import org.apache.thrift.TBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.m2u.eyelink.collector.bo.stat.ActiveTraceBo;
import com.m2u.eyelink.collector.bo.stat.CpuLoadBo;
import com.m2u.eyelink.collector.bo.stat.DataSourceListBo;
import com.m2u.eyelink.collector.bo.stat.JvmGcBo;
import com.m2u.eyelink.collector.bo.stat.JvmGcDetailedBo;
import com.m2u.eyelink.collector.bo.stat.TransactionBo;
import com.m2u.eyelink.collector.dao.AgentAlarmDao;
import com.m2u.eyelink.collector.dao.AgentStatDaoV2;
import com.m2u.eyelink.collector.mapper.thrift.stat.AgentStatBatchMapper;
import com.m2u.eyelink.collector.mapper.thrift.stat.AgentStatBo;
import com.m2u.eyelink.collector.mapper.thrift.stat.AgentStatMapper;
import com.m2u.eyelink.collector.service.AgentStatService;
import com.m2u.eyelink.thrift.TAgentStat;
import com.m2u.eyelink.thrift.TAgentStatBatch;

@Service("agentStatHandlerV2")
public class AgentStatHandlerV2 implements Handler {

    private final Logger logger = LoggerFactory.getLogger(AgentStatHandlerV2.class.getName());

    @Autowired
    private AgentStatMapper agentStatMapper;

    // FIXME add logic under commented logic
    @Autowired
    private AgentStatBatchMapper agentStatBatchMapper;

    @Autowired
    private AgentStatDaoV2<JvmGcBo> jvmGcDao;

    @Autowired
    private AgentStatDaoV2<JvmGcDetailedBo> jvmGcDetailedDao;

    @Autowired
    private AgentStatDaoV2<CpuLoadBo> cpuLoadDao;

    @Autowired
    private AgentStatDaoV2<TransactionBo> transactionDao;

    @Autowired
    private AgentStatDaoV2<ActiveTraceBo> activeTraceDao;

    @Autowired
    private AgentStatDaoV2<DataSourceListBo> dataSourceListDao;

    @Autowired
    private AgentAlarmDao agentAlarmDao;

    @Autowired(required = false)
    private AgentStatService agentStatService;

    @Override
    public void handle(TBase<?, ?> tbase) {
        // FIXME (2014.08) Legacy - TAgentStat should not be sent over the wire.
        if (tbase instanceof TAgentStat) {
            TAgentStat tAgentStat = (TAgentStat)tbase;
            this.handleAgentStat(tAgentStat);
        } else if (tbase instanceof TAgentStatBatch) {
            TAgentStatBatch tAgentStatBatch = (TAgentStatBatch) tbase;
            this.handleAgentStatBatch(tAgentStatBatch);
        } else {
            throw new IllegalArgumentException("unexpected tbase:" + tbase + " expected:" + TAgentStat.class.getName() + " or " + TAgentStatBatch.class.getName());
        }

        if (agentStatService != null) {
            agentStatService.save(tbase);
        }

    }

    private void handleAgentStat(TAgentStat tAgentStat) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received TAgentStat={}", tAgentStat);
        }
        AgentStatBo agentStatBo = this.agentStatMapper.map(tAgentStat);
        this.insertAgentStatBatch(agentStatBo);
    }

    private void handleAgentStatBatch(TAgentStatBatch tAgentStatBatch) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received TAgentStatBatch={}", tAgentStatBatch);
        }
        AgentStatBo agentStatBo = this.agentStatBatchMapper.map(tAgentStatBatch);
        this.insertAgentStatBatch(agentStatBo);
    }

    private void insertAgentStatBatch(AgentStatBo agentStatBo) {
        if (agentStatBo == null) {
            return;
        }
        final String agentId = agentStatBo.getAgentId();
        try {
            this.jvmGcDao.insert(agentId, agentStatBo.getJvmGcBos());
            this.jvmGcDetailedDao.insert(agentId, agentStatBo.getJvmGcDetailedBos());
            this.cpuLoadDao.insert(agentId, agentStatBo.getCpuLoadBos());
            this.transactionDao.insert(agentId, agentStatBo.getTransactionBos());
            // TODO request 발생해서 카운트 정산 집계 여부 테스트 필요함.
            this.activeTraceDao.insert(agentId, agentStatBo.getActiveTraceBos());
            // TODO data source 정의 후 테스트 필요함.
            this.dataSourceListDao.insert(agentId, agentStatBo.getDataSourceListBos());
            // check cpu, mem, service alarm
            this.agentAlarmDao.insert(agentId, agentStatBo);
        } catch (Exception e) {
            logger.warn("Error inserting AgentStatBo. Caused:{}", e.getMessage(), e);
        }
    }

}
