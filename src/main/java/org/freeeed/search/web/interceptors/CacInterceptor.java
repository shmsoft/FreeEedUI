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
package org.freeeed.search.web.interceptors;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.dao.user.UserDao;
import org.freeeed.search.web.model.User;
import org.freeeed.search.web.session.LoggedSiteVisitor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.security.cert.X509Certificate;


/**
 * 
 * The Application Session Interceptor
 * 
 * @author ilazarov
 *
 */
public class CacInterceptor extends HandlerInterceptorAdapter {
	private static final Logger log = Logger.getLogger(CacInterceptor.class);
    private UserDao userDao;
    
	public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handle) throws Exception {

		log.debug("CAC Interceptor!");
		//log.debug(req.getRequestURL());
        
        Object o = req.getAttribute("javax.servlet.request.X509Certificate");
        if (o != null) {
            X509Certificate certs[] = (X509Certificate[]) o;
            X509Certificate cert = certs[0];
            log.debug(cert.getSubjectX500Principal().getName());
            User user = userDao.loginCac(cert.getSubjectX500Principal().getName());
            if (user != null) {
                HttpSession session = req.getSession();
                LoggedSiteVisitor loggedSiteVisitor = new LoggedSiteVisitor();
                loggedSiteVisitor.setUser(user);
                
                session.setAttribute(WebConstants.LOGGED_SITE_VISITOR_SESSION_KEY, loggedSiteVisitor);
                
                log.debug("User: " + cert.getSubjectX500Principal().getName() + " logged in! IP address: " + req.getRemoteHost());
             }
            
            
        }  else {
           
            if (req.getLocalPort() == 8443) {
                java.lang.StringBuffer newUrl = req.getRequestURL();
                int startPos = newUrl.indexOf(":8443/");
                newUrl = newUrl.replace(startPos,startPos+6,":8444/");
                log.debug("redirect to: " + newUrl);
                res.sendRedirect(newUrl.toString());
                return false;
                
            }
           
        }

        java.lang.StringBuffer newUrl = req.getRequestURL();
        int startPos = 0;
        if (req.getLocalPort() == 8444) {
            startPos = newUrl.indexOf(":8444/");
            newUrl = newUrl.replace(startPos,startPos+6,":8443/");            
        }
        startPos = newUrl.lastIndexOf("/");
        newUrl = newUrl.replace(startPos+1,newUrl.length(),WebConstants.MAIN_PAGE_REDIRECT);
        log.debug("redirect to: " + newUrl);
        res.sendRedirect(newUrl.toString());
        return false;
	}
	
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

}
