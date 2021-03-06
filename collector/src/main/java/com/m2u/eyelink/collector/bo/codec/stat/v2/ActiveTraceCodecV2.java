package com.m2u.eyelink.collector.bo.codec.stat.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.m2u.eyelink.collector.bo.codec.stat.AgentStatCodec;
import com.m2u.eyelink.collector.bo.codec.stat.AgentStatDataPointCodec;
import com.m2u.eyelink.collector.bo.codec.stat.header.AgentStatHeaderDecoder;
import com.m2u.eyelink.collector.bo.codec.stat.header.AgentStatHeaderEncoder;
import com.m2u.eyelink.collector.bo.codec.stat.header.BitCountingHeaderDecoder;
import com.m2u.eyelink.collector.bo.codec.stat.header.BitCountingHeaderEncoder;
import com.m2u.eyelink.collector.bo.codec.strategy.EncodingStrategy;
import com.m2u.eyelink.collector.bo.codec.strategy.StrategyAnalyzer;
import com.m2u.eyelink.collector.bo.codec.strategy.UnsignedIntegerEncodingStrategy;
import com.m2u.eyelink.collector.bo.codec.strategy.UnsignedLongEncodingStrategy;
import com.m2u.eyelink.collector.bo.codec.strategy.UnsignedShortEncodingStrategy;
import com.m2u.eyelink.collector.bo.serializer.stat.AgentStatDecodingContext;
import com.m2u.eyelink.collector.bo.stat.ActiveTraceBo;
import com.m2u.eyelink.collector.util.MapUtils;
import com.m2u.eyelink.common.trace.SlotType;
import com.m2u.eyelink.util.Buffer;
import com.m2u.eyelink.util.CollectionUtils;

@Component("activeTraceCodecV2")
public class ActiveTraceCodecV2 implements AgentStatCodec<ActiveTraceBo> {

    private static final byte VERSION = 2;

    private final AgentStatDataPointCodec codec;

    @Autowired
    public ActiveTraceCodecV2(AgentStatDataPointCodec codec) {
        Assert.notNull("agentStatDataPointCodec must not be null");
        this.codec = codec;
    }

    @Override
    public byte getVersion() {
        return VERSION;
    }

    @Override
    public void encodeValues(Buffer valueBuffer, List<ActiveTraceBo> activeTraceBos) {
        if (CollectionUtils.isEmpty(activeTraceBos)) {
            throw new IllegalArgumentException("activeTraceBos must not be empty");
        }
        final int numValues = activeTraceBos.size();
        valueBuffer.putVInt(numValues);

        List<Long> startTimestamps = new ArrayList<Long>(numValues);
        List<Long> timestamps = new ArrayList<Long>(numValues);
        UnsignedShortEncodingStrategy.Analyzer.Builder versionAnalyzerBuilder = new UnsignedShortEncodingStrategy.Analyzer.Builder();
        UnsignedIntegerEncodingStrategy.Analyzer.Builder schemaTypeAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        UnsignedIntegerEncodingStrategy.Analyzer.Builder fastTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        UnsignedIntegerEncodingStrategy.Analyzer.Builder normalTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        UnsignedIntegerEncodingStrategy.Analyzer.Builder slowTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        UnsignedIntegerEncodingStrategy.Analyzer.Builder verySlowTraceCountsAnalyzerBuilder = new UnsignedIntegerEncodingStrategy.Analyzer.Builder();
        for (ActiveTraceBo activeTraceBo : activeTraceBos) {
            startTimestamps.add(activeTraceBo.getStartTimestamp());
            timestamps.add(activeTraceBo.getTimestamp());
            versionAnalyzerBuilder.addValue(activeTraceBo.getVersion());
            schemaTypeAnalyzerBuilder.addValue(activeTraceBo.getHistogramSchemaType());
            final Map<String, Integer> activeTraceCounts = activeTraceBo.getActiveTraceCounts();
            fastTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.FAST, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
            normalTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.NORMAL, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
            slowTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.SLOW, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
            verySlowTraceCountsAnalyzerBuilder.addValue(MapUtils.getIntValue(activeTraceCounts, SlotType.VERY_SLOW, ActiveTraceBo.UNCOLLECTED_ACTIVE_TRACE_COUNT));
        }
        this.codec.encodeValues(valueBuffer, UnsignedLongEncodingStrategy.REPEAT_COUNT, startTimestamps);
        this.codec.encodeTimestamps(valueBuffer, timestamps);
        this.encodeDataPoints(
                valueBuffer,
                versionAnalyzerBuilder.build(),
                schemaTypeAnalyzerBuilder.build(),
                fastTraceCountsAnalyzerBuilder.build(),
                normalTraceCountsAnalyzerBuilder.build(),
                slowTraceCountsAnalyzerBuilder.build(),
                verySlowTraceCountsAnalyzerBuilder.build());
    }

    private void encodeDataPoints(
            Buffer valueBuffer,
            StrategyAnalyzer<Short> versionStrategyAnalyzer,
            StrategyAnalyzer<Integer> schemaTypeStrategyAnalyzer,
            StrategyAnalyzer<Integer> fastTraceCountsStrategyAnalyzer,
            StrategyAnalyzer<Integer> normalTraceCountsStrategyAnalyzer,
            StrategyAnalyzer<Integer> slowTraceCountsStrategyAnalyzer,
            StrategyAnalyzer<Integer> verySlowTraceCountsStrategyAnalyzer) {
        // encode header
        AgentStatHeaderEncoder headerEncoder = new BitCountingHeaderEncoder();
        headerEncoder.addCode(versionStrategyAnalyzer.getBestStrategy().getCode());
        headerEncoder.addCode(schemaTypeStrategyAnalyzer.getBestStrategy().getCode());
        headerEncoder.addCode(fastTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
        headerEncoder.addCode(normalTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
        headerEncoder.addCode(slowTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
        headerEncoder.addCode(verySlowTraceCountsStrategyAnalyzer.getBestStrategy().getCode());
        final byte[] header = headerEncoder.getHeader();
        valueBuffer.putPrefixedBytes(header);
        // encode values
        this.codec.encodeValues(valueBuffer, versionStrategyAnalyzer.getBestStrategy(), versionStrategyAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, schemaTypeStrategyAnalyzer.getBestStrategy(), schemaTypeStrategyAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, fastTraceCountsStrategyAnalyzer.getBestStrategy(), fastTraceCountsStrategyAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, normalTraceCountsStrategyAnalyzer.getBestStrategy(), normalTraceCountsStrategyAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, slowTraceCountsStrategyAnalyzer.getBestStrategy(), slowTraceCountsStrategyAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, verySlowTraceCountsStrategyAnalyzer.getBestStrategy(), verySlowTraceCountsStrategyAnalyzer.getValues());
    }

    @Override
    public List<ActiveTraceBo> decodeValues(Buffer valueBuffer, AgentStatDecodingContext decodingContext) {
        final String agentId = decodingContext.getAgentId();
        final long baseTimestamp = decodingContext.getBaseTimestamp();
        final long timestampDelta = decodingContext.getTimestampDelta();
        final long initialTimestamp = baseTimestamp + timestampDelta;

        int numValues = valueBuffer.readVInt();
        List<Long> startTimestamps = this.codec.decodeValues(valueBuffer, UnsignedLongEncodingStrategy.REPEAT_COUNT, numValues);
        List<Long> timestamps = this.codec.decodeTimestamps(initialTimestamp, valueBuffer, numValues);

        // decode headers
        final byte[] header = valueBuffer.readPrefixedBytes();
        AgentStatHeaderDecoder headerDecoder = new BitCountingHeaderDecoder(header);
        EncodingStrategy<Short> versionEncodingStrategy = UnsignedShortEncodingStrategy.getFromCode(headerDecoder.getCode());
        EncodingStrategy<Integer> schemaTypeEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
        EncodingStrategy<Integer> fastTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
        EncodingStrategy<Integer> normalTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
        EncodingStrategy<Integer> slowTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
        EncodingStrategy<Integer> verySlowTraceCountsEncodingStrategy = UnsignedIntegerEncodingStrategy.getFromCode(headerDecoder.getCode());
        // decode values
        List<Short> versions = this.codec.decodeValues(valueBuffer, versionEncodingStrategy, numValues);
        List<Integer> schemaTypes = this.codec.decodeValues(valueBuffer, schemaTypeEncodingStrategy, numValues);
        List<Integer> fastTraceCounts = this.codec.decodeValues(valueBuffer, fastTraceCountsEncodingStrategy, numValues);
        List<Integer> normalTraceCounts = this.codec.decodeValues(valueBuffer, normalTraceCountsEncodingStrategy, numValues);
        List<Integer> slowTraceCounts = this.codec.decodeValues(valueBuffer, slowTraceCountsEncodingStrategy, numValues);
        List<Integer> verySlowTraceCounts = this.codec.decodeValues(valueBuffer, verySlowTraceCountsEncodingStrategy, numValues);

        List<ActiveTraceBo> activeTraceBos = new ArrayList<ActiveTraceBo>(numValues);
        for (int i = 0; i < numValues; ++i) {
            ActiveTraceBo activeTraceBo = new ActiveTraceBo();
            activeTraceBo.setAgentId(agentId);
            activeTraceBo.setStartTimestamp(startTimestamps.get(i));
            activeTraceBo.setTimestamp(timestamps.get(i));
            activeTraceBo.setVersion(versions.get(i));
            activeTraceBo.setHistogramSchemaType(schemaTypes.get(i));
            Map<String, Integer> activeTraceCounts = new HashMap<String, Integer>();
            activeTraceCounts.put(""+SlotType.FAST, fastTraceCounts.get(i));
            activeTraceCounts.put(""+SlotType.NORMAL, normalTraceCounts.get(i));
            activeTraceCounts.put(""+SlotType.SLOW, slowTraceCounts.get(i));
            activeTraceCounts.put(""+SlotType.VERY_SLOW, verySlowTraceCounts.get(i));
            activeTraceBo.setActiveTraceCounts(activeTraceCounts);
            activeTraceBos.add(activeTraceBo);
        }
        return activeTraceBos;
    }
}
