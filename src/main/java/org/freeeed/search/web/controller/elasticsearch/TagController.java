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
import org.freeeed.search.web.service.elasticsearch.ESTagService;
import org.freeeed.search.web.session.SearchSessionObject;
import org.freeeed.search.web.utils.WebConstants;
import org.freeeed.search.web.service.elasticsearch.ESTagService.Result;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class TagController.
 * <p>
 * Tag specific documents with a given tag.
 *
 * @author ilazarov
 */
public class TagController extends SecureController {
    private static final Logger log = Logger.getLogger(TagController.class);

    private ESTagService ESTagService;

    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");
        log.debug("Tag action received: " + action);

        HttpSession session = this.request.getSession(true);
        SearchSessionObject esSession = (SearchSessionObject)
                session.getAttribute(WebConstants.WEB_SESSION_SEARCH_OBJECT);

        if (Objects.isNull(esSession)) {
            return new ModelAndView(WebConstants.TAG_PAGE);
        }

        if ("newtag".equals(action)) {
            String tag = (String) valueStack.get("tag");
            String documentId = (String) valueStack.get("docid");
            log.debug("Will do tagging for - documentId: " + documentId + ", tag: " + tag);
            if (documentId != null && tag != null && !documentId.isEmpty() && !tag.isEmpty()) {
                Set<String> uniqueTags = Arrays.stream(tag.split(",")).filter(t -> !"".equals(t)).collect(Collectors.toSet());
                Result result = ESTagService.tagSingleDocument(documentId, uniqueTags);
                valueStack.put("result", result);
            }
        } else if ("tagall".equals(action)) {
            String tag = (String) valueStack.get("tag");
            log.debug("Will do tag all, tag: " + tag);
            if (tag != null && tag.trim().length() > 0) {
                Result result = ESTagService.tagAllSearchedDocuments(esSession, tag);
                valueStack.put("result", result);
            }
        } else if ("tagpage".equals(action)) {
            String tag = (String) valueStack.get("tag");
            log.debug("Will do tag page, tag: " + tag);
            if (tag != null && tag.trim().length() > 0) {
                Result result = ESTagService.tagThisPageDocuments(esSession, tag);
                valueStack.put("result", result);
            }
        } else if ("deletetag".equals(action)) {
            String tag = (String) valueStack.get("tag");
            String documentId = (String) valueStack.get("docid");
            log.debug("Will do delete tag for - documentId: " + documentId + ", tag: " + tag);
            if (documentId != null && tag != null && !documentId.isEmpty() && !tag.isEmpty()) {
                Result result = ESTagService.removeTagFromSingleDocument(esSession, documentId, tag);
                valueStack.put("result", result);
            }
        } else if ("deletetagfromall".equals(action)) {
            String tag = (String) valueStack.get("tag");
            log.debug("removing from every docs tag = " + tag);
            if (tag != null && tag.trim().length() > 0) {
                Result result = ESTagService.removeTagFromAllDocs(esSession, tag);
                valueStack.put("result", result);
            }
        }
        valueStack.put("tags", esSession.getSelectedCase().getTags());
        return new ModelAndView(WebConstants.TAG_PAGE);
    }

    public void setESTagService(ESTagService ESTagService) {
        this.ESTagService = ESTagService;
    }
}
