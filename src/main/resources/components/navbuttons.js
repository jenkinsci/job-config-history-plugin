Behaviour.specify('BUTTON.jch-navbutton', 'navbuttonClickHandler', 999, function (el) {
    el.addEventListener('click', function (e) {
        location.href = this.dataset.url;
    });
});
