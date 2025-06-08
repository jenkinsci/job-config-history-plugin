window.addEventListener("DOMContentLoaded", () => {
    hljs.initHighlightingOnLoad();

    var showVersionDiffsJs = true;

    function toggleShowHideVersionDiffsJs(button, hideText, showText) {
        showVersionDiffsJs = !showVersionDiffsJs;
        if (showVersionDiffsJs === true) {
            document.getElementById("tbody_versionDiffsShown").style.display = "table-row-group";
            document.getElementById("tbody_versionDiffsHidden").style.display = "none";

            button.innerText = hideText;
        } else {
            document.getElementById("tbody_versionDiffsShown").style.display = "none";
            document.getElementById("tbody_versionDiffsHidden").style.display = "table-row-group";

            button.innerText = showText;
        }
    }

    document.querySelector("#showHideVersionDiffsJsButton").addEventListener("click", (event) => {
        const { hideText, showText } = event.target.dataset;

        toggleShowHideVersionDiffsJs(event.target, hideText, showText);
    });
});
