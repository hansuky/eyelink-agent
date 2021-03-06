package com.m2u.eyelink.collector.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpanEventBo implements Event {

	   // version 0 means that the type of prefix's size is int

	    private byte version = 0;

	    private short sequence;

	    private int startElapsed;
	    private int endElapsed;
	    private int elapsed;

	    private String rpc;
	    private short serviceType;
	    private String serviceTypeName;

	    private String destinationId;
	    private String endPoint;
	    private int apiId;

	    private List<AnnotationBo> annotationBoList;

	    private int depth = -1;
	    private long nextSpanId = -1;

	    private boolean hasException;
	    private int exceptionId;
	    private String exceptionMessage;

	    // should get exceptionClass from dao
	    private String exceptionClass;

	    private int asyncId = -1;
	    private int nextAsyncId = -1;
	    private short asyncSequence = -1;
	    
	    private long gap;
	    private long executionMilliseconds;
	    
	    public SpanEventBo() {
	    }


	    public byte getVersion() {
	        return version;
	    }

	    public void setVersion(byte version) {
	        this.version = version;
	    }

	    public short getSequence() {
	        return sequence;
	    }

	    public void setSequence(short sequence) {
	        this.sequence = sequence;
	    }

	    public int getStartElapsed() {
	        return startElapsed;
	    }

	    public void setStartElapsed(int startElapsed) {
	        this.startElapsed = startElapsed;
	    }

	    public int getEndElapsed() {
	        return endElapsed;
	    }

	    public void setEndElapsed(int endElapsed) {
	        this.endElapsed = endElapsed;
	    }

	    public String getRpc() {
	        return rpc;
	    }

	    public void setRpc(String rpc) {
	        this.rpc = rpc;
	    }

	    public short getServiceType() {
	        return serviceType;
	    }

	    public void setServiceType(short serviceType) {
	        this.serviceType = serviceType;
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

	    public String getDestinationId() {
	        return destinationId;
	    }

	    public void setDestinationId(String destinationId) {
	        this.destinationId = destinationId;
	    }


	    public List<AnnotationBo> getAnnotationBoList() {
	        return annotationBoList;
	    }

	    public int getDepth() {
	        return depth;
	    }

	    public void setDepth(int depth) {
	        this.depth = depth;
	    }

	    public long getNextSpanId() {
	        return nextSpanId;
	    }

	    public void setNextSpanId(long nextSpanId) {
	        this.nextSpanId = nextSpanId;
	    }


	    public void setAnnotationBoList(List<AnnotationBo> annotationList) {
	        if (annotationList == null) {
	            return;
	        }
	        this.annotationBoList = annotationList;
	    }
	    
	    public boolean isAsync() {
	        return this.asyncId != -1;
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

	    public String getExceptionClass() {
	        return exceptionClass;
	    }

	    public void setExceptionInfo(int exceptionId, String exceptionMessage) {
	        this.hasException = true;
	        this.exceptionId = exceptionId;
	        this.exceptionMessage = exceptionMessage;
	    }


	    public void setExceptionClass(String exceptionClass) {
	        this.exceptionClass = exceptionClass;
	    }

	    public int getAsyncId() {
	        return asyncId;
	    }

	    public void setAsyncId(int asyncId) {
	        this.asyncId = asyncId;
	    }

	    public int getNextAsyncId() {
	        return nextAsyncId;
	    }

	    public void setNextAsyncId(int nextAsyncId) {
	        this.nextAsyncId = nextAsyncId;
	    }
	    
	    public short getAsyncSequence() {
	        return asyncSequence;
	    }

	    public void setAsyncSequence(short asyncSequence) {
	        this.asyncSequence = asyncSequence;
	    }


	    @Override
	    public String toString() {
	        StringBuilder builder = new StringBuilder();
	        builder.append("{version=");
	        builder.append(version);
	        builder.append(", sequence=");
	        builder.append(sequence);
	        builder.append(", startElapsed=");
	        builder.append(startElapsed);
	        builder.append(", endElapsed=");
	        builder.append(endElapsed);
	        builder.append(", rpc=");
	        builder.append(rpc);
	        builder.append(", serviceType=");
	        builder.append(serviceType);
	        builder.append(", destinationId=");
	        builder.append(destinationId);
	        builder.append(", endPoint=");
	        builder.append(endPoint);
	        builder.append(", apiId=");
	        builder.append(apiId);
	        builder.append(", annotationBoList=");
	        builder.append(annotationBoList);
	        builder.append(", depth=");
	        builder.append(depth);
	        builder.append(", nextSpanId=");
	        builder.append(nextSpanId);
	        builder.append(", hasException=");
	        builder.append(hasException);
	        builder.append(", exceptionId=");
	        builder.append(exceptionId);
	        builder.append(", exceptionMessage=");
	        builder.append(exceptionMessage);
	        builder.append(", exceptionClass=");
	        builder.append(exceptionClass);
	        builder.append(", asyncId=");
	        builder.append(asyncId);
	        builder.append(", nextAsyncId=");
	        builder.append(nextAsyncId);
	        builder.append(", asyncSequence=");
	        builder.append(asyncSequence);
	        builder.append(", gap=");
	        builder.append(gap);
	        builder.append(", executionTime=");
	        builder.append(executionMilliseconds);
	        builder.append("}");
	        return builder.toString();
	    }


		public Map<String, Object> getMap() {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("version", this.version);
			map.put("sequence", this.sequence);
			map.put("startElapsed", this.startElapsed);
			map.put("rpc", this.rpc);
			map.put("serviceType", this.serviceType);
			map.put("serviceTypeName", this.serviceTypeName);
			map.put("destinationId", this.destinationId);
			map.put("endPoint", this.endPoint);
			map.put("apiId", this.apiId);
			List<Map<String,Object>> listAnnotationBo = new ArrayList<Map<String,Object>>();
			for(int i = 0; i < this.annotationBoList.size(); i++) {
				AnnotationBo annotationBo = this.annotationBoList.get(i);
				listAnnotationBo.add(annotationBo.getMap());
			}
			map.put("annotationBoList", listAnnotationBo);			
//			map.put("annotationBoListOrg", this.annotationBoList);
			map.put("depth", this.depth);
			map.put("nextSpanId", this.nextSpanId);
			map.put("hasException", this.hasException);
			map.put("exceptionId", this.exceptionId);
			map.put("exceptionMessage", this.exceptionMessage);
			map.put("exceptionClass", this.exceptionClass);
			map.put("asyncId", this.asyncId);
			map.put("nextAsyncId", this.nextAsyncId);
			map.put("asyncSequence", this.asyncSequence);
			map.put("gap", this.gap);
			map.put("executionTime", this.executionMilliseconds);
			map.put("elapsed", this.elapsed);
			return map;
		}


		public String getServiceTypeName() {
			return serviceTypeName;
		}


		public void setServiceTypeName(String serviceTypeName) {
			this.serviceTypeName = serviceTypeName;
		}


		public long getGap() {
			return gap;
		}


		public void setGap(long gap) {
			this.gap = gap;
		}


		public long getExecutionMilliseconds() {
			return executionMilliseconds;
		}


		public void setExecutionMilliseconds(long executionMilliseconds) {
			this.executionMilliseconds = executionMilliseconds;
		}


		public int getElapsed() {
			return elapsed;
		}


		public void setElapsed(int elapsed) {
			this.elapsed = elapsed;
		}	    
	    
	}
