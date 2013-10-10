/*    
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
package org.freeeed.search.web.tlb;

import org.freeeed.search.web.model.User;
import org.freeeed.search.web.dao.settings.AppSettingsDao;
import org.freeeed.search.web.model.AppSettings;

import org.apache.log4j.Logger;


public class TlbUtil {
    private static final Logger log = Logger.getLogger(TlbUtil.class);

    public static boolean hasRight(User user, String right) {
        return user != null && user.hasRight(User.Right.valueOf(right));
    }
    
    public static boolean getUsesCac(AppSettingsDao appSettingsDao) {
        AppSettings appSettings = null;
        if (appSettingsDao != null) {
            appSettings = appSettingsDao.loadSettings();
        }
        if (appSettings != null) {
            return appSettings.getUsesCac();
        }
        return false;
    }

}
