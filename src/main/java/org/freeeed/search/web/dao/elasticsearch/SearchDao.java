package org.freeeed.search.web.dao.elasticsearch;

import org.elasticsearch.action.search.SearchResponse;
import org.freeeed.search.web.service.elasticsearch.ESTagService;

import java.util.Set;

/**
 * Created by nehaojha on 22/04/18.
 */
public interface SearchDao {
    SearchResponse search(String indicesName, Set<String> freeTextQueries, Set<String> tagQueries, int page, int size, String[] includeFields);

    SearchResponse searchById(String indicesName, String documentId, String[] includeFields);

    ESTagService.Result updateSingleDocTag(String documentId, Set<String> tags);
}
