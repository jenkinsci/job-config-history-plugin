function removeEntryFromTable(id, timestamp) {
    if (confirm('Do you really want to delete this history entry?')) {
        var tableRow = document.getElementById(id);
        tableRow.parentNode.removeChild(tableRow);

        //redirect
        var xmlHttp = new XMLHttpRequest();
        var url = 'deleteRevision?timestamp=' + timestamp;
        xmlHttp.open("GET", url, true);
        xmlHttp.send(null);
    } else {
        return false;
    }
}