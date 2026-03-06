package org.freeeed.search.web.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

/**
 * Proxies PII analysis requests to the AI Advisor backend.
 * Backend port is read from ~/.freeeed/.env (key: PORT), defaulting to 8000.
 */
public class PiiProxyServlet extends HttpServlet {

    private static final String[] ALLOWED_ACTIONS = { "detect", "average_pii_doc", "richness" };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        String action = request.getParameter("action");
        String caseId = request.getParameter("case_id");

        if (action == null || action.isEmpty() || caseId == null || caseId.isEmpty()) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Missing action or case_id parameter\"}");
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

        // Read PORT from ~/.freeeed/.env; fall back to 8000
        String piiPort = "8000";
        File envFile = new File(System.getProperty("user.home") + "/.freeeed/.env");
        if (envFile.exists()) {
            Properties envProps = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(envFile);
                envProps.load(fis);
            } catch (IOException ignored) {
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignored2) {
                    }
                }
            }
            String portVal = envProps.getProperty("PORT");
            if (portVal != null && !portVal.trim().isEmpty()) {
                piiPort = portVal.trim();
            }
        }

        String piiBackendBase = "http://localhost:" + piiPort;
        String backendUrl = piiBackendBase + "/advisors/pii/" + action
                + "?case_id=" + URLEncoder.encode(caseId, "UTF-8");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(backendUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(90000);
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
