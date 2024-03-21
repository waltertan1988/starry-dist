package com.walter.starry.security;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
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
import org.junit.jupiter.api.Assertions;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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

            // 添加索引
            IndexResponse response = elasticsearchClient.index(i -> i
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .id(String.valueOf(esUser.getId()))
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
                            .id(String.valueOf(esUser.getId()))
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
         * 更新单个文档
         */
        @Test
        void update() throws IOException {
            UpdateResponse<EsUser> updateResponse = elasticsearchClient.update(u -> u
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .id("1")
                .script(s -> s.inline(i -> i
                    .lang(ScriptLanguage.Painless)
                    .source("ctx._source.nickname = params.nickname")
                    .params("nickname", JsonData.of("孙悟空")))
                ), EsUser.class);

            System.out.println("version: " + updateResponse.version());
        }

        /**
         * 按搜索条件删除文档
         */
        @Test
        void deleteByQuery() throws IOException {
            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(d -> d
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .query(q -> q.term(t -> t.field("username").value("member")))
            );

            logger.info("delete total: {}", response.total());
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
         * 根据文档id查询一个文档
         */
        @Test
        void get() throws IOException {
            GetResponse<EsUser> getResponse = elasticsearchClient.get(g -> g
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .sourceIncludes("update_time")
                .id("1"), EsUser.class);

            EsUser esUser = getResponse.source();
            System.out.println(esUser.getUpdateTime());
            System.out.println(esUser.getUpdateTime().getTime());
            System.out.println(esUser.getUpdateTime().getHours());
            System.out.println(JsonUtil.toJson(esUser));
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

//                                    .timeZone(TimeZone.getDefault().getID())
//                                    .format("yyyy-MM-dd HH:mm:ss.SSS")
//                                    .gte(JsonData.of("2023-10-17 17:23:00.000"))
//                                    .lt(JsonData.of("2023-10-17 17:24:00.000"))
//
//                                    .format("yyyy-MM-dd HH:mm:ss.SSSZ")
//                                    .gte(JsonData.of("2023-10-17 17:23:00.000+0800"))
//                                    .lt(JsonData.of("2023-10-17 17:24:00.000+0800"))
//
//                                    .gte(JsonData.of("2023-10-17T09:23:00.000Z"))
//                                    .lt(JsonData.of("2023-10-17T09:24:00.000Z"))
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

        /**
         * 使用SearchAfter+PIT进行滚动分页查询
         */
        @Test
        void searchAfterWithPit() throws IOException {
            final String pitKeepAlive = "5s";
            final int pageSize = 3;
            final Function<SortOptions.Builder, ObjectBuilder<SortOptions>> sortFunc = s -> s
                .field(f -> f.field("username").order(SortOrder.Desc));

            // 开启PIT
            String pitId = elasticsearchClient.openPointInTime(pit -> pit
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .keepAlive(k -> k.time(pitKeepAlive))
            ).id();
            AtomicReference<String> pitIdRef = new AtomicReference<>(pitId);

            // 滚动分页查询
            int n = 0;
            SearchResponse<EsUser> response = elasticsearchClient.search(s -> s
                .pit(p -> p.id(pitIdRef.get()).keepAlive(k -> k.time(pitKeepAlive)))
                .size(pageSize)
                .sort(sortFunc)
                .trackTotalHits(t -> t.enabled(false) // 禁用totalHits以加速分页
                ), EsUser.class
            );
            while (CollectionUtils.isNotEmpty(response.hits().hits())){
                for (Hit<EsUser> hit : response.hits().hits()) {
                    System.out.printf("%s:\t%s%n", ++n, JsonUtil.toJson(hit.source()));
                }

                System.out.println();
                pitIdRef.set(response.pitId());// 使用响应体中的最新的pitId
                List<FieldValue> searchAfterSortList = response.hits().hits().getLast().sort();
                response = elasticsearchClient.search(s -> s
                    .pit(p -> p.id(pitIdRef.get()).keepAlive(k -> k.time(pitKeepAlive)))
                    .size(pageSize)
                    .sort(sortFunc)
                    .searchAfter(searchAfterSortList)
                    .trackTotalHits(t -> t.enabled(false)), EsUser.class
                );
            }

            // 关闭PIT
            elasticsearchClient.closePointInTime(pit -> pit.id(pitIdRef.get()));
        }
    }

    @Nested
    class AggregationTest {
        /**
         * 值数量统计
         */
        @Test
        void valueCount() throws IOException {
            final String key = "value_count(openId)";
            SearchResponse<Void> searchResponse = elasticsearchClient.search(search -> search
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .query(q -> q.term(t -> t.field("enabled").value(true)))
                .aggregations(key, a -> a.valueCount(vc -> vc.field("open_id")))
                .size(0), Void.class);

            Aggregate aggregate = searchResponse.aggregations().get(key);
            System.out.printf("%s: %s%n", key, aggregate.valueCount().value());
        }

        /**
         * 最小值统计
         */
        @Test
        void min() throws IOException {
            final String key = "min(create_time)";
            SearchResponse<Void> searchResponse = elasticsearchClient.search(search -> search
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .query(q -> q.term(t -> t.field("enabled").value(false)))
                .aggregations(key, a -> a.min(vc -> vc.field("create_time")))
                .size(0), Void.class);

            Aggregate aggregate = searchResponse.aggregations().get(key);
            System.out.printf("%s: %s%n", key, aggregate.min().valueAsString());
        }

        /**
         * 嵌套统计最大值
         */
        @Test
        void nestedMax() throws IOException {
            final String key1 = "nested(authorities)";
            final String key2 = "max(create_time)";
            SearchResponse<Void> searchResponse = elasticsearchClient.search(search -> search
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .query(q -> q.term(t -> t.field("enabled").value(true)))
                .aggregations(key1, a -> a
                    .nested(n -> n.path("authorities"))
                    .aggregations(key2, agg -> agg.max(m -> m.field("authorities.create_time")))
                )
                .size(0), Void.class);

            Aggregate aggregate = searchResponse.aggregations().get(key1).nested().aggregations().get(key2);
            System.out.printf("%s.%s: %s%n", key1, key2, aggregate.max().valueAsString());
        }
    }

    @Nested
    class PointInTimeTest {
        @Test
        void pointInTime() throws IOException, InterruptedException {
            // 初始化14条数据
            long count1 = elasticsearchClient.count(c -> c.index(ElasticsearchIndexAliasEnum.USER.getAlias())).count();
            System.out.println(">>>>>> count1: " + count1);

            // 开启PIT
            String pitId = elasticsearchClient.openPointInTime(pit -> pit
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .keepAlive(k -> k.time("1m"))
            ).id();

            // 删除1条数据
            elasticsearchClient.deleteByQuery(d -> d
                .index(ElasticsearchIndexAliasEnum.USER.getAlias())
                .query(q -> q.term(t -> t.field("username").value("admin")))
            );

            // 等待ES刷新磁盘
            TimeUnit.SECONDS.sleep(5);

            // 剩余13条数据
            long count2 = elasticsearchClient.count(c -> c.index(ElasticsearchIndexAliasEnum.USER.getAlias())).count();
            System.out.println(">>>>>> count2: " + count2);

            // 使用PIT检索到14条数据
            SearchResponse<EsUser> searchResponse = elasticsearchClient.search(s -> s.pit(p -> p.id(pitId)), EsUser.class);
            System.out.println(">>>>>> count3: " + searchResponse.hits().total());

            // 关闭PIT
            elasticsearchClient.closePointInTime(pit -> pit.id(pitId));

            // 使用已关闭的PIT会报错
            Assertions.assertThrows(Exception.class, () -> elasticsearchClient.search(s -> s.pit(p -> p.id(pitId)), EsUser.class));
        }
    }
}
