package org.freeeed.search.web.dao.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.freeeed.search.web.configuration.Configuration;
import org.freeeed.search.web.model.elasticsearch.Note;
import org.freeeed.search.web.service.elasticsearch.ESTagService;
import org.freeeed.search.web.service.elasticsearch.ESTagService.Result;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.session.SessionContext;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.freeeed.search.web.service.elasticsearch.ESTagService.Result.ERROR;
import static org.freeeed.search.web.service.elasticsearch.ESTagService.Result.SUCCESS;

/**
 * Created by nehaojha on 14/04/18.
 */
public class ESSearchDao implements SearchDao {

    private static final Logger LOGGER = Logger.getLogger(ESSearchDao.class);
    private Configuration configuration;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SearchResponse search(String indicesName, Set<String> freeTextQueries, Set<String> tagQueries, int page, int size, String[] includeFields) {
        String url = configuration.getESEndpoint();
        try {
            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)))) {
                SearchRequest searchRequest = new SearchRequest(indicesName);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                BoolQueryBuilder booleanQueryBuilder = QueryBuilders.boolQuery();
                freeTextQueries.forEach((freeText) -> booleanQueryBuilder.must(QueryBuilders.simpleQueryStringQuery(freeText)));
                tagQueries.forEach((tag) -> booleanQueryBuilder.must(QueryBuilders.matchQuery(ESTagService.TAGS_SEARCH_FIELD, tag)));
                searchSourceBuilder.query(booleanQueryBuilder);
                searchSourceBuilder.from(page * size);
                searchSourceBuilder.size(size);
                searchSourceBuilder.query(booleanQueryBuilder);
                if (Objects.nonNull(includeFields)) {
                    searchSourceBuilder.fetchSource(includeFields, new String[0]);
                }
                searchRequest.source(searchSourceBuilder);

                return client.search(searchRequest);
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        return null;
    }

    @Override
    public SearchResponse searchById(String indicesName, String documentId, String[] includeFields) {
        String url = configuration.getESEndpoint();
        try {
            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)))) {
                SearchRequest searchRequest = new SearchRequest(indicesName);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.matchQuery("_id", documentId));
                if (Objects.nonNull(includeFields)) {
                    searchSourceBuilder.fetchSource(includeFields, new String[0]);
                }
                searchRequest.source(searchSourceBuilder);
                return client.search(searchRequest);
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        return null;
    }

    @Override
    public Result updateSingleDocTag(String documentId, Set<String> tags) {
        try {
            SearchSessionObject esSession = SessionContext.getElasticSearchSession();
            String indicesName = esSession.getSelectedCase().getEsSourceIndices();
            String url = configuration.getESEndpoint();
            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)))) {
                UpdateRequest updateRequest = new UpdateRequest(indicesName, indicesName, documentId);
                XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
                        .startObject().field(ESTagService.TAGS_SEARCH_FIELD, tags).endObject();
                updateRequest.doc(xContentBuilder);
                client.update(updateRequest);
                refreshIndex();
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while tagging document " + documentId, ex);
            return ERROR;
        }
        return SUCCESS;
    }

    @Override
    public Result updateNotes(String documentId, String notes) {
        try {
            SearchSessionObject esSession = SessionContext.getElasticSearchSession();
            String indicesName = esSession.getSelectedCase().getEsSourceIndices();
            String url = configuration.getESEndpoint();
            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)))) {
                UpdateRequest updateRequest = new UpdateRequest(indicesName, indicesName, documentId);
                XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
                        .startObject().field("notes", notes).endObject();
                updateRequest.doc(xContentBuilder);
                client.update(updateRequest);
                refreshIndex();
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while tagging document " + documentId, ex);
            return ERROR;
        }
        return SUCCESS;
    }

    public void refreshIndex() {
        try {
            SearchSessionObject esSession = SessionContext.getElasticSearchSession();
            String indicesName = esSession.getSelectedCase().getEsSourceIndices();
            String url = configuration.getESEndpoint();
            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)))) {
                RefreshRequest request = new RefreshRequest(indicesName);
                client.indices().refresh(request);
            }
        } catch (Exception ex) {
            LOGGER.error("Exception while tagging document ", ex);
        }
    }
}
