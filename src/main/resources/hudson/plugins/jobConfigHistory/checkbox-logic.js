// If _.showChangeReasonCommentWindow gets unchecked, uncheck _.changeReasonCommentIsMandatory as well.
Behaviour.specify('INPUT[name="_.showChangeReasonCommentWindow"]', 'CheckboxBehaviour', -999, function (el) {
    el.addEventListener('change', function (e) {
        if (!el.checked) {
            var c = document.querySelector('INPUT[name="_.changeReasonCommentIsMandatory"]');
            if (null != c) {
                c.checked = false;
            }
        }
    });
});

