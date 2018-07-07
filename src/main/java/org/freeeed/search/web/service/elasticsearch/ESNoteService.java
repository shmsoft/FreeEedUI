/*
 *
 * Copyright SHMsoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.freeeed.search.web.service.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.freeeed.search.web.configuration.Configuration;
import org.freeeed.search.web.dao.cases.CaseDao;
import org.freeeed.search.web.dao.elasticsearch.ESSearchDao;
import org.freeeed.search.web.dao.elasticsearch.SearchDao;
import org.freeeed.search.web.model.cases.Case;
import org.freeeed.search.web.model.elasticsearch.Note;
import org.freeeed.search.web.service.elasticsearch.ESTagService.Result;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.session.SessionContext;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.freeeed.search.web.service.elasticsearch.ESTagService.Result.ERROR;
import static org.freeeed.search.web.service.elasticsearch.ESTagService.Result.SUCCESS;
import static org.freeeed.search.web.service.elasticsearch.ESTagService.TAGS_SEARCH_FIELD;

public class ESNoteService implements NoteService {

    private static final Logger LOGGER = Logger.getLogger(ESNoteService.class);
    public static final String NOTES_FIELD = "notes";
    public static final String DOC_ID = "id";
    private ESSearchDao searchDao;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result addNotesToDoc(String documentId, String note, String userName, SearchSessionObject esSession) {
        Note newNote = new Note();
        UUID uuid = UUID.randomUUID();
        newNote.setId(uuid.toString());
        newNote.setAddedOn(new Date());//Timezone??
        newNote.setValue(note);
        newNote.setAuthor(userName);

        //get document by id
        String indicesName = esSession.getSelectedCase().getEsSourceIndices();
        SearchResponse searchResponse = searchDao.searchById(indicesName, documentId, new String[]{DOC_ID, NOTES_FIELD});
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        if (searchHits.length > 0) {
            String noteStr = (String) searchHits[0].getSourceAsMap().get(NOTES_FIELD);
            try {
                List<Note> notes = new ArrayList<>();
                if (Objects.nonNull(noteStr)) {
                    notes = objectMapper.readValue(noteStr, new TypeReference<List<Note>>() {
                    });
                }

                notes.add(newNote);
                searchDao.updateNotes(searchHits[0].getId(), objectMapper.writeValueAsString(notes));
            } catch (Exception e) {
                e.printStackTrace();
                return Result.ERROR;
            }
        }

        return SUCCESS;
    }

    @Override
    public Result removeNoteFromSingleDocument(String documentId, String noteId, SearchSessionObject esSession) {
        //get document by id
        String indicesName = esSession.getSelectedCase().getEsSourceIndices();
        SearchResponse searchResponse = searchDao.searchById(indicesName, documentId, new String[]{DOC_ID, NOTES_FIELD});
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        if (searchHits.length > 0) {
            String noteStr = (String) searchHits[0].getSourceAsMap().get(NOTES_FIELD);
            try {
                List<Note> notes = objectMapper.readValue(noteStr, new TypeReference<List<Note>>() {
                });
                if (!Objects.isNull(notes) && !notes.isEmpty()) {
                    notes = notes.stream().filter(n -> !n.getId().equals(noteId)).collect(Collectors.toList());
                }
                searchDao.updateNotes(searchHits[0].getId(), objectMapper.writeValueAsString(notes));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return SUCCESS;
    }

    public void setSearchDao(ESSearchDao searchDao) {
        this.searchDao = searchDao;
    }
}
