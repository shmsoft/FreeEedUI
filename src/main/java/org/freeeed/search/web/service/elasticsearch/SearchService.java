package org.freeeed.search.web.service.elasticsearch;

import org.freeeed.search.web.searchviews.SearchResultView;

import java.util.Set;

/**
 * Created by nehaojha on 23/04/18.
 */
public interface SearchService {
    SearchResultView search(Set<String> freeTextQueries, Set<String> tagQueries, int from, int rows, String[] fields);
}
