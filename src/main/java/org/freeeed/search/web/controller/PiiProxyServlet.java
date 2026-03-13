package org.freeeed.search.web.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.freeeed.search.web.configuration.EnvConfig;

/**
 * Proxies PII analysis requests to the AI Advisor backend.
 * Supports both synchronous calls (detect, average_pii_doc, richness)
 * and async background jobs (start → status polling).
 *
 * Backend port is read from ~/.freeeed/.env (key: PORT), defaulting to 8000.
 */
public class PiiProxyServlet extends HttpServlet {

    private static final String[] ALLOWED_ACTIONS = {
            "detect", "average_pii_doc", "richness", "start", "status"
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        String action = request.getParameter("action");

        if (action == null || action.isEmpty()) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Missing action parameter\"}");
            return;
        }

        // Only allow known actions to prevent SSRF
        boolean allowed = false;
        for (String a : ALLOWED_ACTIONS) {
            if (a.equals(action)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Unknown action\"}");
            return;
        }

        String piiBackendBase = EnvConfig.getAiBackendUrl(8000);
        String backendUrl;

        if ("start".equals(action)) {
            // /start requires sub (detect|average_pii_doc|richness) and case_id
            String sub = request.getParameter("sub");
            String caseId = request.getParameter("case_id");
            if (sub == null || sub.isEmpty() || caseId == null || caseId.isEmpty()) {
                response.setStatus(400);
                response.getWriter().print("{\"error\":\"Missing sub or case_id parameter\"}");
                return;
            }
            backendUrl = piiBackendBase + "/advisors/pii/start"
                    + "?action=" + URLEncoder.encode(sub, "UTF-8")
                    + "&case_id=" + URLEncoder.encode(caseId, "UTF-8");
        } else if ("status".equals(action)) {
            // /status requires job_id
            String jobId = request.getParameter("job_id");
            if (jobId == null || jobId.isEmpty()) {
                response.setStatus(400);
                response.getWriter().print("{\"error\":\"Missing job_id parameter\"}");
                return;
            }
            backendUrl = piiBackendBase + "/advisors/pii/status"
                    + "?job_id=" + URLEncoder.encode(jobId, "UTF-8");
        } else {
            // Synchronous calls: detect, average_pii_doc, richness
            String caseId = request.getParameter("case_id");
            if (caseId == null || caseId.isEmpty()) {
                response.setStatus(400);
                response.getWriter().print("{\"error\":\"Missing case_id parameter\"}");
                return;
            }
            backendUrl = piiBackendBase + "/advisors/pii/" + action
                    + "?case_id=" + URLEncoder.encode(caseId, "UTF-8");
        }

        // Short timeout for start/status (quick calls); long for sync analysis
        int readTimeout = ("start".equals(action) || "status".equals(action)) ? 15000 : 180000;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(backendUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(readTimeout);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            response.setStatus(status);

            InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) {
                response.getWriter().print("{\"error\":\"No response from backend (HTTP " + status + ")\"}");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            response.getWriter().print(sb.toString());

        } catch (java.net.ConnectException e) {
            response.setStatus(503);
            response.getWriter().print(
                    "{\"error\":\"Cannot connect to PII backend at " + piiBackendBase + ". Is it running?\"}");
        } catch (java.net.SocketTimeoutException e) {
            response.setStatus(504);
            response.getWriter().print("{\"error\":\"PII backend timed out. The LLM analysis is taking too long.\"}");
        } catch (Exception e) {
            response.setStatus(500);
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "unknown error";
            response.getWriter().print("{\"error\":\"Proxy error: " + msg + "\"}");
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }
}
