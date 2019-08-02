//name is an optional parameter for job deletion history and system config history
function removeEntryFromTable(id, timestamp, name) {
    console.log("tedst");
    var confirmPhrase = 'Do you really want to delete the history entry ' + timestamp + '?';
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