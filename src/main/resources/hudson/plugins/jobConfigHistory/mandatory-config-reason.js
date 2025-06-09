// job-config-history-plugin singleton lib object
window.jchpLib = window.jchpLib || {
    'reasonNeedsReset': false,
    'dialogShown': false,
    'reason': null,
    'mandatory': false,
    'dlgMsg': 'defaultDialogMessage',
    'dlgCancel': 'defaultCancel',

    'init': function(e) {
        this.reason = e;
        this.mandatory = e.dataset.mandatory == 'true';
        this.dlgMsg = e.dataset.dialogMessage;
        if (!this.mandatory) {
            this.dlgMsg += ' ' + e.dataset.dialogOptional;
        }
        this.dlgCancel = e.dataset.dialogCancel;
    },

    'resetReason': function() {
        this.reason.value = '';
        this.reasonNeedsReset = false;
    },

    'handleFormData': function(e) {
        this.reasonNeedsReset = true;
    },

    'handleClick': function(e) {
        var button = e.target;
        var isSubmit = button.classList.contains('jenkins-submit-button');
        if (this.reasonNeedsReset) {
            this.resetReason();
        }
        if ((this.reason.value.trim() == '') && !this.dialogShown) {
            e.preventDefault();
            e.stopImmediatePropagation();
            this.dialogShown = true;
            dialog.prompt(this.dlgMsg, {
                minWidth: '600px',
                okText: button.innerText.trim(),
                cancelText: this.dlgCancel,
                allowEmpty: !this.mandatory,
            }).then(
                (val) => {
                    this.reason.value = val;
                    if (this.underTest) {
                        this.dialogShown = false;
                    } else {
                        button.dispatchEvent(new Event('click'));
                    }
                },
                () => {
                    this.dialogShown = false;
                },
            );
        } else {
            this.dialogShown = false;
            if (isSubmit) {
                var f = document.querySelector('FORM[name="config"]');
                if (null != f) {
                    f.requestSubmit();
                }
            }
        }
    }
};

Behaviour.specify('INPUT.change-reason-comment', 'ConfigHistoryInit', -999, function (e) {
    jchpLib.init.bind(jchpLib)(e);
});

Behaviour.specify('FORM', 'ConfigHistoryForm', -999, function (e) {
    e.addEventListener('formdata', jchpLib.handleFormData.bind(jchpLib));
});

Behaviour.specify('div.bottom-sticker-inner > button.jenkins-button', 'ConfigHistory', -999, function (e) {
    e.addEventListener('click', jchpLib.handleClick.bind(jchpLib));
});
