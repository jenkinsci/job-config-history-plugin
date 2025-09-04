// job-config-history-plugin singleton lib object
window.jchpLib = window.jchpLib || {
    'reasonNeedsReset': false,
    'dialogShown': false,
    'reason': null,
    'mandatory': false,
    'dlgMsg': 'defaultDialogMessage',
    'dlgCancel': 'defaultCancel',
    'disabled': false,

    'init': function(e) {
        this.reason = e.querySelector("INPUT.change-reason-comment");
        this.disabled = e.dataset.enabled == 'false';
        this.mandatory = e.dataset.mandatory == 'true';
        this.dlgMsg = e.dataset.dialogMessage;
        if (!this.mandatory) {
            this.dlgMsg += ' ' + e.dataset.dialogOptional;
        }
        this.dlgCancel = e.dataset.dialogCancel;
        // Hide corresponding parent optional container in node config and check the
        // corresponding checkbox in order to get the change message actually submitted.
        var p = e.closest('DIV.optionalBlock-container');
        if (p != null) {
            p.style.display = 'none';
            var cb = p.querySelector('INPUT.optional-block-control');
            if (null != cb) {
              if (e.dataset.enabled == 'true') {
                cb.checked = true;
              } else {
                cb.checked = false;
              }
            }
        }
    },

    'resetReason': function() {
        this.reason.value = '';
        this.reasonNeedsReset = false;
    },

    'handleFormData': function(e) {
        this.reasonNeedsReset = true;
    },

    'handleClick': function(e) {
        if (this.disabled) {
          return;
        }
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

Behaviour.specify('DIV.change-reason-container', 'ConfigHistoryInit', -999, function (e) {
    jchpLib.init.bind(jchpLib)(e);
});

const re = /^\/computer\/[^\/]+\/configure$/;
// Only install our hooks if we are on a computer config page
if (re.test(window.location.pathname.replace(new RegExp("^" + document.head.dataset.rooturl), ''))) {
    Behaviour.specify('FORM', 'ConfigHistoryForm', -999, function (e) {
        e.addEventListener('formdata', jchpLib.handleFormData.bind(jchpLib));
    });

    Behaviour.specify('div.bottom-sticker-inner > button.jenkins-button', 'ConfigHistory', -999, function (e) {
        e.addEventListener('click', jchpLib.handleClick.bind(jchpLib));
    });
}
