//name is an optional parameter for job deletion history and system config history
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
        xmlHttp.open("GET", url, true);
        xmlHttp.send(null);
    } else {
        return false;
    }
}