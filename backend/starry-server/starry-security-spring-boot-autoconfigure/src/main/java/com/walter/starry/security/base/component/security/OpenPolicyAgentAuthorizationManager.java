package com.walter.starry.security.base.component.security;

import com.google.common.collect.Lists;
import com.walter.starry.security.base.config.SecurityConfig;
import com.walter.starry.security.base.entity.AclAuthorityItem;
import com.walter.starry.security.base.entity.AclAuthorityResource;
import com.walter.starry.security.base.entity.AclResourceItem;
import com.walter.starry.security.base.repository.AclAuthorityItemRepository;
import com.walter.starry.security.base.repository.AclAuthorityResourceRepository;
import com.walter.starry.security.base.repository.AclResourceItemRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authorization.*;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 独立式授权管理器（数据库配置方式）
 * @author: walter.tan
 * @datetime: 2023/9/17 17:24
 */
@Slf4j
public class OpenPolicyAgentAuthorizationManager extends AbstractRequestMatcherRegistry<List<RequestMatcher>> implements AuthorizationManager<RequestAuthorizationContext> {

    private final Log logger = LogFactory.getLog(getClass());

    public static final String HTTP_METHOD_LIST_DELIMITER = ",";

    private static final AuthorizationManager<RequestAuthorizationContext> permitAllAuthorizationManager = (a, o) -> new AuthorizationDecision(true);

    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);

    private final Object requestMatcherEntryHolderLock = new Object();
    private volatile RequestMatcherEntryHolder requestMatcherEntryHolder = null;

    /**
     * 定义层次角色，参考：https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-hierarchical-roles
     */
    @Getter
    private final RoleHierarchy roleHierarchy = new DelegatedRoleHierarchy();
    private volatile boolean isRoleHierarchyRefreshed = false;

    private final AclAuthorityItemRepository aclAuthorityItemRepository;

    private final AclResourceItemRepository aclResourceItemRepository;

    private final AclAuthorityResourceRepository aclAuthorityResourceRepository;

    public OpenPolicyAgentAuthorizationManager(AclAuthorityItemRepository aclAuthorityItemRepository, AclResourceItemRepository aclResourceItemRepository, AclAuthorityResourceRepository aclAuthorityResourceRepository) {
        this.aclAuthorityItemRepository = aclAuthorityItemRepository;
        this.aclResourceItemRepository = aclResourceItemRepository;
        this.aclAuthorityResourceRepository = aclAuthorityResourceRepository;
    }

    /**
     * 刷新层次角色关系
     */
    public void refreshRoleHierarchy(){
        this.isRoleHierarchyRefreshed = false;
        log.info("refreshRoleHierarchy done");
    }

    private void refreshRoleHierarchy0(){
        List<String> pairList = Lists.newArrayList();
        List<AclAuthorityItem> itemList = aclAuthorityItemRepository.findAll();
        for (AclAuthorityItem item : itemList) {
            if(StringUtils.isBlank(item.getParentCode()) || Objects.equals(item.getCode(), item.getParentCode())){
                continue;
            }
            pairList.add(item.getParentCode() + " > " + item.getCode());
        }

        if(CollectionUtils.isEmpty(pairList)){
            return;
        }

        ((DelegatedRoleHierarchy)roleHierarchy).setRoleHierarchy(RoleHierarchyImpl.fromHierarchy(String.join("\n", pairList)));

        this.isRoleHierarchyRefreshed = true;

        log.info("refreshRoleHierarchy0 done");
    }

    /**
     * 刷新所有权限与资源的关联关系
     */
    public void refreshRequestMatcherEntryHolder() {
        RequestMatcherEntryHolder newHolder = new RequestMatcherEntryHolder();

        // 创建permitAll的url请求的匹配列表，并关联到permitAll授权管理器
        this.access(newHolder, permitAllAuthorizationManager, SecurityConfig.permitRequestUrlPatterns());

        // 创建数据库里自定义的url请求的匹配列表，并关联到角色授权管理器
        this.refreshRequestMatcherEntryHolderFromDb(newHolder);

        // 其他任意请求，关联到"已认证"授权管理器
        newHolder.nullHttpMethodMappingList.add(new RequestMatcherEntry<>(AnyRequestMatcher.INSTANCE, AuthenticatedAuthorizationManager.authenticated()));

        this.requestMatcherEntryHolder = newHolder;

        log.info("refreshRequestMatcherEntryHolder done");
    }

    private void refreshRequestMatcherEntryHolderFromDb(RequestMatcherEntryHolder holder){
        int pageNo = 0;
        final int pageSize = 10;
        final Sort aclResourceItemPageSort = Sort.by("seq").ascending().and(Sort.by("id").ascending());
        Page<AclResourceItem> aclResourceItemPage = aclResourceItemRepository.findAll(PageRequest.of(pageNo++, pageSize, aclResourceItemPageSort));
        while (aclResourceItemPage.hasContent()){
            List<String> resourceItemCodeList = aclResourceItemPage.getContent().stream().map(AclResourceItem::getCode).distinct().toList();
            Map<String, List<AclAuthorityResource>> resourceAuthoritiesMap = aclAuthorityResourceRepository.findByResourceItemCodeIn(resourceItemCodeList)
                    .stream().collect(Collectors.groupingBy(AclAuthorityResource::getResourceItemCode));

            for (AclResourceItem aclResourceItem : aclResourceItemPage.getContent()) {
                List<AclAuthorityResource> authorityResourceList = resourceAuthoritiesMap.get(aclResourceItem.getCode());
                if(CollectionUtils.isEmpty(authorityResourceList)){
                    continue;
                }
                
                String[] authorityItemCodes = authorityResourceList.stream().map(AclAuthorityResource::getAuthorityItemCode).toList().toArray(new String[0]);
                for (String httpMethod : aclResourceItem.getHttpMethodList().split(HTTP_METHOD_LIST_DELIMITER)) {
                    String pattern = aclResourceItem.getPattern();
                    this.access(holder, AuthorityAuthorizationManager.hasAnyAuthority(authorityItemCodes), HttpMethod.valueOf(httpMethod), pattern);
                }
            }

            aclResourceItemPage = aclResourceItemRepository.findAll(PageRequest.of(pageNo++, pageSize, aclResourceItemPageSort));
        }
    }

    private void access(RequestMatcherEntryHolder holder, AuthorizationManager<RequestAuthorizationContext> manager, String... patterns){
        this.access(holder, manager, null, patterns);
    }

    /**
     * 把RequestMatcher关联到AuthorizationManager
     * @param holder
     * @param manager
     * @param httpMethod
     * @param patterns
     */
    private void access(RequestMatcherEntryHolder holder, AuthorizationManager<RequestAuthorizationContext> manager, HttpMethod httpMethod, String... patterns){
        Assert.notNull(manager, "manager cannot be null");

        List<? extends RequestMatcher> matchers = super.requestMatchers(httpMethod, patterns);

        if(Objects.isNull(httpMethod)){
            matchers.forEach(matcher -> holder.nullHttpMethodMappingList.add(new RequestMatcherEntry<>(matcher, withRoleHierarchy(manager))));
        }else{
            List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> mappingList = holder.httpMethodMappingListMap.computeIfAbsent(httpMethod, httpmethod -> new ArrayList<>());
            matchers.forEach(matcher -> mappingList.add(new RequestMatcherEntry<>(matcher, withRoleHierarchy(manager))));
        }
    }

    private AuthorizationManager<RequestAuthorizationContext> withRoleHierarchy(
            AuthorizationManager<RequestAuthorizationContext> manager) {
        if(manager instanceof AuthorityAuthorizationManager<RequestAuthorizationContext> mgr){
            mgr.setRoleHierarchy(roleHierarchy);
        }
        return manager;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        // 访问资源时，检查是否已授权

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(LogMessage.format("Authorizing %s", context.getRequest()));
        }

        // 刷新层次角色
        if(!this.isRoleHierarchyRefreshed){
            synchronized (roleHierarchy){
                if(!this.isRoleHierarchyRefreshed){
                    this.refreshRoleHierarchy0();
                }
            }
        }

        // 刷新所有权限与资源的关联关系
        if(Objects.isNull(this.requestMatcherEntryHolder)){
            synchronized (requestMatcherEntryHolderLock){
                if(Objects.isNull(this.requestMatcherEntryHolder)){
                    this.refreshRequestMatcherEntryHolder();
                }
            }
        }

        // 优先在指定了HttpMethod的匹配项中查找
        HttpMethod httpMethod = HttpMethod.valueOf(context.getRequest().getMethod());
        List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> mappings = this.requestMatcherEntryHolder.httpMethodMappingListMap.get(httpMethod);
        if(CollectionUtils.isNotEmpty(mappings)){
            for (RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>> mapping : mappings) {
                RequestMatcher matcher = mapping.getRequestMatcher();
                RequestMatcher.MatchResult matchResult = matcher.matcher(context.getRequest());
                if (matchResult.isMatch()) {
                    AuthorizationManager<RequestAuthorizationContext> manager = mapping.getEntry();
                    if (this.logger.isTraceEnabled()) {
                        this.logger.trace(LogMessage.format("Checking authorization on %s using %s", context.getRequest(), manager));
                    }
                    return manager.authorize(authentication,
                            new RequestAuthorizationContext(context.getRequest(), matchResult.getVariables()));
                }
            }
        }

        // 在未指定了HttpMethod的匹配项中查找
        for (RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>> mapping : this.requestMatcherEntryHolder.nullHttpMethodMappingList) {
            RequestMatcher matcher = mapping.getRequestMatcher();
            RequestMatcher.MatchResult matchResult = matcher.matcher(context.getRequest());
            if (matchResult.isMatch()) {
                AuthorizationManager<RequestAuthorizationContext> manager = mapping.getEntry();
                if (this.logger.isTraceEnabled()) {
                    this.logger.trace(LogMessage.format("Checking authorization on %s using %s", context.getRequest(), manager));
                }
                return manager.authorize(authentication,
                        new RequestAuthorizationContext(context.getRequest(), matchResult.getVariables()));
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(LogMessage.of(() -> "Denying request since did not find matching RequestMatcher"));
        }

        return DENY;
    }

    @Override
    protected List<RequestMatcher> chainRequestMatchers(List<RequestMatcher> requestMatchers) {
        return requestMatchers;
    }

    /**
     * 把HTTP Method集合进行排序，并转换为用{@link #HTTP_METHOD_LIST_DELIMITER}分隔的字符串
     * @param httpMethods
     * @return
     */
    public String httpMethodsToSortedString(Collection<String> httpMethods){
        Assert.notEmpty(httpMethods, "httpMethods cannot be empty");
        return String.join(HTTP_METHOD_LIST_DELIMITER, httpMethods.stream().sorted().toList().toArray(new String[0]));
    }

    private static class RequestMatcherEntryHolder {
        /**
         * 指定httpMethod的RequestMatcherEntry
         */
        private final Map<HttpMethod, List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>>> httpMethodMappingListMap = new HashMap<>();
        /**
         * 未指定httpMethod的RequestMatcherEntry
         */
        private final List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> nullHttpMethodMappingList = new ArrayList<>();
    }

    /**
     * 层次角色的代理
     */
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DelegatedRoleHierarchy implements RoleHierarchy {

        private volatile RoleHierarchy roleHierarchy;

        @Override
        public Collection<? extends GrantedAuthority> getReachableGrantedAuthorities(Collection<? extends GrantedAuthority> authorities) {
            return roleHierarchy.getReachableGrantedAuthorities(authorities);
        }
    }
}
