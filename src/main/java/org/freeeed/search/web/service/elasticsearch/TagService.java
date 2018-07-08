package org.freeeed.search.web.service.elasticsearch;

import org.freeeed.search.web.service.elasticsearch.ESTagService.Result;
import org.freeeed.search.web.session.SearchSessionObject;

import java.util.Set;

/**
 * Created by nehaojha on 23/04/18.
 */
public interface TagService {

    Result tagSingleDocument(String documentId, Set<String> tags);

    Result tagThisPageDocuments(SearchSessionObject esSession, String tag);

    Result tagAllSearchedDocuments(SearchSessionObject esSession, String tag);

    Result removeTagFromSingleDocument(SearchSessionObject esSession, String documentId, String tag);

    Result removeTagFromAllDocs(SearchSessionObject esSession, String tag);
}
