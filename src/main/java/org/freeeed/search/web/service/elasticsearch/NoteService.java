package org.freeeed.search.web.service.elasticsearch;

import org.freeeed.search.web.model.elasticsearch.Note;
import org.freeeed.search.web.service.elasticsearch.ESTagService.Result;
import org.freeeed.search.web.session.SearchSessionObject;

import java.util.Set;

/**
 * Created by nehaojha on 07/07/18.
 */
public interface NoteService {

    Result addNotesToDoc(String documentId, String note, String userName, SearchSessionObject esSession);

    Result removeNoteFromSingleDocument(String documentId, String noteId, SearchSessionObject esSession);
}
