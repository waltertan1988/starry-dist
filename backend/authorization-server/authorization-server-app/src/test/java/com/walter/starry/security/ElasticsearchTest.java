package com.walter.starry.security;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.google.common.collect.Lists;
import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.bo.elasticsearch.EsUser;
import com.walter.starry.security.base.common.enums.ElasticsearchIndexAliasEnum;
import com.walter.starry.security.base.entity.AclAuthority;
import com.walter.starry.security.base.entity.AclUser;
import com.walter.starry.security.base.repository.AclAuthorityRepository;
import com.walter.starry.security.base.repository.AclUserRepository;
import com.walter.starry.security.base.util.JsonUtil;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-09 19:22:15
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class ElasticsearchTest {
    private final static Logger logger = LoggerFactory.getLogger(ElasticsearchTest.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @Autowired
    private AclUserRepository aclUserRepository;
    @Autowired
    private AclAuthorityRepository aclAuthorityRepository;

    @Nested
    class InfoTest {
        @Test
        void info() throws IOException {
            InfoResponse info = elasticsearchClient.info();
            logger.info("elasticsearch info. clusterName:{}, clusterUuid:{}", info.clusterName(), info.clusterUuid());
        }
    }

    @Nested
    class DocumentTest {
        /**
         * 索引单个用户文档
         * @throws IOException
         */
        @Test
        void index() throws IOException {
            AclUser aclUserExample = new AclUser();
            aclUserExample.setUsername("director");
            AclUser aclUser = aclUserRepository.findOne(Example.of(aclUserExample)).orElseThrow();
            EsUser esUser = new EsUser();
            BeanUtils.copyProperties(aclUser, esUser);

            AclAuthority aclAuthorityExample = new AclAuthority();
            List<EsUser.EsUserAuthority> esUserAuthorityList = aclAuthorityRepository.findAll(Example.of(aclAuthorityExample)).stream().map(po -> {
                EsUser.EsUserAuthority res = new EsUser.EsUserAuthority();
                BeanUtils.copyProperties(po, res);
                return res;
            }).toList();
            esUser.setAuthorities(esUserAuthorityList);

            System.out.println(JsonUtil.toJson(esUser));

            IndexResponse response = elasticsearchClient.index(i -> i
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .document(esUser)
            );
            logger.info("Indexed with version " + response.version());
        }

        /**
         * 批量索引用户信息
         */
        @Test
        void bulkIndex() throws IOException {
            final AtomicReference<String> lastUsernameRef = new AtomicReference<>(StringUtils.EMPTY);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());
            Specification<AclUser> aclUserSpec = (root, query, builder) -> {
                List<Predicate> andPredicates = new ArrayList<>();
                andPredicates.add(builder.greaterThan(root.get("username"), lastUsernameRef.get()));
                return builder.and(andPredicates.toArray(new Predicate[0]));
            };
            List<AclUser> aclUserList = aclUserRepository.findAll(aclUserSpec, pageable).getContent();
            while (CollectionUtils.isNotEmpty(aclUserList)){
                List<String> usernameList = aclUserList.stream().map(AclUser::getUsername).toList();

                Specification<AclAuthority> aclAuthoritySpec = (root, query, builder) -> {
                    List<Predicate> andPredicates = new ArrayList<>();
                    andPredicates.add(builder.in(root.get("username")).value(usernameList));
                    return builder.and(andPredicates.toArray(new Predicate[0]));
                };
                Map<String, List<AclAuthority>> authoritiesMap = aclAuthorityRepository.findAll(aclAuthoritySpec)
                        .stream().collect(Collectors.groupingBy(AclAuthority::getUsername));

                List<EsUser> esUserList = aclUserList.stream().map(aclUser -> {
                    EsUser esUser = new EsUser();
                    BeanUtils.copyProperties(aclUser, esUser);
                    List<EsUser.EsUserAuthority> esUserAuthorityList = authoritiesMap.get(aclUser.getUsername()).stream().map(aclAuthority -> {
                        EsUser.EsUserAuthority esUserAuthority = new EsUser.EsUserAuthority();
                        BeanUtils.copyProperties(aclAuthority, esUserAuthority);
                        return esUserAuthority;
                    }).toList();
                    esUser.setAuthorities(esUserAuthorityList);
                    return esUser;
                }).toList();

                // 批量索引用户
                BulkRequest.Builder br = new BulkRequest.Builder();
                for (EsUser esUser : esUserList) {
                    br.operations(op -> op
                        .index(idx -> idx
                            .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                            .document(esUser)
                        )
                    );
                }
                BulkResponse result = elasticsearchClient.bulk(br.build());
                if (result.errors()) {
                    logger.error("Bulk had errors");
                    for (BulkResponseItem item: result.items()) {
                        if (item.error() != null) {
                            logger.error(item.error().reason());
                        }
                    }
                }

                // 获取下一批用户
                lastUsernameRef.set(usernameList.getLast());
                aclUserList = aclUserRepository.findAll(aclUserSpec, pageable).getContent();
            }
        }

        /**
         * 统计数量
         */
        @Test
        public void count() throws IOException {
            CountResponse countResponse = elasticsearchClient.count(c -> c
                    .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                    .query(q -> q.term(t -> t.field("nickname").value("管理")))
            );

            System.out.println(">>>>>> Count:" + countResponse.count());
        }

        /**
         * 查询用户信息
         */
        @Test
        void searchEsUser() throws IOException {
            SearchResponse<EsUser> search = elasticsearchClient.search(s -> s
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .source(src -> src.filter(f -> f.includes("username", "nickname", "enabled", "authorities", "update_time")))
                .from(0).size(5)
                .sort(sort -> sort.field(f -> f.field("username").order(SortOrder.Desc)))
                .query(q0 -> q0
                    .bool(b -> b
                        .filter(
                            Query.of(q1 -> q1.term(t -> t.field("enabled").value(true))),
                            Query.of(q1 -> q1.wildcard(w -> w.field("nickname.keyword").value("*员*"))),
                            Query.of(q1 -> q1.terms(t -> t.field("username")
                                .terms(ts -> ts.value(Lists.newArrayList(
                                    FieldValue.of("admin"),
                                    FieldValue.of("bizadmin"),
                                    FieldValue.of("member")))))
                            ),
                            Query.of(q1 -> q1
                                .range(r -> r.field("update_time")
                                    .format("yyyy-MM-dd HH:mm:ss.SSS")
                                    .gte(JsonData.of("2024-02-06 09:57:00.000"))
                                    .lt(JsonData.of("2024-02-06 09:58:00.000"))
                                )
                            ),
                            Query.of(q1 -> q1
                                .nested(n -> n
                                    .path("authorities")
                                    .query(q2 -> q2.term(t -> t.field("authorities.authority").value("ROLE_USER")))
                                )
                            )
                        ))),
                    EsUser.class);

            System.out.println(">>>>>> Total:" + search.hits().total());

            for (Hit<EsUser> hit: search.hits().hits()) {
                EsUser esUser = hit.source();
                System.out.println(">>>>>>:" + JsonUtil.toJson(esUser));
            }
        }

        /**
         * 批量请求查询用户信息
         */
        @Test
        void msearchEsUser() throws IOException {
            MsearchResponse<EsUser> msearchResponse = elasticsearchClient.msearch(s -> s
                .searches(Lists.newArrayList(
                    RequestItem.of(r -> r
                        .header(h -> h.index(ElasticsearchIndexAliasEnum.USER.getAlias()))
                        .body(b -> b
                            .source(src -> src.filter(f -> f.includes("username", "nickname", "update_time")))
                            .from(0).size(5)
                            .sort(sort -> sort.field(f -> f.field("username").order(SortOrder.Desc)))
                            .query(q -> q.match(m -> m.field("nickname").query("管理")))
                        )
                    ),
                    RequestItem.of(r -> r
                        .header(h -> h.index(ElasticsearchIndexAliasEnum.USER.getAlias()))
                        .body(b -> b
                            .source(src -> src.filter(f -> f.includes("username", "nickname", "authorities")))
                            .query(q -> q.match(m -> m.field("nickname").query("普通")))
                        )
                    )
                )), EsUser.class);

            for (MultiSearchResponseItem<EsUser> responseItem : msearchResponse.responses()) {
                if(responseItem.isFailure()){
                    logger.error("msearchEsUser found error. {}", responseItem.failure());
                    continue;
                }

                System.out.println(">>>>>> Total:" + responseItem.result().hits().total());
                for (Hit<EsUser> hit : responseItem.result().hits().hits()) {
                    EsUser esUser = hit.source();
                    System.out.println(">>>>>>:" + JsonUtil.toJson(esUser));
                }
                System.out.println();
            }
        }
    }
}
