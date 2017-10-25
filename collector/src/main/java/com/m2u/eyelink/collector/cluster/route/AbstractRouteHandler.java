package com.m2u.eyelink.collector.cluster.route;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.m2u.eyelink.collector.cluster.ClusterPointLocator;
import com.m2u.eyelink.collector.cluster.TargetClusterPoint;
import com.m2u.eyelink.context.thrift.TCommandTransfer;

public abstract class AbstractRouteHandler<T extends RouteEvent> implements RouteHandler<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ClusterPointLocator<TargetClusterPoint> targetClusterPointLocator;

    public AbstractRouteHandler(ClusterPointLocator<TargetClusterPoint> targetClusterPointLocator) {
        this.targetClusterPointLocator = targetClusterPointLocator;
    }

    protected TargetClusterPoint findClusterPoint(TCommandTransfer deliveryCommand) {
        String applicationName = deliveryCommand.getApplicationName();
        String agentId = deliveryCommand.getAgentId();
        long startTimeStamp = deliveryCommand.getStartTime();

        List<TargetClusterPoint> result = new ArrayList<>();

        for (TargetClusterPoint targetClusterPoint : targetClusterPointLocator.getClusterPointList()) {
            if (!targetClusterPoint.getApplicationName().equals(applicationName)) {
                continue;
            }

            if (!targetClusterPoint.getAgentId().equals(agentId)) {
                continue;
            }

            if (!(targetClusterPoint.getStartTimeStamp() == startTimeStamp)) {
                continue;
            }

            result.add(targetClusterPoint);
        }

        if (result.size() == 1) {
            return result.get(0);
        }

        if (result.size() > 1) {
            logger.warn("Ambiguous ClusterPoint {}, {}, {} (Valid Agent list={}).", applicationName, agentId, startTimeStamp, result);
            return null;
        }

        return null;
    }

}
