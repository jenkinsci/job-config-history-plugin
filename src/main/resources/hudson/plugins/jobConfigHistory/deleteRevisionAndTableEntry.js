//name is an optional parameter for job deletion history and system config history
function removeEntryFromTable(id, timestamp, name, message) {
    const confirmPhrase = message + timestamp + '?';
    if (dialog.confirm("Delete", {
      message: confirmPhrase,
      type: "destructive",
    }).then(() => {
      const tableRow = document.getElementById(id);
      tableRow.parentNode.removeChild(tableRow);

      //redirect
      const xmlHttp = new XMLHttpRequest();
      const url = 'deleteRevision?timestamp=' + timestamp;
      if (name != null) {
        url += "&name=" + name;
      }
      xmlHttp.open("POST", url, true);
      xmlHttp.setRequestHeader(document.head.getAttribute('data-crumb-header'), document.head.getAttribute('data-crumb-value'));
      xmlHttp.send(null);
    }, () => {}));
}

document.addEventListener('DOMContentLoaded', function () {
    const targetDiv = document.querySelector('#target-div');
    let jobName = null;
    if (targetDiv) {
        jobName = targetDiv.getAttribute('jobName');
    }

    const systemConfigDeleteButtons = document.querySelectorAll('.system-config-history-delete-button');
    systemConfigDeleteButtons.forEach((button) => {
        button.addEventListener('click', () => {
            const configNr = button.getAttribute('data-config-nr');
            const configDate = button.getAttribute('data-config-date');
            const message = button.getAttribute('data-message-text');
            removeEntryFromTable(`table-row-${configNr}`, configDate, jobName, message);
        });
    });

    const agentConfigDeleteButtons = document.querySelectorAll('.agent-config-history-delete-button');
    agentConfigDeleteButtons.forEach((button) => {
        button.addEventListener('click', () => {
            const configNr = button.getAttribute('data-config-nr');
            const configDate = button.getAttribute('data-config-date');
            const message = button.getAttribute('data-message-text');
            removeEntryFromTable(`table-row-${configNr}`, configDate, null, message);
        });
    });
});
