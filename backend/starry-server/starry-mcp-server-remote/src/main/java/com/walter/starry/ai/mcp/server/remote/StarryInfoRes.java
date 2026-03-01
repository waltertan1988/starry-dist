package com.walter.starry.ai.mcp.server.remote;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @author walter.tan
 */
public record StarryInfoRes(@JsonPropertyDescription("Starry系统的作者的用户ID") String authorUid, @JsonPropertyDescription ("Starry系统的介绍") String desc){}
