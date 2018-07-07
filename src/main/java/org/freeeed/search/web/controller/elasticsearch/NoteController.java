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
package org.freeeed.search.web.controller.elasticsearch;

import org.apache.log4j.Logger;
import org.freeeed.search.web.controller.commons.SecureController;
import org.freeeed.search.web.service.elasticsearch.NoteService;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.utils.WebConstants;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * Class NoteController.
 * <p>
 * Tag specific documents with a given tag.
 *
 * @author nehaojha
 */
public class NoteController extends SecureController {

    private static final Logger log = Logger.getLogger(NoteController.class);

    private NoteService noteService;

    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");
        log.debug("Note action received: " + action);

        HttpSession session = this.request.getSession(true);
        SearchSessionObject esSession = (SearchSessionObject)
                session.getAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT);

        if (Objects.isNull(esSession)) {
            return new ModelAndView(WebConstants.TAG_PAGE);
        }

        if ("addnote".equals(action)) {
            noteService.addNotesToDoc(String.valueOf(valueStack.get("docid")),
                    String.valueOf(valueStack.get("note")),
                    String.valueOf(loggedSiteVisitor.getUser().getUsername()),
                    esSession);
        } else if ("removenote".equals(action)) {
            noteService.removeNoteFromSingleDocument(String.valueOf(valueStack.get("docid")),
                    String.valueOf(valueStack.get("noteid")),
                    esSession);
        }

        valueStack.put("result", "SUCCESS");
        return new ModelAndView(WebConstants.TAG_PAGE);
    }

    public void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }
}
