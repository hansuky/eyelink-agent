package com.m2u.eyelink.collector.bo;

import java.util.HashMap;
import java.util.Map;

import com.m2u.eyelink.collector.util.RowKeyUtils;
import com.m2u.eyelink.collector.util.TimeUtils;
import com.m2u.eyelink.util.BytesUtils;

import com.m2u.eyelink.common.ELAgentConstants;

public class ApiMetaDataBo {
	private String agentId;
	private long startTime;

	private int apiId;

	private String apiInfo;
	private int lineNumber;
	private MethodTypeEnum methodTypeEnum = MethodTypeEnum.DEFAULT;
	private int type;

	public ApiMetaDataBo() {
	}

	public ApiMetaDataBo(String agentId, long startTime, int apiId) {
		if (agentId == null) {
			throw new NullPointerException("agentId must not be null");
		}

		this.agentId = agentId;
		this.startTime = startTime;
		this.apiId = apiId;
		this.type = 0;
	}

	public ApiMetaDataBo(String agentId, long startTime, int apiId, int type) {
		if (agentId == null) {
			throw new NullPointerException("agentId must not be null");
		}

		this.agentId = agentId;
		this.startTime = startTime;
		this.apiId = apiId;
		this.type = type;
	}

	public ApiMetaDataBo(String agentId, long startTime, int apiId, String apiInfo, int type) {
		if (agentId == null) {
			throw new NullPointerException("agentId must not be null");
		}

		this.agentId = agentId;
		this.startTime = startTime;
		this.apiId = apiId;
		this.apiInfo = apiInfo;
		this.type = type;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public int getApiId() {
		return apiId;
	}

	public void setApiId(int apiId) {
		this.apiId = apiId;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public String getApiInfo() {
		return apiInfo;
	}

	public void setApiInfo(String apiInfo) {
		this.apiInfo = apiInfo;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public MethodTypeEnum getMethodTypeEnum() {
		return methodTypeEnum;
	}

	public void setMethodTypeEnum(MethodTypeEnum methodTypeEnum) {
		if (methodTypeEnum == null) {
			throw new NullPointerException("methodTypeEnum must not be null");
		}
		this.methodTypeEnum = methodTypeEnum;
	}

	public String getDescription() {
		if (lineNumber != -1) {
			return apiInfo + ":" + lineNumber;
		}

		return apiInfo;
	}

	public void readRowKey(byte[] bytes) {
		this.agentId = BytesUtils.safeTrim(BytesUtils.toString(bytes, 0, ELAgentConstants.AGENT_NAME_MAX_LEN));
		this.startTime = TimeUtils.recoveryTimeMillis(readTime(bytes));
		this.apiId = readKeyCode(bytes);
	}

	private static long readTime(byte[] rowKey) {
		return BytesUtils.bytesToLong(rowKey, ELAgentConstants.AGENT_NAME_MAX_LEN);
	}

	private static int readKeyCode(byte[] rowKey) {
		return BytesUtils.bytesToInt(rowKey, ELAgentConstants.AGENT_NAME_MAX_LEN + BytesUtils.LONG_BYTE_LENGTH);
	}

	public byte[] toRowKey() {
		return RowKeyUtils.getMetaInfoRowKey(this.agentId, this.startTime, this.apiId);
	}

	@Override
	public String toString() {
		return "ApiMetaDataBo{" + "agentId='" + agentId + '\'' + ", apiId=" + apiId + ", startTime=" + startTime
				+ ", apiInfo='" + apiInfo + '\'' + ", lineNumber=" + lineNumber + '}';
	}

	public Map<String, Object> getMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("agentId", this.agentId);
		map.put("agentStartTime", TimeUtils.convertEpochToDate(this.startTime));
		map.put("apiId", this.apiId);
		map.put("apiInfo", this.apiInfo);
		map.put("line", this.lineNumber);
		map.put("type", this.type);
		return map;
	}
}
