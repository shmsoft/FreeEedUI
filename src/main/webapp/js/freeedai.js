function onSubmit(event) {
    event.preventDefault();

    const container = $(".chat-wrapper")[0];
    const $questionInput = $(".question_input");
    const question = $questionInput.val();

    container.innerHTML += '<div class="question">' + escapeHtml(question) + '</div>';
    if ($('#chat-welcome').is(':visible')) { $('#chat-welcome').fadeOut(150); $('#chat-chips').slideUp(150); }
    $('#send_question').prop('disabled', true);
    $questionInput.prop('disabled', true);

    const allCases = $("#allCasesCheckbox").prop("checked");
    // Normalize to prevent accidental double slashes when concatenating paths
    const aiApiUrl = String($("#aiApiUrl").val() || '').replace(/\/+$/, '');

    // --- runtime diagnostics removed (was showing extra details above answers) ---

    // dropdown value is DB id; options have data-project-id="case_3" etc.
    const selectedCaseDbId = $(".your-case-select").val();
    const selectedOpt = $('#case_Select option:selected');
    const selectedCaseProjectId = selectedOpt.data('project-id') || $("#aiProjectId").val();

    function resetInputs() {
        $questionInput.val('');
        $('#send_question').prop('disabled', false);
        $questionInput.prop('disabled', false);
    }

    function renderError(msg, context) {
        console.error("AI Advisor error", { context: context, message: msg });
        container.innerHTML += '<div class="answer"><b>AI error (' + escapeHtml(context) + '):</b> <pre style="white-space:pre-wrap">' + escapeHtml(msg) + '</pre></div>';
        resetInputs();
    }

    function renderAnswer(data, usedCaseId) {
        let sourcesHtml = '';

        try {
            if (data && data.answer && typeof data.answer === 'string' && data.answer.indexOf('No matching documents') >= 0 && usedCaseId) {
                data.answer = data.answer + ' (case_id=' + usedCaseId + ')';
            }
        } catch (e) {
            // ignore
        }

        if (allCases) {
            // Most common shapes: array OR {answers:[...]}. If not, show raw.
            const answers = Array.isArray(data) ? data : (data && Array.isArray(data.answers) ? data.answers : null);
            if (answers) {
                answers.forEach(function (answer) {
                    const caseId = answer.case_id;
                    if (answer.sources && answer.sources.length > 0) {
                        sourcesHtml = '<small class="source">';
                        for (let i = 0; i < answer.sources.length; i++) {
                            const source = answer.sources[i];
                            const href = getCurrentUrl() + '/search.html?caseid=' + encodeURIComponent(caseId) + '&query=UPI:' + encodeURIComponent(source);
                            sourcesHtml += '<a target="_blank" href="' + href + '">' + escapeHtml(source) + '</a> | ';
                        }
                        sourcesHtml += '</small>';
                    }
                    container.innerHTML += '<div class="answer">' + escapeHtml(answer.answer || JSON.stringify(answer)) + sourcesHtml + '</div>';
                });
            } else {
                container.innerHTML += '<div class="answer">' + escapeHtml((data && data.answer) ? data.answer : JSON.stringify(data)) + '</div>';
            }
        } else {
            if (data && data.sources && data.sources.length > 0) {
                sourcesHtml = '<small class="source">';
                for (let i = 0; i < data.sources.length; i++) {
                    const source = data.sources[i];
                    const href = getCurrentUrl() + '/search.html?caseid=' + encodeURIComponent(selectedCaseDbId) + '&query=UPI:' + encodeURIComponent(source);
                    sourcesHtml += '<a target="_blank" href="' + href + '">' + escapeHtml(source) + '</a> | ';
                }
                sourcesHtml += '</small>';
            }
            container.innerHTML += '<div class="answer">' + escapeHtml((data && data.answer) ? data.answer : JSON.stringify(data)) + sourcesHtml + '</div>';
        }

        resetInputs();
    }

    function toNumericCaseId(value) {
        if (!value) return null;
        const m = String(value).match(/(\d+)$/);
        return m ? m[1] : null;
    }

    if (allCases) {
        const options = $(".your-case-select").find('option');
        const caseIds = [];
        options.each(function () {
            const dbId = $(this).val();
            const projectId = $(this).data('project-id');
            const numeric = toNumericCaseId(projectId) || toNumericCaseId(dbId);
            if (numeric) caseIds.push(String(numeric));
        });

        if (!caseIds.length) {
            renderError('Could not derive numeric case ids from dropdown/projectId values.', 'case_id');
            return;
        }

        const params = { question: question, case_ids: caseIds };

        $.ajax({
            type: 'GET',
            url: aiApiUrl + '/advisors/retrieval/question_cases',
            data: params,
            traditional: true,
            dataType: 'json',
            success: function (resp) {
                renderAnswer(resp, caseIds.join(','));
            },
            error: function (xhr) {
                const msg = (xhr && xhr.responseText) ? xhr.responseText : ("HTTP " + (xhr ? xhr.status : ""));
                renderError(msg, 'question_cases');
            }
        });
        return;
    }

    // Prioritize extracting from projectId if it looks like "case_X"
    let numericCaseId = null;
    if (selectedCaseProjectId) {
        // Try to match "case_123" or just "123"
        const m = String(selectedCaseProjectId).match(/(?:^|case_|_)?(\d+)$/i);
        if (m) numericCaseId = m[1];
    }

    // Fallback to dbId if projectId yielded nothing (though dbId usually corresponds to internal ID, not API ID)
    if (!numericCaseId && selectedCaseDbId) {
        numericCaseId = selectedCaseDbId;
    }

    if (!numericCaseId) {
        renderError('Could not derive numeric case_id from selected case (dbId=' + selectedCaseDbId + ', projectId=' + selectedCaseProjectId + ')', 'case_id');
        return;
    }

    const params = { question: question, case_id: String(numericCaseId) };

    $.ajax({
        type: 'GET',
        url: aiApiUrl + '/advisors/retrieval/question_case',
        data: params,
        dataType: 'json',
        success: function (resp) {
            renderAnswer(resp, String(numericCaseId));
        },
        error: function (xhr) {
            const msg = (xhr && xhr.responseText) ? xhr.responseText : ("HTTP " + (xhr ? xhr.status : ""));
            renderError(msg, 'question_case');
        }
    });
}

function getCurrentUrl() {
    const url = new URL(window.location.href);
    const path = url.pathname.split("/");
    path.pop();
    return path.join("/");
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function matterTypeSelect(value) {
    if (value !== 'generic') {
        var select = document.querySelector('#matterType');
        select.value = 'generic';
        alert("Specialized reports are under development. \nCurrently you can vote for which report you would like to ne developed first. \nCast your ballot by writing to info by writing to info@scaia.ai. \nThank you!");
    }
}

function sendQuestion(event) {
    alert("Specialized reports are under development. \nCurrently you can vote for which report you would like to ne developed first. \nCast your ballot by writing to info by writing to info@scaia.ai. \nThank you!");
    return;
}

var _piiCharts = [];
var _piiPendingResults = null; // holds scan results when user was in a different browser tab

function sendPiiReport(event) {
    event.preventDefault();
    var selectedOpt = $('#case_Select option:selected');
    var caseName = (selectedOpt.text() || '').trim() || 'Unknown Case';

    // Primary: extract numeric id from case name (e.g. "case_1" → "1", "case_42" → "42")
    var numericCaseId = null;
    var nameMatch = caseName.match(/case_?(\d+)$/i);
    if (nameMatch) {
        numericCaseId = nameMatch[1];
    }

    // Fallback: try projectId attribute
    if (!numericCaseId) {
        var projectId = selectedOpt.data('project-id') || $('#aiProjectId').val();
        if (projectId) {
            var pm = String(projectId).match(/(\d+)$/);
            if (pm && pm[1] !== '0') numericCaseId = pm[1];
        }
    }

    _piiCharts.forEach(function (c) { try { c.destroy(); } catch (e) { } });
    _piiCharts = [];
    $('#pii-report-modal').remove();
    _piiOpenModal(caseName);

    if (!numericCaseId) {
        // No valid case selected — show friendly prompt
        setTimeout(function () {
            $('#pii-body').html(
                '<div class="pii-err">' +
                '<div class="pii-err-icon">\uD83D\uDCC2</div>' +
                '<div class="pii-err-title">No Case Selected</div>' +
                '<div class="pii-err-msg">Please select a specific case from the dropdown above before running the PII report.</div>' +
                '<div class="pii-err-hint">The \'default\' case has no indexed documents.</div>' +
                '</div>'
            );
        }, 400);
        return;
    }

    _piiStartScan(numericCaseId, caseName);
}

function _piiOpenModal(caseName) {
    $('body').append(
        '<div id="pii-report-modal" class="pii-overlay">' +
        '<div class="pii-panel">' +
        '<div class="pii-header">' +
        '<div class="pii-header-left">' +
        '<span class="pii-header-icon">\uD83D\uDD0F</span>' +
        '<span class="pii-header-title">PII Intelligence Report</span>' +
        '<span class="pii-header-badge">' + escapeHtml(caseName) + '</span>' +
        '</div>' +
        '<button class="pii-close" id="pii-close-btn">\u2715</button>' +
        '</div>' +
        '<div class="pii-body" id="pii-body">' + _piiScannerHtml() + '</div>' +
        '</div>' +
        '</div>'
    );
    $('#pii-report-modal').hide().fadeIn(300);
    $('#pii-close-btn').on('click', function () { $('#pii-report-modal').fadeOut(220, function () { $(this).remove(); }); });
    $('#pii-report-modal').on('click', function (e) { if (e.target === this) $(this).fadeOut(220, function () { $(this).remove(); }); });
}

function _piiScannerHtml() {
    return (
        '<div class="pii-scanner">' +
        '<div class="pii-radar-wrap">' +
        '<svg class="pii-radar-svg" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">' +
        '<circle cx="100" cy="100" r="90" fill="none" stroke="rgba(106,13,173,0.15)" stroke-width="1.5"/>' +
        '<circle cx="100" cy="100" r="65" fill="none" stroke="rgba(106,13,173,0.2)" stroke-width="1.5"/>' +
        '<circle cx="100" cy="100" r="40" fill="none" stroke="rgba(106,13,173,0.28)" stroke-width="1.5"/>' +
        '<circle cx="100" cy="100" r="15" fill="rgba(37,0,91,0.3)"/>' +
        '<line x1="100" y1="10" x2="100" y2="190" stroke="rgba(106,13,173,0.12)" stroke-width="1"/>' +
        '<line x1="10" y1="100" x2="190" y2="100" stroke="rgba(106,13,173,0.12)" stroke-width="1"/>' +
        '<path class="pii-beam" d="M100,100 L100,10 A90,90 0 0,1 190,100 Z" fill="rgba(106,13,173,0.18)"/>' +
        '<path class="pii-beam-trail" d="M100,100 L100,10 A90,90 0 0,1 163,163 Z" fill="rgba(106,13,173,0.06)"/>' +
        '</svg>' +
        '<div class="pii-radar-icon">\uD83D\uDD0D</div>' +
        '</div>' +
        '<div class="pii-scan-title">Scanning for PII Entities\u2026</div>' +
        '<div class="pii-scan-sub">\u26A1 Running 3 AI detection APIs <strong>sequentially</strong> \u2014 you can switch tabs</div>' +
        '<div class="pii-steps-list">' +
        '<div class="pii-step" id="pii-step-0"><div class="pii-dot pii-dot-idle"></div><span class="pii-step-txt">Detecting PII entities</span><span class="pii-step-api">\u2192 /advisors/pii/detect</span></div>' +
        '<div class="pii-step" id="pii-step-1"><div class="pii-dot pii-dot-idle"></div><span class="pii-step-txt">Calculating average PII per doc</span><span class="pii-step-api">\u2192 /advisors/pii/average_pii_doc</span></div>' +
        '<div class="pii-step" id="pii-step-2"><div class="pii-dot pii-dot-idle"></div><span class="pii-step-txt">Measuring PII richness</span><span class="pii-step-api">\u2192 /advisors/pii/richness</span></div>' +
        '<div class="pii-step" id="pii-step-3"><div class="pii-dot pii-dot-idle"></div><span class="pii-step-txt">Compiling intelligence report</span><span class="pii-step-api">\u2192 Building charts</span></div>' +
        '</div>' +
        '<div class="pii-prog-track"><div class="pii-prog-bar" id="pii-prog-bar" style="width:0%"></div></div>' +
        '<div class="pii-prog-pct" id="pii-prog-pct">0%</div>' +
        '</div>'
    );
}

function _piiStepOn(i) {
    $('#pii-step-' + i + ' .pii-dot').removeClass('pii-dot-idle pii-dot-done').addClass('pii-dot-active');
    $('#pii-step-' + i + ' .pii-step-txt').css({ color: '#25005b', fontWeight: '700' });
    var p = i * 25 + 5;
    $('#pii-prog-bar').animate({ width: p + '%' }, 350);
    $('#pii-prog-pct').text(p + '%');
}

function _piiStepOk(i) {
    $('#pii-step-' + i + ' .pii-dot').removeClass('pii-dot-idle pii-dot-active').addClass('pii-dot-done');
    $('#pii-step-' + i + ' .pii-step-txt').css({ color: '#27ae60', fontWeight: '700' });
    var p = (i + 1) * 25;
    $('#pii-prog-bar').animate({ width: p + '%' }, 300);
    $('#pii-prog-pct').text(p + '%');
}

function _piiStartScan(caseId, caseName, targetId) {
    targetId = targetId || 'pii-body';
    var PROXY = getCurrentUrl() + '/pii-proxy';
    var results = {};
    var POLL_INTERVAL = 3000;  // 3 seconds between polls

    function _dotOn(i) {
        var $s = $('#pii-step-' + i);
        if ($s.length) {
            $s.find('.pii-dot').removeClass('pii-dot-idle pii-dot-done').addClass('pii-dot-active');
            $s.find('.pii-step-txt').css({ color: '#25005b', fontWeight: '700' });
        }
    }
    function _dotOk(i) {
        var $s = $('#pii-step-' + i);
        if ($s.length) {
            $s.find('.pii-dot').removeClass('pii-dot-idle pii-dot-active').addClass('pii-dot-done');
            $s.find('.pii-step-txt').css({ color: '#27ae60', fontWeight: '700' });
        }
    }
    function _dotErr(i) {
        var $s = $('#pii-step-' + i);
        if ($s.length) {
            $s.find('.pii-dot').removeClass('pii-dot-idle pii-dot-active').addClass('pii-dot-done');
            $s.find('.pii-step-txt').css({ color: '#e74c3c', fontWeight: '700' });
        }
    }
    function _progSet(pct) {
        if ($('#pii-prog-bar').length) {
            $('#pii-prog-bar').stop(true).animate({ width: pct + '%' }, 280);
            $('#pii-prog-pct').text(pct + '%');
        }
    }

    // Start a background job, poll until done, then resolve
    function _startAndPoll(sub) {
        var d = $.Deferred();
        $.ajax({
            type: 'GET', url: PROXY,
            data: { action: 'start', sub: sub, case_id: String(caseId) },
            dataType: 'json', timeout: 15000,
            success: function (resp) {
                if (resp && resp.job_id) {
                    _poll(resp.job_id, d);
                } else {
                    d.resolve({ _error: 'No job_id returned' });
                }
            },
            error: function (xhr) {
                d.resolve({ _error: xhr.responseText || ('HTTP ' + xhr.status) });
            }
        });
        return d.promise();
    }

    function _poll(jobId, deferred) {
        setTimeout(function () {
            $.ajax({
                type: 'GET', url: PROXY,
                data: { action: 'status', job_id: jobId },
                dataType: 'json', timeout: 15000,
                success: function (resp) {
                    if (resp.status === 'done') {
                        deferred.resolve(resp.result);
                    } else if (resp.status === 'error') {
                        deferred.resolve({ _error: resp.error || 'Job failed' });
                    } else {
                        // Still pending — poll again
                        _poll(jobId, deferred);
                    }
                },
                error: function (xhr) {
                    // Network glitch during poll — retry once more
                    setTimeout(function () { _poll(jobId, deferred); }, POLL_INTERVAL);
                }
            });
        }, POLL_INTERVAL);
    }

    function _finish() {
        _dotOn(3);
        try {
            localStorage.setItem('_piiLastScan', JSON.stringify({
                caseName: caseName, results: results, ts: Date.now()
            }));
            localStorage.removeItem('_piiScanInProgress');
        } catch (e) {}
        setTimeout(function () {
            _dotOk(3);
            _progSet(100);
            setTimeout(function () {
                if (document.hidden) {
                    _piiPendingResults = { caseName: caseName, results: results, targetId: targetId };
                    _piiNotifyTabDone(caseName);
                } else {
                    _piiRender(caseName, results, targetId);
                }
            }, 400);
        }, 500);
    }

    // ── Fire all 3 actions in PARALLEL for max speed ──
    _progSet(2);
    _dotOn(0); _dotOn(1); _dotOn(2);
    var _doneCount = 0;

    function _onOne() {
        _doneCount++;
        _progSet(Math.round(_doneCount * 25 + 2));
    }

    var pDetect = _startAndPoll('detect').then(function (d) {
        if (d && d._error) { results.detectError = d._error; _dotErr(0); }
        else { results.detect = d; _dotOk(0); }
        _onOne();
    });

    var pAvg = _startAndPoll('average_pii_doc').then(function (d) {
        if (d && d._error) { _dotErr(1); }
        else { results.average = d; _dotOk(1); }
        _onOne();
    });

    var pRich = _startAndPoll('richness').then(function (d) {
        if (d && d._error) { _dotErr(2); }
        else { results.richness = d; _dotOk(2); }
        _onOne();
    });

    $.when(pDetect, pAvg, pRich).then(function () {
        // All 3 done — compile report
        _finish();
    });
}

// ── PII table/export state ──
var _piiTableData = { headers: [], rows: [], caseName: '' };

function _piiExportCsv() {
    var h = _piiTableData.headers, rows = _piiTableData.rows;
    if (!h.length) { alert('No data to export.'); return; }
    var esc = function (v) { return '"' + String(v == null ? '' : v).replace(/"/g, '""') + '"'; };
    var lines = [h.map(esc).join(',')];
    rows.forEach(function (r) { lines.push(h.map(function (k) { return esc(r[k]); }).join(',')); });
    var blob = new Blob([lines.join('\r\n')], { type: 'text/csv' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url; a.download = 'pii_report_' + (_piiTableData.caseName || 'export') + '.csv';
    document.body.appendChild(a); a.click();
    setTimeout(function () { URL.revokeObjectURL(url); document.body.removeChild(a); }, 500);
}

function _piiFlatRow(obj, prefix) {
    prefix = prefix || '';
    var out = {};
    Object.keys(obj || {}).forEach(function (k) {
        var v = obj[k], key = prefix ? prefix + '_' + k : k;
        if (v !== null && !Array.isArray(v) && typeof v === 'object') {
            var sub = _piiFlatRow(v, key);
            Object.keys(sub).forEach(function (sk) { out[sk] = sub[sk]; });
        } else if (Array.isArray(v)) {
            out[key] = v.map(function (x) { return typeof x === 'object' ? JSON.stringify(x) : x; }).join(' | ');
        } else {
            out[key] = v;
        }
    });
    return out;
}

function _piiExtractRows(data) {
    if (Array.isArray(data)) return data;
    var keys = ['documents', 'results', 'items', 'data', 'records', 'entities', 'pii_results', 'detections', 'files'];
    for (var i = 0; i < keys.length; i++) {
        if (Array.isArray(data[keys[i]])) return data[keys[i]];
    }
    return Object.keys(data).length > 0 ? [data] : [];
}

function _piiKpi(icon, value, label, color) {
    return '<div class="pii-kpi" style="border-top:4px solid ' + color + '">' +
        '<div class="pii-kpi-icon">' + icon + '</div>' +
        '<div class="pii-kpi-val" data-target="' + value + '">0</div>' +
        '<div class="pii-kpi-lbl">' + label + '</div>' +
        '</div>';
}

function _piiRender(caseName, results, targetId) {
    targetId = targetId || 'pii-body';
    _piiTableData.caseName = caseName;
    var PII_META = {
        email: { icon: '\u2709', color: '#3498db' },
        ssn: { icon: '\uD83D\uDD12', color: '#e74c3c' },
        phone: { icon: '\uD83D\uDCDE', color: '#2ecc71' },
        address: { icon: '\uD83C\uDFE0', color: '#e67e22' },
        credit_card: { icon: '\uD83D\uDCB3', color: '#9b59b6' },
        creditcard: { icon: '\uD83D\uDCB3', color: '#9b59b6' },
        passport: { icon: '\uD83D\uDEC2', color: '#1abc9c' },
        name: { icon: '\uD83D\uDC64', color: '#f39c12' },
        full_name: { icon: '\uD83D\uDC64', color: '#f39c12' },
        dob: { icon: '\uD83D\uDCC5', color: '#e91e63' },
        ip: { icon: '\uD83C\uDF10', color: '#00bcd4' },
        ip_address: { icon: '\uD83C\uDF10', color: '#00bcd4' },
        medical: { icon: '\uD83C\uDFE5', color: '#ff5722' },
        bank_account: { icon: '\uD83C\uDFE6', color: '#607d8b' }
    };
    var SEVC = { high: '#e74c3c', medium: '#f39c12', low: '#27ae60' };
    var data = results.detect || {}, avg = results.average || {}, rich = results.richness || {};

    // Helper to show raw JSON fallback — popup NEVER disappears
    function _showRaw(title, err) {
        // Always re-enable the Generate button — scan is finished (even if with an error)
        $('#pii-gen-btn').prop('disabled', false)
            .html('<i class="bi-shield-shaded"></i>&nbsp;Generate Report');
        var rawHtml = '<div style="padding:14px">';
        rawHtml += '<div class="pii-err" style="margin-bottom:14px"><div class="pii-err-icon">\u26A0\uFE0F</div>';
        rawHtml += '<div class="pii-err-title">' + escapeHtml(title) + '</div>';
        if (err) rawHtml += '<div class="pii-err-msg" style="font-size:11px;margin-top:6px">' + escapeHtml(String(err)) + '</div>';
        rawHtml += '</div>';
        rawHtml += '<div style="font-weight:700;font-size:12px;color:#5a0099;margin:10px 0 5px">\uD83D\uDD0E Raw API Responses (for debugging):</div>';
        ['detect', 'average', 'richness'].forEach(function (k) {
            rawHtml += '<div style="margin-bottom:8px"><b style="font-size:11px;color:#666">' + k + ':</b><pre style="font-size:11px;background:#f5f2ff;border:1px solid #ddd;border-radius:6px;padding:8px;margin:3px 0;white-space:pre-wrap;word-break:break-all;max-height:140px;overflow:auto">' + escapeHtml(JSON.stringify(results[k] || results[k + 'Error'] || null, null, 2)) + '</pre></div>';
        });
        rawHtml += '</div>';
        $('#' + targetId).stop(true).show().html(rawHtml);
    }

    if (results.detectError && !Object.keys(data).length) {
        var errMsg = results.detectError;
        try { var ep = JSON.parse(errMsg); if (ep.detail) errMsg = ep.detail; } catch (e) { }
        // HTTP 0 = connection refused = Python backend not running
        if (/^HTTP 0$/i.test($.trim(errMsg)) || /connection refused|econnrefused/i.test(errMsg)) {
            errMsg = 'Cannot connect to the PII backend (HTTP 0 — connection refused). ' +
                'Please start the Python backend server and try again.';
        }
        _showRaw('\uD83D\uDEA8 Backend Unreachable', errMsg);
        return;
    }

    try {  // ── guard: if ANY render error, show raw JSON instead of blank popup

        // ── Extract document rows from detect response ──
        var rows = _piiExtractRows(data);
        var flatRows = rows.map(function (r) { return _piiFlatRow(r); });
        _piiTableData.headers = [];
        _piiTableData.rows = flatRows;

        // Build ordered headers (put common id/name fields first)
        var allKeys = {}, priorityKeys = ['doc_id', 'document_id', 'file_id', 'filename', 'file_name', 'document_name', 'name'];
        flatRows.forEach(function (r) { Object.keys(r).forEach(function (k) { allKeys[k] = true; }); });
        var headers = priorityKeys.filter(function (k) { return allKeys[k]; });
        Object.keys(allKeys).forEach(function (k) { if (headers.indexOf(k) < 0) headers.push(k); });
        _piiTableData.headers = headers;

        // ── Compute KPIs from actual API responses ──
        // /richness returns: total_documents, documents_with_pii, total_pii_incidents,
        //   average_density_score, pii_type_frequency, severity_totals,
        //   overall_richness_rating, pii_coverage_rate
        // /average_pii_doc returns: total_documents, total_pii_incidents,
        //   average_pii_per_document, per_document_breakdown
        // /detect returns: case_id, total_chunks, chunks_analyzed, sources_analyzed, analysis, summary

        // Total documents (source files, not chunks)
        var total = rich.total_documents || avg.total_documents || data.total_documents || data.total || 0;
        if (!total && data.sources_analyzed) total = data.sources_analyzed.length || 0;
        if (!total) total = rows.length || 0;

        // PII detected (documents containing PII)
        var found = rich.documents_with_pii || rich.total_pii_incidents || avg.total_pii_incidents || data.pii_found || data.pii_count || 0;

        // Average PII per document
        var avgPii = avg.average_pii_per_document || rich.average_pii_per_document || avg.average || data.average_pii_per_doc || 0;

        // PII richness / density score (0–10)
        var richVal = rich.average_density_score || rich.pii_density_score || rich.richness || rich.score || data.richness || 0;

        // ── Categories for charts ──
        // rich.pii_type_frequency is an object {type: count}; convert to array
        var catMap = {};
        var typeFreq = rich.pii_type_frequency || data.pii_type_frequency || {};
        if (typeof typeFreq === 'object' && !Array.isArray(typeFreq)) {
            Object.keys(typeFreq).forEach(function (k) {
                catMap[k.toLowerCase()] = typeFreq[k] || 0;
            });
        }
        // Fallback: try per_document_breakdown from avg or per_document_profiles from rich
        if (!Object.keys(catMap).length) {
            var profiles = rich.per_document_profiles || avg.per_document_breakdown || [];
            if (Array.isArray(profiles)) {
                profiles.forEach(function (p) {
                    var ptc = p.pii_type_counts || {};
                    Object.keys(ptc).forEach(function (k) {
                        catMap[k.toLowerCase()] = (catMap[k.toLowerCase()] || 0) + (ptc[k] || 0);
                    });
                    var ptf = p.pii_types_found || [];
                    if (Array.isArray(ptf)) {
                        ptf.forEach(function (t) {
                            var tk = (typeof t === 'string' ? t : (t.type || 'unknown')).toLowerCase();
                            if (!catMap[tk]) catMap[tk] = (catMap[tk] || 0) + 1;
                        });
                    }
                });
            }
        }
        // Also try legacy array formats
        var cats = data.categories || data.pii_types || avg.categories || rich.categories || [];
        if (!Object.keys(catMap).length && cats.length) {
            cats.forEach(function (c) { catMap[(c.type || c.name || 'unknown').toLowerCase()] = c.count || c.occurrences || 0; });
        }
        // Build sorted cats array from catMap
        if (Object.keys(catMap).length) {
            cats = Object.keys(catMap).sort(function (a, b) { return catMap[b] - catMap[a]; }).map(function (k) { return { type: k, count: catMap[k] }; });
        }

        // Dynamically discover actual severity for each category from the detailed rows (flatRows)
        var catSeverityMap = {};
        flatRows.forEach(function (r) {
            var typeKey = null;
            var sevKey = null;
            Object.keys(r).forEach(function (k) {
                var lk = k.toLowerCase();
                if (lk.indexOf('type') >= 0 || lk.indexOf('category') >= 0 || lk.indexOf('entity') >= 0) {
                    typeKey = k;
                }
                if (/severity|risk|level/i.test(lk)) {
                    sevKey = k;
                }
            });
            if (typeKey && sevKey && r[typeKey]) {
                var catName = String(r[typeKey]).toLowerCase();
                var sevVal = String(r[sevKey]).toLowerCase();
                var rank = sevVal.indexOf('critical') >= 0 ? 4 : sevVal.indexOf('high') >= 0 ? 3 : sevVal.indexOf('medium') >= 0 ? 2 : 1;
                
                var existingRank = 0;
                if (catSeverityMap[catName]) {
                    var ex = catSeverityMap[catName];
                    existingRank = ex === 'critical' ? 4 : ex === 'high' ? 3 : ex === 'medium' ? 2 : 1;
                }
                if (rank > existingRank) {
                    catSeverityMap[catName] = sevVal.indexOf('critical') >= 0 ? 'critical' : sevVal.indexOf('high') >= 0 ? 'high' : sevVal.indexOf('medium') >= 0 ? 'medium' : 'low';
                }
            }
        });
        
        // Also map some inherent severities for standard types via substring matching
        function getInherentSeverity(name) {
            name = (name || '').toLowerCase();
            if (/ssn|social security/i.test(name)) return 'high'; // maps to red pill
            if (/credit|card|cc\b|payment|bank|account|financial/i.test(name)) return 'high';
            if (/password|credential|patient|health|medical|biometric|passport|driver|license/i.test(name)) return 'high';
            if (/dob|date of birth/i.test(name)) return 'medium';
            if (/phone|address|location/i.test(name)) return 'low';
            if (/name|email|ip\b/i.test(name)) return 'low';
            return null;
        }

        // ── Risk score & level ──
        // Best source: overall_richness_rating from /richness endpoint
        var ratingMap = { 'critical': 90, 'high': 70, 'medium': 45, 'low': 20, 'none': 0 };
        var rating = (rich.overall_richness_rating || '').toLowerCase();
        var score = 0;
        if (ratingMap[rating] != null) {
            score = ratingMap[rating];
        } else {
            // Fallback: compute from severity_totals
            var sev = rich.severity_totals || {};
            var sevTotal = (parseInt(sev.high) || 0) + (parseInt(sev.medium) || 0) + (parseInt(sev.low) || 0);
            if (sevTotal > 0) {
                var weighted = (parseInt(sev.high) || 0) * 3 + (parseInt(sev.medium) || 0) * 2 + (parseInt(sev.low) || 0) * 1;
                score = Math.min(100, Math.round((weighted / (sevTotal * 3)) * 100));
            } else if (found > 0) {
                score = Math.min(100, Math.round((found / Math.max(total, 1)) * 150));
            }
        }
        score = Math.min(100, Math.max(0, parseInt(score) || 0));

        // Safegaurd: if the detailed CSV rows contain 'high' or 'critical' severity, force the UI to reflect it
        var hasHighSeverity = false;
        flatRows.forEach(function(r) {
            Object.keys(r).forEach(function(k) {
                if (/severity|risk|level/i.test(k)) {
                    var val = String(r[k]).toLowerCase();
                    if (val.indexOf('high') >= 0 || val.indexOf('critical') >= 0 || val.indexOf('severe') >= 0) {
                        hasHighSeverity = true;
                    }
                }
            });
        });
        if (hasHighSeverity && score < 70) {
            score = 75; // artificial boost to push it into 'high'
        }

        var rl = score >= 70 ? 'high' : score >= 30 ? 'medium' : 'low';
        var rc = SEVC[rl];
        var rEmoji = rl === 'high' ? '\uD83D\uDEA8' : rl === 'medium' ? '\u26A0\uFE0F' : '\u2705';
        var avgDisplay = (typeof avgPii === 'number' ? avgPii.toFixed(2) : String(avgPii || '0.00'));
        var richDisplay = (typeof richVal === 'number' ? richVal.toFixed(2) : String(richVal || '0.00'));

        // ── API status bar ──
        var apiBar = [
            results.detect ? '<span class="pii-api-ok">\u2713 detect</span>' : '<span class="pii-api-err">\u2717 detect</span>',
            results.average ? '<span class="pii-api-ok">\u2713 avg_pii_doc</span>' : '<span class="pii-api-err">\u2717 avg_pii_doc</span>',
            results.richness ? '<span class="pii-api-ok">\u2713 richness</span>' : '<span class="pii-api-err">\u2717 richness</span>'
        ].join('');

        var html = '<div class="pii-api-bar">' + apiBar + '</div>';
        html += '<div class="pii-risk-banner pii-rb-' + rl + '">' + rEmoji + '&nbsp; Risk Level: <strong>' + rl.toUpperCase() + '</strong>&nbsp;&nbsp;<span style="opacity:.75">Score: ' + score + '%</span></div>';
        html += '<div class="pii-kpi-row">' +
            _piiKpi('\uD83D\uDCC2', total, 'Total Documents', '#3498db') +
            _piiKpi('\uD83D\uDD0D', found, 'PII Detected', '#9b59b6') +
            _piiKpi('\uD83D\uDCC8', avgDisplay, 'Avg PII / Doc', '#e67e22') +
            _piiKpi('\uD83D\uDCAF', richDisplay, 'PII Richness', '#1abc9c') +
            '</div>';

        // ── Tabs ──
        html += '<div class="pii-tabs">' +
            '<button class="pii-tab pii-tab-active" data-tab="detail">\uD83D\uDCCB Detailed PII Data</button>' +
            '<button class="pii-tab" data-tab="analytics">\uD83D\uDCCA Analytics</button>' +
            '</div>';

        // ── DETAIL TAB: Excel-like table ──
        html += '<div class="pii-tab-pane" id="pii-tab-detail">';
        html += '<div class="pii-tbl-toolbar">' +
            '<button class="pii-csv-btn" onclick="_piiExportCsv()">\u2B07 Export CSV</button>' +
            '<span class="pii-tbl-count">' + flatRows.length + ' document(s) &bull; ' + headers.length + ' column(s)</span>' +
            '</div>';
        if (flatRows.length > 0 && headers.length > 0) {
            html += '<div class="pii-tbl-wrap"><table class="pii-tbl"><thead><tr>';
            headers.forEach(function (h) { html += '<th>' + escapeHtml(h.replace(/_/g, ' ')) + '</th>'; });
            html += '</tr></thead><tbody>';
            flatRows.forEach(function (row, i) {
                html += '<tr class="' + (i % 2 === 0 ? 'pii-tr-even' : 'pii-tr-odd') + '">';
                headers.forEach(function (h) {
                    var v = String(row[h] == null ? '' : row[h]);
                    html += '<td title="' + escapeHtml(v) + '">' + escapeHtml(v) + '</td>';
                });
                html += '</tr>';
            });
            html += '</tbody></table></div>';
        } else {
            html += '<div class="pii-raw"><pre>' + escapeHtml(JSON.stringify(data, null, 2)) + '</pre></div>';
        }
        html += '</div>';

        // ── ANALYTICS TAB ──
        html += '<div class="pii-tab-pane pii-tab-hidden" id="pii-tab-analytics">';
        html += '<div class="pii-gauge-section"><div class="pii-gauge-head"><span>Risk Exposure Meter</span><span>' + score + ' / 100</span></div><div class="pii-gauge-track"><div class="pii-gauge-bar" data-w="' + score + '" style="width:0%;background:linear-gradient(90deg,' + rc + ',' + (rl === 'high' ? '#ff6b6b' : rl === 'medium' ? '#ffd166' : '#06d6a0') + ')"></div></div><div class="pii-gauge-ticks"><span>Low</span><span>Medium</span><span>High</span><span>Critical</span></div></div>';
        if (cats.length > 0) {
            html += '<div class="pii-charts-row"><div class="pii-chart-card"><div class="pii-chart-head">\uD83E\uDD67 PII Distribution</div><canvas id="pii-donut"></canvas></div><div class="pii-chart-card"><div class="pii-chart-head">\uD83D\uDCCA Occurrences by Type</div><canvas id="pii-bar"></canvas></div></div>';
        }
        html += '<div class="pii-section-head">\uD83D\uDCCB Detected PII Categories</div>';
        if (cats.length > 0) {
            var maxCnt = Math.max.apply(null, cats.map(function (c) { return c.count || c.occurrences || 1; }));
            html += '<div class="pii-cat-list">';
            cats.forEach(function (cat) {
                var key = (cat.type || cat.name || '').toLowerCase().replace(/[\s\-]+/g, '_');
                var meta = PII_META[key] || { icon: '\uD83D\uDCC4', color: '#607d8b' };
                var cnt = cat.count || cat.occurrences || 0;
                
                // Determine severity from dynamic map, then inherent pattern matcher, then default to low
                var sev = 'low';
                var searchKey = (cat.type || cat.name || '').toLowerCase();
                
                // Try dynamic map first (if CSV parsing picked up severity)
                if (catSeverityMap[searchKey]) {
                    sev = catSeverityMap[searchKey];
                    if (sev === 'critical') sev = 'high';
                } else {
                    // Try robust inherent severity matching
                    var inherent = getInherentSeverity(searchKey);
                    if (inherent) {
                        sev = inherent;
                    } else {
                        // Fallback to strict count thresholds
                        sev = cat.severity || (cnt > 50 ? 'high' : cnt > 10 ? 'medium' : 'low');
                    }
                }

                var pct = Math.round((cnt / Math.max(maxCnt, 1)) * 100);
                html += '<div class="pii-cat-row"><div class="pii-cat-l"><span class="pii-cat-ico">' + meta.icon + '</span><span class="pii-cat-nm">' + escapeHtml(cat.type || cat.name || 'Unknown') + '</span></div><div class="pii-cat-bar-wrap"><div class="pii-cat-bar" data-w="' + pct + '" style="width:0%;background:' + meta.color + '"></div></div><div class="pii-cat-r"><span class="pii-cat-cnt">' + cnt + '</span><span class="pii-sev pii-sev-' + sev + '">' + sev.toUpperCase() + '</span></div></div>';
            });
            html += '</div>';
        } else {
            html += '<p style="color:#999;font-size:13px;padding:12px 0">No category breakdown available from API.</p>';
        }
        html += '</div>';

        $('#' + targetId).fadeOut(180, function () {
            var $self = $(this);
            try {
                $self.html(html).fadeIn(280);
                // Re-enable the generate button (PII report page)
                $('#pii-gen-btn').prop('disabled', false)
                    .html('<i class="bi-shield-shaded"></i>&nbsp;Generate Report');

                // Tab switching
                $(document).off('click.piitab').on('click.piitab', '.pii-tab', function () {
                    var tab = $(this).data('tab');
                    $('.pii-tab').removeClass('pii-tab-active');
                    $(this).addClass('pii-tab-active');
                    $('.pii-tab-pane').addClass('pii-tab-hidden');
                    $('#pii-tab-' + tab).removeClass('pii-tab-hidden');
                    // Render charts when analytics tab becomes visible
                    if (tab === 'analytics' && cats.length > 0 && typeof Chart !== 'undefined' && !_piiCharts.length) {
                        _piiBuildCharts(cats, PII_META);
                    }
                });

                // Animate KPI counters
                $('.pii-kpi-val[data-target]').each(function () {
                    var $el = $(this), raw = $el.data('target'), t = parseFloat(raw) || 0;
                    var isFloat = String(raw).indexOf('.') >= 0;
                    $({ n: 0 }).animate({ n: t }, {
                        duration: 1400, easing: 'swing',
                        step: function () { $el.text(isFloat ? this.n.toFixed(2) : Math.round(this.n)); },
                        complete: function () { $el.text(isFloat ? t.toFixed(2) : t); }
                    });
                });
            } catch (cbErr) {
                // Ensure element never stays permanently hidden if callback throws
                $self.show();
            }
        });
    } catch (renderErr) {
        // If rendering fails for any reason, show raw JSON instead of disappearing
        _showRaw('Render error (showing raw API data)', renderErr);
    }
}

function _piiBuildCharts(cats, PII_META) {
    var labels = cats.map(function (c) { return c.type || c.name || 'Unknown'; });
    var counts = cats.map(function (c) { return c.count || c.occurrences || 0; });
    var colors = cats.map(function (c) { var k = (c.type || c.name || '').toLowerCase().replace(/[\s\-]+/g, '_'); return (PII_META[k] || { color: '#607d8b' }).color; });
    setTimeout(function () {
        $('.pii-gauge-bar[data-w]').animate({ width: $('.pii-gauge-bar').data('w') + '%' }, 1100);
        $('.pii-cat-bar[data-w]').each(function (i) { var $b = $(this); setTimeout(function () { $b.animate({ width: $b.data('w') + '%' }, 500); }, i * 90); });
        var dc = document.getElementById('pii-donut');
        if (dc) _piiCharts.push(new Chart(dc, { type: 'doughnut', data: { labels: labels, datasets: [{ data: counts, backgroundColor: colors, borderWidth: 3, borderColor: '#fff', hoverOffset: 12 }] }, options: { responsive: true, cutout: '65%', plugins: { legend: { position: 'bottom', labels: { font: { size: 10 }, padding: 8, boxWidth: 10 } } }, animation: { animateRotate: true, duration: 1000 } } }));
        var bc = document.getElementById('pii-bar');
        if (bc) _piiCharts.push(new Chart(bc, { type: 'bar', data: { labels: labels, datasets: [{ data: counts, backgroundColor: colors, borderRadius: 6, borderWidth: 0 }] }, options: { indexAxis: 'y', responsive: true, plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } }, y: { grid: { display: false } } }, animation: { duration: 1000 } } }));
    }, 200);
}



function sendTemplateQuestion(event, type) {
    event.preventDefault();
    var questions = {
        'responsive': 'Which documents are responsive to the legal matter?',
        'privileged': 'Which documents may be legally privileged?',
        'smoking_gun': 'Are there any smoking gun documents in this case?',
        'timeline': 'What is the timeline of key events in this case?',
        'key_parties': 'Who are the key parties and what are their roles in this case?'
    };
    var q = questions[type] || type;
    $('.question_input').val(q);
    $('#chat-welcome').fadeOut(150);
    $('#chat-chips').slideUp(150);
    document.casifyAIForm.dispatchEvent(new Event('submit'));
}

function generatePiiReport() {
    var selectedOpt = $('#pii-page-case-select option:selected');
    var caseName = (selectedOpt.text() || '').trim() || 'Unknown Case';
    var numericCaseId = null;
    var nameMatch = caseName.match(/case_?(\d+)$/i);
    if (nameMatch) { numericCaseId = nameMatch[1]; }
    if (!numericCaseId) {
        var projectId = selectedOpt.data('project-id');
        if (projectId) {
            var pm = String(projectId).match(/(\d+)$/);
            if (pm && pm[1] !== '0') numericCaseId = pm[1];
        }
    }
    _piiCharts.forEach(function (c) { try { c.destroy(); } catch (e) { } });
    _piiCharts = [];
    var $area = $('#pii-report-area');
    if (!numericCaseId) {
        $area.html(
            '<div style="padding:40px;text-align:center">' +
            '<div class="pii-err"><div class="pii-err-icon">📂</div>' +
            '<div class="pii-err-title">No Valid Case Selected</div>' +
            '<div class="pii-err-msg">Please select a specific case before generating the PII report.</div>' +
            '</div></div>'
        );
        return;
    }
    $('#pii-gen-btn').prop('disabled', true).html('<i class="bi-hourglass-split"></i>&nbsp;Scanning&hellip;');
    $area.html(_piiScannerHtml());
    // Persist case selection so dropdown survives page navigation; track in-progress scan
    try {
        localStorage.setItem('_piiCaseDbId', String(selectedOpt.val()));
        localStorage.setItem('_piiCaseName', caseName);
        localStorage.removeItem('_piiLastScan');
        localStorage.setItem('_piiScanInProgress', JSON.stringify({
            caseDbId: String(selectedOpt.val()),
            numericCaseId: numericCaseId,
            caseName: caseName,
            ts: Date.now()
        }));
    } catch (e) { }
    _piiStartScan(numericCaseId, caseName, 'pii-report-area');
}

function _piiNotifyTabDone(caseName) {
    var origTitle = document.title;
    var alertTitle = '\uD83D\uDD34 PII Ready \u2014 ' + origTitle;
    document.title = alertTitle;

    // Blink the browser tab title every 1.5s
    var blinkTimer = setInterval(function () {
        document.title = (document.title === alertTitle) ? origTitle : alertTitle;
    }, 1500);

    // When user returns to this tab, render results
    var onVisible = function () {
        if (!document.hidden) {
            document.removeEventListener('visibilitychange', onVisible);
            clearInterval(blinkTimer);
            document.title = origTitle;
            if (_piiPendingResults) {
                var p = _piiPendingResults;
                _piiPendingResults = null;
                $('#pii-gen-btn').prop('disabled', false)
                    .html('<i class="bi-shield-shaded"></i>&nbsp;Generate Report');
                _piiRender(p.caseName, p.results, p.targetId);
            }
        }
    };
    document.addEventListener('visibilitychange', onVisible);

    // Browser Notification (if permission already granted)
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('\u2705 PII Report Ready', {
            body: 'Scan complete for: ' + caseName,
            icon: './images/FreeEED-01.png'
        });
    }
}

function _piiToast(html, duration) {
    duration = duration || 5000;
    var $t = $('<div class="pii-toast"><div class="pii-toast-inner">' + html + '</div></div>');
    $('body').append($t);
    setTimeout(function () { $t.addClass('pii-toast-show'); }, 10);
    setTimeout(function () { $t.removeClass('pii-toast-show'); setTimeout(function () { $t.remove(); }, 400); }, duration);
}

$(document).ready(function () {
    $("#allCasesCheckbox").change(function () {
        $("#case_Select").prop('disabled', this.checked);
    });

    // Auto-submit case change so the backend session updates selectedCase
    $("#case_Select").change(function () {
        if (document.change) {
            document.change.submit();
        }
    });

    // ── PII Report page init ──
    if ($('#pii-report-area').length) {
        // Ask for notification permission so we can alert when scan finishes in background
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }

        // 1) Restore dropdown to last-used case (survives page navigation)
        try {
            var savedDbId = localStorage.getItem('_piiCaseDbId');
            if (savedDbId) {
                var $savedOpt = $('#pii-page-case-select option[value="' + savedDbId + '"]');
                if ($savedOpt.length) { $savedOpt.prop('selected', true); }
            }
        } catch (e) { }

        // 2) When user changes case, update server session via silent AJAX + save to localStorage
        $('#pii-page-case-select').on('change', function () {
            var newDbId = $(this).val();
            try { localStorage.setItem('_piiCaseDbId', newDbId); } catch (e) { }
            $.ajax({
                type: 'POST', url: 'piireport.html',
                data: { action: 'changecase', id: newDbId }, error: function () { }
            });
        });

        // 3a) If a scan was interrupted by page navigation, auto-restart it
        var _scanResumed = false;
        try {
            var inProg = localStorage.getItem('_piiScanInProgress');
            if (inProg) {
                var ip = JSON.parse(inProg);
                if (ip && ip.numericCaseId && (Date.now() - ip.ts) < 300000) { // 5-min TTL
                    localStorage.removeItem('_piiScanInProgress');
                    var $ipOpt = $('#pii-page-case-select option[value="' + ip.caseDbId + '"]');
                    if ($ipOpt.length) { $ipOpt.prop('selected', true); }
                    _scanResumed = true;
                    _piiToast('\uD83D\uDD04 Resuming interrupted scan &mdash; <strong>' + escapeHtml(ip.caseName) + '</strong>');
                    setTimeout(function () {
                        $('#pii-gen-btn').prop('disabled', true).html('<i class="bi-hourglass-split"></i>&nbsp;Scanning&hellip;');
                        $('#pii-report-area').html(_piiScannerHtml());
                        _piiStartScan(ip.numericCaseId, ip.caseName, 'pii-report-area');
                    }, 400);
                } else {
                    localStorage.removeItem('_piiScanInProgress');
                }
            }
        } catch (e) { }

        // 3b) Restore last completed scan from localStorage (if < 10 minutes old)
        if (!_scanResumed) {
            try {
                var saved = localStorage.getItem('_piiLastScan');
                if (saved) {
                    var parsed = JSON.parse(saved);
                    if (parsed && parsed.results && (Date.now() - parsed.ts) < 600000) {
                        var $matchOpt = $('#pii-page-case-select option').filter(function () {
                            return $.trim($(this).text()) === parsed.caseName;
                        });
                        if ($matchOpt.length) { $matchOpt.prop('selected', true); }
                        _piiToast('\u26A1 Restoring last scan &mdash; <strong>' + escapeHtml(parsed.caseName) + '</strong>');
                        setTimeout(function () {
                            _piiRender(parsed.caseName, parsed.results, 'pii-report-area');
                        }, 600);
                    }
                }
            } catch (e) { }
        }
    }

    // ── Case Summary page init ──
    if ($('#cs-report-area').length) {
        try {
            var csDbId = localStorage.getItem('_csCaseDbId');
            if (csDbId) {
                var $csOpt = $('#cs-case-select option[value="' + csDbId + '"]');
                if ($csOpt.length) $csOpt.prop('selected', true);
            }
        } catch (e) { }
        $('#cs-case-select').on('change', function () {
            try { localStorage.setItem('_csCaseDbId', $(this).val()); } catch (e) { }
        });
    }
});

/* ═══════════════════════════════════════════════════════════════
   CASE INTELLIGENCE SUMMARY — 8-dimension AI analysis
═══════════════════════════════════════════════════════════════ */

var CS_CATEGORIES = [
    {
        id: 'actors', icon: '\uD83D\uDC65', label: 'Actors & Identity', color: '#3498db',
        question: 'Who are all the actors (senders, recipients, authors, signers) in this case? Identify aliases, name variants, shared accounts. Classify actors as internal, external, or unknown. Which appear only once vs consistently? List the most important actors and explain why each matters for the investigation.'
    },
    {
        id: 'relationships', icon: '\uD83D\uDD17', label: 'Relationships & Connections', color: '#9b59b6',
        question: 'What connections exist between actors? Who communicates with whom and how often? Is communication one-way or reciprocal? How strong are connections by frequency, duration, and topic? Who are the most central, influential, or controlling actors — who initiates, approves, or bridges different groups?'
    },
    {
        id: 'groups', icon: '\uD83C\uDFE2', label: 'Groups & Structure', color: '#e67e22',
        question: 'What clusters or groups emerge from the documents? Identify formal teams vs informal groups, internal vs external clusters, stable vs temporary groupings. Are there silos or isolated sub-networks? Who communicates outside normal channels? Flag any red-flag structural patterns.'
    },
    {
        id: 'timeline', icon: '\uD83D\uDCC5', label: 'Time & Change', color: '#27ae60',
        question: 'How do relationships and actor roles evolve over time? When do new actors appear or disappear? When do connections intensify or dissolve? Do changes correlate with key events? Who becomes more or less influential over time? Are there leadership shifts, sudden delegation, or crisis behavior patterns?'
    },
    {
        id: 'topics', icon: '\uD83D\uDCA1', label: 'Topics, Events & Intent', color: '#f39c12',
        question: 'What topics connect actors? What events can be inferred — decisions, approvals, incidents, transactions? Provide a chronological event timeline with supporting documents. Who knew what and when? Identify earliest mentions of key issues, who was informed first, and who was informed late or excluded.'
    },
    {
        id: 'behavior', icon: '\u26A0\uFE0F', label: 'Behavior & Anomalies', color: '#e74c3c',
        question: 'Are there behavioral anomalies? Identify: communication outside normal hierarchy, sudden volume spikes, unusual after-hours activity, use of personal or external channels. Are there missing or silent actors who should have been present? Are there signs of attempts to obscure activity — vague language, increased use of attachments, sudden switches to informal phrasing?'
    },
    {
        id: 'evidence', icon: '\uD83D\uDD2C', label: 'Evidence & Confidence', color: '#1abc9c',
        question: 'What specific evidence (documents, metadata, text excerpts, frequency counts) supports the main conclusions? For each key finding rate confidence as High, Medium, or Low and explain the reasoning. Identify conflicting signals and alternative interpretations. Format as: Finding | Confidence: High/Medium/Low | Evidence: [details]'
    },
    {
        id: 'review', icon: '\uD83C\uDFAF', label: 'Review & Actionability', color: '#e91e63',
        question: 'What actors or relationships should be prioritized for human review — high influence combined with sensitive topics, anomalous behavior, or central connectors? What follow-up questions should a reviewer ask? Which documents or time periods need deeper review? What hypotheses does the data support: coordination vs coincidence, awareness vs ignorance, policy compliance vs deviation?'
    }
];

function generateCaseSummary() {
    var $select = $('#cs-case-select');
    var selectedOpt = $select.find('option:selected');
    var caseName = $.trim(selectedOpt.text()) || 'Unknown Case';
    var caseDbId = selectedOpt.val();
    var projectId = selectedOpt.data('project-id');

    var numericCaseId = null;
    var nm = caseName.match(/case_?(\d+)$/i);
    if (nm) numericCaseId = nm[1];
    if (!numericCaseId && projectId) {
        var pm = String(projectId).match(/(\d+)$/);
        if (pm && pm[1] !== '0') numericCaseId = pm[1];
    }
    if (!numericCaseId) numericCaseId = String(caseDbId);

    var aiApiUrl = String($('#cs-ai-url').val() || '').replace(/\/+$/, '');
    if (!aiApiUrl) {
        $('#cs-report-area').html('<div style="padding:48px;text-align:center;color:#888"><div style="font-size:48px;opacity:0.3;margin-bottom:12px">\u2699\uFE0F</div>AI API URL not configured. Please check <strong>App Settings</strong>.</div>');
        return;
    }

    try { localStorage.setItem('_csCaseDbId', String(caseDbId)); } catch (e) { }

    $('#cs-analyze-btn').prop('disabled', true).html('<i class="bi-hourglass-split"></i>&nbsp;Analyzing\u2026');
    $('#cs-report-area').html(_csScannerHtml(caseName));

    var results = {};
    var errCount = 0;
    var total = CS_CATEGORIES.length;
    var POLL_INTERVAL = 3000;

    // Start a background job, poll until done
    function _csStartAndPoll(question, csId) {
        var d = $.Deferred();
        $.ajax({
            type: 'GET',
            url: aiApiUrl + '/advisors/retrieval/start',
            data: { question: question, case_id: String(csId) },
            dataType: 'json', timeout: 15000,
            success: function (resp) {
                if (resp && resp.job_id) {
                    _csPoll(resp.job_id, d);
                } else {
                    d.resolve({ _error: true });
                }
            },
            error: function () { d.resolve({ _error: true }); }
        });
        return d.promise();
    }

    function _csPoll(jobId, deferred) {
        setTimeout(function () {
            $.ajax({
                type: 'GET',
                url: aiApiUrl + '/advisors/retrieval/status',
                data: { job_id: jobId },
                dataType: 'json', timeout: 15000,
                success: function (resp) {
                    if (resp.status === 'done') {
                        deferred.resolve(resp.result);
                    } else if (resp.status === 'error') {
                        deferred.resolve({ _error: true, _msg: resp.error });
                    } else {
                        _csPoll(jobId, deferred);
                    }
                },
                error: function () {
                    // Network glitch — retry
                    setTimeout(function () { _csPoll(jobId, deferred); }, POLL_INTERVAL);
                }
            });
        }, POLL_INTERVAL);
    }

    // ── Process categories in parallel batches of 3 for speed ──
    var BATCH_SIZE = 3;
    var _csDoneCount = 0;

    function _processBatch(startIdx) {
        if (startIdx >= total) {
            setTimeout(function () { _csFinalise(caseName, total, errCount); }, 500);
            return;
        }
        var endIdx = Math.min(startIdx + BATCH_SIZE, total);
        var promises = [];

        for (var i = startIdx; i < endIdx; i++) {
            (function (idx) {
                var cat = CS_CATEGORIES[idx];
                _csDotState(idx, 'active', 'Analyzing\u2026');

                var p = _csStartAndPoll(cat.question, numericCaseId).then(function (d) {
                    if (d && d._error) {
                        results[cat.id] = { answer: null, sources: [] };
                        errCount++;
                        _csDotState(idx, 'err', '\u2717 Error');
                    } else {
                        results[cat.id] = d;
                        _csDotState(idx, 'done', '\u2713 Done');
                    }
                    _csDoneCount++;
                    _csProgSet(Math.round((_csDoneCount / total) * 100));
                    _csRenderCard(cat, results[cat.id], caseDbId);
                });
                promises.push(p);
            })(i);
        }

        // When this batch finishes, start the next batch
        $.when.apply($, promises).then(function () {
            _processBatch(endIdx);
        });
    }
    _processBatch(0);
}

function _csScannerHtml(caseName) {
    var stepsHtml = '';
    CS_CATEGORIES.forEach(function (cat, i) {
        stepsHtml +=
            '<div class="cs-step" id="cs-step-' + i + '">' +
            '<div class="cs-dot" id="cs-dot-' + i + '"></div>' +
            '<span class="cs-step-label">' + cat.icon + '\u00a0' + escapeHtml(cat.label) + '</span>' +
            '<span class="cs-step-status" id="cs-ss-' + i + '">Queued</span>' +
            '</div>';
    });
    return '<div class="cs-scanner">' +
        '<div class="cs-scanner-title">\uD83E\uDDE0 Analyzing: <em style="font-weight:500">' + escapeHtml(caseName) + '</em></div>' +
        '<div class="cs-scanner-sub">Running 8 AI analyses sequentially &mdash; each section appears as it completes.</div>' +
        '<div class="cs-prog-wrap"><div class="cs-prog-bar" id="cs-prog-bar"></div></div>' +
        '<span class="cs-prog-pct" id="cs-prog-pct">0% complete</span>' +
        '<div class="cs-steps">' + stepsHtml + '</div>' +
        '</div>' +
        '<div class="cs-grid" id="cs-cards-grid" style="margin-top:20px"></div>';
}

function _csDotState(i, state, txt) {
    $('#cs-dot-' + i).removeClass('cs-dot-active cs-dot-done cs-dot-err').addClass('cs-dot-' + state);
    $('#cs-step-' + i).removeClass('cs-step-active cs-step-done cs-step-err').addClass('cs-step-' + state);
    $('#cs-ss-' + i).text(txt || '');
}

function _csProgSet(pct) {
    $('#cs-prog-bar').css('width', pct + '%');
    $('#cs-prog-pct').text(pct + '% complete');
}

function _csRenderCard(cat, data, caseDbId) {
    var answer = (data && data.answer) ? data.answer : null;
    var sources = (data && data.sources && data.sources.length) ? data.sources : [];

    // Detect confidence level from AI response text
    var conf = 'medium';
    if (answer) {
        var al = answer.toLowerCase();
        if (/confidence:\s*high/i.test(al) || /\bhigh confidence\b/i.test(al)) conf = 'high';
        else if (/confidence:\s*low/i.test(al) || /\blow confidence\b|\buncertain\b|\blimited evidence\b/i.test(al)) conf = 'low';
    } else {
        conf = 'error';
    }

    var confLabels = { high: 'High Confidence', medium: 'Medium Confidence', low: 'Low Confidence', error: 'Unavailable' };
    var bodyHtml = answer ? _csFormatAnswer(answer)
        : '<div style="color:#c0392b;font-size:12px">\u26A0\uFE0F Analysis unavailable — AI backend may be offline or case has no indexed documents.</div>';

    var srcHtml = '';
    if (sources.length) {
        srcHtml = '<div class="cs-card-sources"><span class="cs-src-lbl">Sources</span>';
        sources.forEach(function (src) {
            var href = getCurrentUrl() + '/search.html?caseid=' + encodeURIComponent(caseDbId) + '&query=UPI:' + encodeURIComponent(src);
            srcHtml += '<a class="cs-src-pill" href="' + href + '" target="_blank">' + escapeHtml(String(src)) + '</a>';
        });
        srcHtml += '</div>';
    }

    var cardHtml =
        '<div class="cs-card" id="cs-card-' + cat.id + '" style="--cat-color:' + cat.color + ';opacity:0;transform:translateY(14px)">' +
        '<div class="cs-card-head">' +
        '<span class="cs-card-ico">' + cat.icon + '</span>' +
        '<span class="cs-card-label">' + escapeHtml(cat.label) + '</span>' +
        '<span class="cs-card-conf cs-conf-' + conf + '">' + (confLabels[conf] || 'Medium Confidence') + '</span>' +
        '</div>' +
        '<div class="cs-card-body">' + bodyHtml + '</div>' +
        srcHtml +
        '</div>';

    $('#cs-cards-grid').append(cardHtml);
    // Animate card in
    setTimeout(function () {
        $('#cs-card-' + cat.id).css({ opacity: 1, transform: 'translateY(0)' });
    }, 40);
}

function _csFormatAnswer(raw) {
    var lines = String(raw).split('\n');
    var html = '';
    var inList = false;
    lines.forEach(function (line) {
        var esc = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        // Bold **text**
        esc = esc.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        // Italic *text* (simple non-greedy)
        esc = esc.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        // Strip leading ### headings
        esc = esc.replace(/^#{1,3}\s+/, '');
        // Inline confidence badges
        esc = esc.replace(/Confidence:\s*High/gi, 'Confidence: <span class="cs-inline-conf cs-conf-high">High</span>');
        esc = esc.replace(/Confidence:\s*Medium/gi, 'Confidence: <span class="cs-inline-conf cs-conf-medium">Medium</span>');
        esc = esc.replace(/Confidence:\s*Low/gi, 'Confidence: <span class="cs-inline-conf cs-conf-low">Low</span>');

        var trimmed = $.trim(esc);
        var isBullet = /^[-\u2022]\s/.test(trimmed) || /^\d+\.\s/.test(trimmed);
        if (isBullet) {
            if (!inList) { html += '<ul>'; inList = true; }
            html += '<li>' + trimmed.replace(/^[-\u2022\d\.]+\s*/, '') + '</li>';
        } else if (trimmed) {
            if (inList) { html += '</ul>'; inList = false; }
            // All-caps-ish short lines act as section sub-headers
            if (/^[A-Z][^a-z]{0,50}:$/.test($.trim(line))) {
                html += '<p style="margin:10px 0 3px;font-weight:700;color:#25005b">' + trimmed + '</p>';
            } else {
                html += '<p style="margin:4px 0">' + trimmed + '</p>';
            }
        }
    });
    if (inList) html += '</ul>';
    return html || '<p style="color:#aaa;font-size:12px">No content returned.</p>';
}

function _csFinalise(caseName, total, errCount) {
    var okCount = total - errCount;
    $('#cs-analyze-btn').prop('disabled', false).html('<i class="bi-cpu"></i>&nbsp;Re-analyze');

    // Collapse scanner
    $('.cs-scanner').slideUp(300);

    // Insert dark overview banner before the cards
    var ovHtml =
        '<div class="cs-overview" id="cs-overview" style="display:none">' +
        '<div class="cs-ov-icon">\uD83E\uDDE0</div>' +
        '<div class="cs-ov-body">' +
        '<div class="cs-ov-eyebrow">Case Intelligence Summary</div>' +
        '<div class="cs-ov-name">' + escapeHtml(caseName) + '</div>' +
        '<div class="cs-ov-meta">' + total + ' dimensions analyzed &mdash; ' + new Date().toLocaleTimeString() + '</div>' +
        '</div>' +
        '<div class="cs-ov-pills">' +
        '<span class="cs-ov-pill ok">\u2713 ' + okCount + ' section' + (okCount !== 1 ? 's' : '') + '</span>' +
        (errCount ? '<span class="cs-ov-pill err">\u2717 ' + errCount + ' error' + (errCount !== 1 ? 's' : '') + '</span>' : '') +
        '</div>' +
        '</div>';

    var actionHtml =
        '<div class="cs-action-bar" id="cs-action-bar">' +
        '<div class="cs-action-meta">Analysis complete for <strong>' + escapeHtml(caseName) + '</strong></div>' +
        '<div class="cs-action-btns">' +
        '<button class="cs-action-btn" onclick="window.print()"><i class="bi-printer"></i>&nbsp;Print Report</button>' +
        '</div>' +
        '</div>';

    $('#cs-cards-grid').before(ovHtml);
    $('#cs-overview').slideDown(300);
    $('#cs-cards-grid').after(actionHtml);
}