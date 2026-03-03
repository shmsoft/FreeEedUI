<%@ page contentType="application/json; charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="java.net.*, java.io.*" %>
<%
    response.setHeader("Cache-Control", "no-cache");
    String action = request.getParameter("action");
    String caseId  = request.getParameter("case_id");

    if (action == null || action.isEmpty() || caseId == null || caseId.isEmpty()) {
        response.setStatus(400);
        out.print("{\"error\":\"Missing action or case_id parameter\"}");
        return;
    }

    // Only allow known actions to prevent SSRF
    if (!action.equals("detect") && !action.equals("average_pii_doc") && !action.equals("richness")) {
        response.setStatus(400);
        out.print("{\"error\":\"Unknown action\"}");
        return;
    }

    String backendUrl = "http://localhost:3000/advisors/pii/" + action + "?case_id=" + java.net.URLEncoder.encode(caseId, "UTF-8");

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
            out.print("{\"error\":\"No response from backend (HTTP " + status + ")\"}");
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        out.print(sb.toString());

    } catch (java.net.ConnectException e) {
        response.setStatus(503);
        out.print("{\"error\":\"Cannot connect to PII backend at localhost:3000. Is it running?\"}");
    } catch (java.net.SocketTimeoutException e) {
        response.setStatus(504);
        out.print("{\"error\":\"PII backend timed out. The LLM analysis is taking too long.\"}");
    } catch (Exception e) {
        response.setStatus(500);
        out.print("{\"error\":\"Proxy error: " + e.getMessage().replace("\"","'") + "\"}");
    } finally {
        if (conn != null) conn.disconnect();
    }
%>
