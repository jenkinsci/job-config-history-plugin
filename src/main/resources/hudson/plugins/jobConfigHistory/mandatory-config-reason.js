Behaviour.specify('INPUT.change-reason-comment', 'ConfigHistoryChangeReason', -99, function (e) {
    e.onchange = e.oninput = e.onkeyup =  (function() {
        var empty = (this.value.trim() == '');
        var buttons =  document.querySelectorAll('div.bottom-sticker-inner > button.jenkins-button');
        buttons.forEach(function (b) {
            b.disabled = empty;
        });
    }).bind(e);
    e.onchange();
});
