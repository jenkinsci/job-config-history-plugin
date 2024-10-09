function removeEntryFromTable(id, timestamp, name, message) {
    var confirmPhrase = message + timestamp + '?';
    if (confirm(confirmPhrase)) {
        var tableRow = document.getElementById(id);
        tableRow.parentNode.removeChild(tableRow);

        //redirect
        var xmlHttp = new XMLHttpRequest();
        var url = 'deleteRevision?timestamp=' + timestamp;
        if (name != null) {
            url += "&name=" + name;
        }
        xmlHttp.open("POST", url, true);
        xmlHttp.setRequestHeader(document.head.getAttribute('data-crumb-header'), document.head.getAttribute('data-crumb-value'));
        xmlHttp.send(null);
    } else {
        return false;
    }
}

document.addEventListener('DOMContentLoaded', function() {

    const buttons = document.querySelectorAll('.jenkins-button--destructive');

    const targetDiv = document.querySelector('#target-div');
    let jobName = null
    if (targetDiv) {
        jobName = targetDiv.getAttribute('jobName');
    }

    buttons.forEach(button => {
            button.addEventListener('click', function(event) {
                const configNr = this.getAttribute('data-config-nr');
                const configDate = this.getAttribute('data-config-date');
                const message = this.getAttribute('data-message-text');
                removeEntryFromTable(`table-row-${configNr}`, configDate, jobName, message);
            });
        }
    );
});