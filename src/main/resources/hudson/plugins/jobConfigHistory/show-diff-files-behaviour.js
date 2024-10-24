window.addEventListener("DOMContentLoaded", () => {
    hljs.initHighlightingOnLoad();

    var showVersionDiffsJs = true;

    function toggleShowHideVersionDiffsJs(button, hideText, showText) {
        showVersionDiffsJs = !showVersionDiffsJs;
        if (showVersionDiffsJs === true) {
            document.getElementById("tbody_versionDiffsShown").style.display = '';
            document.getElementById("tbody_versionDiffsHidden").style.display = 'none';

            button.value = hideText;
        } else {
            document.getElementById("tbody_versionDiffsShown").style.display = 'none';
            document.getElementById("tbody_versionDiffsHidden").style.display = '';

            button.value = showText;
        }
    }

    document.querySelector("#showHideVersionDiffsJsButton").addEventListener("click", (event) => {
        const { hideText, showText } = event.target.dataset;

        toggleShowHideVersionDiffsJs(event.target, hideText, showText);
    });
});
