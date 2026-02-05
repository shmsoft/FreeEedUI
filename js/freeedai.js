function onSubmit(event) {
    event.preventDefault();

    const container = $(".chat-wrapper")[0];
    const $questionInput = $(".question_input");
    const question = $questionInput.val();

    container.innerHTML += '<div class="question">' + escapeHtml(question) + '</div>';
    $('#send_question').prop('disabled', true);
    $questionInput.prop('disabled', true);

    const allCases = $("#allCasesCheckbox").prop("checked");
    // Normalize to prevent accidental double slashes when concatenating paths
    const aiApiUrl = String($("#aiApiUrl").val() || '').replace(/\/+$/, '');

    // --- runtime diagnostics: identify which JS file is actually running ---
    try {
        const currentScriptUrl = (document.currentScript && document.currentScript.src) ? document.currentScript.src : null;
        const loadedScript = (function () {
            const scripts = document.getElementsByTagName('script');
            for (let i = scripts.length - 1; i >= 0; i--) {
                const src = scripts[i].getAttribute('src') || '';
                if (src.indexOf('freeedai.js') >= 0) return scripts[i].src || src;
            }
            return null;
        })();
        const effectiveSrc = currentScriptUrl || loadedScript || '(inline/unknown)';
        window.__FREEEED_AI_UI_VERSION__ = 'freeedai.js 2026-02-05 endpoints:/advisors/retrieval/*';
        console.info('[AI Advisor] UI version:', window.__FREEEED_AI_UI_VERSION__);
        console.info('[AI Advisor] freeedai.js loaded from:', effectiveSrc);
        console.info('[AI Advisor] aiApiUrl:', aiApiUrl);

        $('#aiRuntimeDebug').remove();
        $('.chat-wrapper').prepend(
            '<div id="aiRuntimeDebug" class="answer"><small>' +
            'AI UI: ' + escapeHtml(window.__FREEEED_AI_UI_VERSION__) + '<br/>' +
            'Script: ' + escapeHtml(effectiveSrc) +
            '</small></div>'
        );
    } catch (e) {
        // ignore
    }

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

    function showRequestDebug(url, paramsObj) {
        try {
            const usp = new URLSearchParams();
            Object.keys(paramsObj || {}).forEach(function (k) {
                const v = paramsObj[k];
                if (Array.isArray(v)) {
                    v.forEach(function (vv) { usp.append(k, vv); });
                } else {
                    usp.set(k, v);
                }
            });
            $("#aiRequestDebug").remove();
            $('.chat-wrapper').prepend(
                '<div id="aiRequestDebug" class="answer"><small>Request: ' +
                escapeHtml(url + '?' + usp.toString()) +
                '</small></div>'
            );
        } catch (e) {
            // ignore
        }
    }

    // Optional: show server-side collection list for debugging (non-blocking)
    $.ajax({
        type: 'GET',
        // new endpoint (support both with/without trailing slash on server)
        url: aiApiUrl + '/advisors/retrieval/describe_index',
        dataType: 'json',
        success: function (indexInfo) {
            try {
                const names = [];
                if (Array.isArray(indexInfo)) {
                    indexInfo.forEach(function (x) {
                        if (x && x.name) names.push(x.name + '(' + x.count + ')');
                    });
                }
                if (names.length > 0) {
                    $("#aiIndexDebug").remove();
                    $('.chat-wrapper').prepend('<div id="aiIndexDebug" class="answer"><small>FastAPI collections: ' + escapeHtml(names.join(', ')) + '</small></div>');
                }
            } catch (e) {
                // ignore
            }
        }
    });

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

        $("#aiDebug").remove();
        $('.chat-wrapper').prepend('<div id="aiDebug" class="answer"><small>AI case_ids (numeric): ' + escapeHtml(caseIds.join(', ')) + '</small></div>');

        const params = { question: question, case_ids: caseIds };
        // all-cases endpoint moved under /advisors/retrieval
        showRequestDebug(aiApiUrl + '/advisors/retrieval/question_cases', params);

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

    $("#aiDebug").remove();
    $('.chat-wrapper').prepend('<div id="aiDebug" class="answer"><small>AI case_id (numeric): ' + escapeHtml(String(numericCaseId)) + '</small></div>');

    const params = { question: question, case_id: String(numericCaseId) };
    showRequestDebug(aiApiUrl + '/advisors/retrieval/question_case', params);

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
});