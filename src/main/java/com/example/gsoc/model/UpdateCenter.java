package com.example.gsoc.model;

import com.fasterxml.jackson.databind.JsonNode;

public record UpdateCenter(String connectionCheckUrl, String generationTimestamp, String id, JsonNode core, JsonNode deprecations, JsonNode plugins, JsonNode signature, JsonNode updateCenterVersion, JsonNode warnings) {

	public UpdateCenter {
		
	}

	public UpdateCenter(String connectionCheckUrl, String generationTimestamp, String id) {
		this(connectionCheckUrl, generationTimestamp, id, null, null, null, null, null, null);
	}

}
