package org.freeeed.search.web.service.elasticsearch;

import org.freeeed.search.web.session.SearchSessionObject;

import java.util.Set;

/**
 * Created by nehaojha on 23/04/18.
 */
public interface TagService {

    ESTagService.Result tagSingleDocument(String documentId, Set<String> tags);

    ESTagService.Result tagThisPageDocuments(SearchSessionObject esSession, String tag);

    ESTagService.Result tagAllSearchedDocuments(SearchSessionObject esSession, String tag);

    ESTagService.Result removeTagFromSingleDocument(SearchSessionObject esSession, String documentId, String tag);

    ESTagService.Result removeTagFromAllDocs(SearchSessionObject esSession, String tag);
}
