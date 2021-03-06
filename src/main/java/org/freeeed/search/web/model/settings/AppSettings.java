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
package org.freeeed.search.web.model.settings;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * Class AppSettings.
 * 
 * @author ilazarov.
 *
 */
public class AppSettings implements Serializable {
    private static final long serialVersionUID = -122869144861713552L;
    
    private int resultsPerPage;
    private String esEndpoint;
    private List<String> permanentTags;
    
    public int getResultsPerPage() {
        return resultsPerPage;
    }
    
    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }
    
    public String getEsEndpoint() {
        return esEndpoint;
    }
    
    public void setEsEndpoint(String esEndpoint) {
        this.esEndpoint = esEndpoint;
    }

    public List<String> getPermanentTags() {
        return permanentTags;
    }

    public void setPermanentTags(List<String> permanentTags) {
        this.permanentTags = permanentTags;
    }
}
