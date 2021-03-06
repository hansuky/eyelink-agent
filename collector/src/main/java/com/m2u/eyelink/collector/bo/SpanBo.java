package com.m2u.eyelink.collector.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.m2u.eyelink.collector.util.TimeUtils;
import com.m2u.eyelink.util.TransactionId;

public class SpanBo implements Event, BasicSpan {

    // version 0 means that the type of prefix's size is int
    private byte version = 0;

//  private AgentKeyBo agentKeyBo;
    private String agentId;
    private String applicationId;
    private long agentStartTime;

    private TransactionId transactionId;

    private long spanId;
    private long parentSpanId;

    private String parentApplicationId;
    private short parentApplicationServiceType;
    private String parentApplicationServiceTypeName;

    private long startTime;
    private int elapsed;

    private String rpc;
    private short serviceType;
    private String serviceTypeName;
    private String endPoint;
    private int apiId;

    private List<AnnotationBo> annotationBoList = new ArrayList<AnnotationBo>();
    private short flag; // optional
    private int errCode;

    private List<SpanEventBo> spanEventBoList = new ArrayList<SpanEventBo>();

    private long collectorAcceptTime;

    private boolean hasException = false;
    private int exceptionId;
    private String exceptionMessage;
    private String exceptionClass;
    
    private Short applicationServiceType;
    private String applicationServiceTypeName;

    private String acceptorHost;
    private String remoteAddr; // optional

    private byte loggingTransactionInfo; //optional




    public SpanBo() {
    }

    public int getVersion() {
        return version & 0xFF;
    }


    public byte getRawVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version < 0 || version > 255) {
            throw new IllegalArgumentException("out of range (0~255)");
        }
        // check range
        this.version = (byte) (version & 0xFF);
    }

    public TransactionId getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(TransactionId transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getTranscationFullId() {
    		return this.transactionId.getAgentId() + "^" + this.transactionId.getAgentStartTime() + "^" + this.transactionId.getTransactionSequence();
    }
    
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public long getAgentStartTime() {
        return agentStartTime;
    }

    public void setAgentStartTime(long agentStartTime) {
        this.agentStartTime = agentStartTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }


    public int getElapsed() {
        return elapsed;
    }

    public void setElapsed(int elapsed) {
        this.elapsed = elapsed;
    }


    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }


    public long getSpanId() {
        return spanId;
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public long getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public short getFlag() {
        return flag;
    }

    public void setFlag(short flag) {
        this.flag = flag;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public int getApiId() {
        return apiId;
    }

    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    public List<AnnotationBo> getAnnotationBoList() {
        return annotationBoList;
    }


    public void setAnnotationBoList(List<AnnotationBo> anoList) {
        if (anoList == null) {
            return;
        }
        this.annotationBoList = anoList;
    }

    public void addSpanEventBoList(List<SpanEventBo> spanEventBoList) {
        if (spanEventBoList == null) {
            return;
        }
        this.spanEventBoList.addAll(spanEventBoList);
    }


    public void addSpanEvent(SpanEventBo spanEventBo) {
        if (spanEventBo == null) {
            return;
        }
        spanEventBoList.add(spanEventBo);
    }

    public List<SpanEventBo> getSpanEventBoList() {
        return spanEventBoList;
    }

    public short getServiceType() {
        return serviceType;
    }

    public void setServiceType(short serviceType) {
        this.serviceType = serviceType;
    }
    
    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getAcceptorHost() {
        return acceptorHost;
    }

    public void setAcceptorHost(String acceptorHost) {
        this.acceptorHost = acceptorHost;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public long getCollectorAcceptTime() {
        return collectorAcceptTime;
    }

    public void setCollectorAcceptTime(long collectorAcceptTime) {
        this.collectorAcceptTime = collectorAcceptTime;
    }

    public boolean isRoot() {
        return -1L == parentSpanId;
    }

    public boolean hasException() {
        return hasException;
    }

    public int getExceptionId() {
        return exceptionId;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionInfo(int exceptionId, String exceptionMessage) {
        this.hasException = true;
        this.exceptionId = exceptionId;
        this.exceptionMessage = exceptionMessage;
    }


    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
    
    public void setApplicationServiceType(Short applicationServiceType) {
        this.applicationServiceType  = applicationServiceType;
    }

    public boolean hasApplicationServiceType() {
        return applicationServiceType != null;
    }

    public short getApplicationServiceType() {
        if (hasApplicationServiceType()) {
            return this.applicationServiceType;
        } else {
            return this.serviceType;
        }
    }

    public String getParentApplicationId() {
        return parentApplicationId;
    }

    public void setParentApplicationId(String parentApplicationId) {
        this.parentApplicationId = parentApplicationId;
    }

    public short getParentApplicationServiceType() {
        return parentApplicationServiceType;
    }

    public void setParentApplicationServiceType(short parentApplicationServiceType) {
        this.parentApplicationServiceType = parentApplicationServiceType;
    }

    /**
     * @see com.navercorp.pinpoint.common.trace.LoggingInfo
     * @return loggingInfo key
     */
    public byte getLoggingTransactionInfo() {
        return loggingTransactionInfo;
    }


    public void setLoggingTransactionInfo(byte loggingTransactionInfo) {
        this.loggingTransactionInfo = loggingTransactionInfo;
    }


    @Override
    public String toString() {
        return "SpanBo{" +
                "version=" + version +
                ", agentId='" + agentId + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", agentStartTime=" + agentStartTime +
                ", transactionId=" + transactionId +
                ", spanId=" + spanId +
                ", parentSpanId=" + parentSpanId +
                ", parentApplicationId='" + parentApplicationId + '\'' +
                ", parentApplicationServiceType=" + parentApplicationServiceType +
                ", startTime=" + startTime +
                ", elapsed=" + elapsed +
                ", rpc='" + rpc + '\'' +
                ", serviceType=" + serviceType +
                ", endPoint='" + endPoint + '\'' +
                ", apiId=" + apiId +
                ", annotationBoList=" + annotationBoList +
                ", flag=" + flag +
                ", errCode=" + errCode +
                ", spanEventBoList=" + spanEventBoList +
                ", collectorAcceptTime=" + collectorAcceptTime +
                ", hasException=" + hasException +
                ", exceptionId=" + exceptionId +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                ", exceptionClass='" + exceptionClass + '\'' +
                ", applicationServiceType=" + applicationServiceType +
                ", acceptorHost='" + acceptorHost + '\'' +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", loggingTransactionInfo=" + loggingTransactionInfo +
                '}';
    }
    
	public Map<String, Object> getMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("version", this.version);
		map.put("agentId", this.agentId);
		map.put("applicationId", this.applicationId);
		map.put("agentStartTime", TimeUtils.convertEpochToDate(this.agentStartTime));
		map.put("transactionId", getTranscationFullId());
		map.put("spanId", this.spanId);
		map.put("parentSpanId", this.parentSpanId);
		map.put("parentApplicationId", this.parentApplicationId);
		map.put("parentApplicationServiceType", this.parentApplicationServiceType);
		map.put("parentApplicationServiceTypeName", this.parentApplicationServiceTypeName);
		map.put("startTime", TimeUtils.convertEpochToDate(this.startTime));
		map.put("elapsed", this.elapsed);
		map.put("rpc", this.rpc);
		map.put("serviceType", this.serviceType);
		map.put("serviceTypeName", this.serviceTypeName);
		map.put("endPoint", this.endPoint);
		map.put("apiId", this.apiId);
//		map.put("annotationBoListOrg", this.annotationBoList);
		List<Map<String,Object>> listAnnotationBo = new ArrayList<Map<String,Object>>();
		for(int i = 0; i < this.annotationBoList.size(); i++) {
			AnnotationBo annotationBo = this.annotationBoList.get(i);
			listAnnotationBo.add(annotationBo.getMap());
		}
		map.put("annotationBoList", listAnnotationBo);
		map.put("flag", this.flag);
		map.put("errCode", this.errCode);

		List<Map<String,Object>> listEventBo = new ArrayList<Map<String,Object>>();
		for(int i = 0; i < this.spanEventBoList.size(); i++) {
			SpanEventBo eventBo = this.spanEventBoList.get(i);
			listEventBo.add(eventBo.getMap());
		}
//		map.put("spanEventBoListOrg", this.spanEventBoList);
		map.put("spanEventBoList", listEventBo);
		map.put("collectorAcceptTime", TimeUtils.convertEpochToDate(this.collectorAcceptTime));
		map.put("hasException", this.hasException);
		map.put("exceptionId", this.exceptionId);
		map.put("exceptionMessage", this.exceptionMessage);
		map.put("exceptionClass", this.exceptionClass);
		map.put("agentStartTime", TimeUtils.convertEpochToDate(this.agentStartTime));
		map.put("applicationServiceType", this.applicationServiceType);
		map.put("applicationServiceTypeName", this.applicationServiceTypeName);
		map.put("acceptorHost", this.acceptorHost);
		map.put("remoteAddr", this.remoteAddr);
		map.put("loggingTransactionInfo", this.loggingTransactionInfo);
		return map;
	}

	public String getServiceTypeName() {
		return serviceTypeName;
	}

	public void setServiceTypeName(String serviceTypeName) {
		this.serviceTypeName = serviceTypeName;
	}

	public String getApplicationServiceTypeName() {
		return applicationServiceTypeName;
	}

	public void setApplicationServiceTypeName(String applicationServiceTypeName) {
		this.applicationServiceTypeName = applicationServiceTypeName;
	}

	public String getParentApplicationServiceTypeName() {
		return parentApplicationServiceTypeName;
	}

	public void setParentApplicationServiceTypeName(String parentApplicationServiceTypeName) {
		this.parentApplicationServiceTypeName = parentApplicationServiceTypeName;
	}
}
