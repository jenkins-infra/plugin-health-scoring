package com.example.gsoc.model;

import com.fasterxml.jackson.databind.JsonNode;

public class UpdateCenter {

	String connectionCheckUrl;
	String generationTimestamp;
	String id;
	JsonNode core;
	JsonNode deprecations;
	JsonNode plugins;
	JsonNode signature;
	JsonNode updateCenterVersion;
	JsonNode warnings;

	public UpdateCenter() {
		
	}

	public UpdateCenter(String connectionCheckUrl, String generationTimestamp, String id) {
		this.connectionCheckUrl = connectionCheckUrl;
		this.generationTimestamp = generationTimestamp;
		this.id = id;
	}
	
	public String getConnectionCheckUrl() {
		return connectionCheckUrl;
	}
	public void setConnectionCheckUrl(String connectionCheckUrl) {
		this.connectionCheckUrl = connectionCheckUrl;
	}
	public JsonNode getCore() {
		return core;
	}
	public void setCore(JsonNode core) {
		this.core = core;
	}
	public String getGenerationTimestamp() {
		return generationTimestamp;
	}
	public void setGenerationTimestamp(String generationTimestamp) {
		this.generationTimestamp = generationTimestamp;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public JsonNode getDeprecations() {
		return deprecations;
	}

	public void setDeprecations(JsonNode deprecations) {
		this.deprecations = deprecations;
	}

	public JsonNode getPlugins() {
		return plugins;
	}

	public void setPlugins(JsonNode plugins) {
		this.plugins = plugins;
	}

	public JsonNode getSignature() {
		return signature;
	}

	public void setSignature(JsonNode signature) {
		this.signature = signature;
	}

	public JsonNode getUpdateCenterVersion() {
		return updateCenterVersion;
	}

	public void setUpdateCenterVersion(JsonNode updateCenterVersion) {
		this.updateCenterVersion = updateCenterVersion;
	}

	public JsonNode getWarnings() {
		return warnings;
	}

	public void setWarnings(JsonNode warnings) {
		this.warnings = warnings;
	}

}
