package com.walter.starry.ai.mcp.server.service;

import com.walter.starry.ai.mcp.server.entity.AclAuthorityItem;
import com.walter.starry.ai.mcp.server.remote.AclAuthorityItemRes;
import com.walter.starry.ai.mcp.server.remote.StarryInfoRes;
import com.walter.starry.ai.mcp.server.repository.AclAuthorityItemRepository;
import com.walter.starry.autoconfigure.mdc.ai.mcp.server.MdcMcpToolRequest;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.util.MdcUtil;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.mcp.annotation.McpProgressToken;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author walter.tan
 */
@Slf4j
@Service
public class StarryMcpService {

    private static final String STARRY_AUTHOR_UID = "PC9527";
    @Autowired
    private AclAuthorityItemRepository aclAuthorityItemRepository;

    @McpTool(description = "提供Starry系统的基本信息")
    public StarryInfoRes getStarryInfo(McpSyncServerExchange exchange, @McpProgressToken String progressToken, MdcMcpToolRequest req) {
        // 向mcp客户端发送服务端的日志信息
        exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .data("【McpServer-getStarryInfo】开始处理，progressToken: " + progressToken)
                .meta(Map.of(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId()))
                .build());

        log.info("getStarryInfo start");

        try{
            return new StarryInfoRes(STARRY_AUTHOR_UID, "Starry系统是一个基于Java和Vue3的综合性系统基座");
        }finally {
            // 向mcp客户端发送执行进度信息
            if(StringUtils.isNotBlank(progressToken)){
                exchange.progressNotification(new McpSchema.ProgressNotification(
                        progressToken, 1.0, 1.0, "【McpServer-getStarryInfo】处理完成",
                        Map.of(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId())));
            }
        }
    }

    @McpTool(description = "根据用户ID获取该用户的简介信息")
    public String getUserInfo(McpSyncRequestContext context, UserInfoReq req){
        // 向mcp客户端发送执行进度信息
        context.progress(p -> p.progress(0).total(1.0)
                .meta(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId())
                .message("【McpServer-getUserInfo】开始处理"));

        log.info("getUserInfo start. uid: {}", req.getUid());

        // 向mcp客户端发送服务端的日志信息
        context.log(log -> log
                .level(McpSchema.LoggingLevel.INFO)
                .message("【McpServer-getUserInfo】处理中: uid: %s, progressToken: %s, traceId: %s"
                        .formatted(req.getUid(), context.request().progressToken(), context.requestMeta().get(MdcUtil.ATTR_TRACE_ID)))
                .meta(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId()));

        try{
            if(STARRY_AUTHOR_UID.equals(req.getUid())){
                return "Walter.Tan，是一名资深的专业软件开发从业者，特别擅长互联网行业的软件架构和技术方案的设计";
            }else{
                return "暂未找到该用户的个人简介";
            }
        }finally {
            context.progress(p -> p.progress(1.0).total(1.0)
                    .meta(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId())
                    .message("【McpServer-getUserInfo】处理完成"));
        }
    }

    @McpTool(description = "按条件分页查询权限项配置表。条件与条件之间的关系是逻辑与。支持按主键、编码值或名称来进行查询")
    public List<AclAuthorityItemRes> pageQueryAclAuthorityItem(McpSyncRequestContext context, PageQueryAclAuthorityItemReq req){
        log.info("pageQueryAclAuthorityItem start. req: {}", JsonUtil.toJson(req));

        Specification<AclAuthorityItem> spec = (root, query, builder) -> {
            List<Predicate> andPredicates = new ArrayList<>();
            if(Objects.nonNull(req.getId())){
                andPredicates.add(builder.equal(root.get("id"), req.getId()));
            }
            if(StringUtils.isNotBlank(req.getCode())){
                andPredicates.add(builder.equal(root.get("code"), req.getCode()));
            }
            if(StringUtils.isNotBlank(req.getName())){
                andPredicates.add(builder.like(root.get("name"), "%" + req.getName() + "%"));
            }
            if(Objects.nonNull(req.getSystemAuthority())){
                andPredicates.add(builder.equal(root.get("systemAuthority"), req.getSystemAuthority()));
            }
            return builder.and(andPredicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(req.getPageNumber(), req.getPageSize(), Sort.by("id"));
        return aclAuthorityItemRepository.findAll(spec, pageable).stream().map(item -> {
            AclAuthorityItemRes res = new AclAuthorityItemRes();
            BeanUtils.copyProperties(item, res);
            return res;
        }).toList();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserInfoReq extends MdcMcpToolRequest {
        @McpToolParam(description = "用户ID")
        private String uid;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PageQueryAclAuthorityItemReq extends MdcMcpToolRequest {
        @McpToolParam(description = "物理主键", required = false)
        private Long id;

        @McpToolParam(description = "编码值", required = false)
        private String code;

        @McpToolParam(description = "名称（支持模糊查询）", required = false)
        private String name;

        @McpToolParam(description = "是否为系统权限（即无法修改）。0-否，1-是", required = false)
        private Boolean systemAuthority;

        @McpToolParam(description = "分页页码，0表示第1页，默认值为0", required = false)
        private Integer pageNumber = 0;

        @McpToolParam(description = "分页大小，默认值为5", required = false)
        private Integer pageSize = 5;
    }
}
