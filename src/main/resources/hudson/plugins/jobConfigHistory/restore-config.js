Behaviour.specify(".restore-config", "jobConfigHistory", 0, function(button) {
  button.addEventListener("click", function(event) {
    const formTemplate = document.getElementById("restore-config-template");
    const form = formTemplate.firstElementChild.cloneNode(true);
    const type = button.dataset.type;
    if (type === "name") {
      form.action="restore?" + new URLSearchParams({ name: button.dataset.name });
    } else {
      form.action="restore?" + new URLSearchParams({ timestamp: button.dataset.timestamp });
    }
    const data = type === "name" ? button.dataset.name : button.dataset.timestamp;
    form.querySelector(".restore-config-form-content").innerText = formTemplate.dataset.description + data;
    dialog.form(form, {
      title: formTemplate.dataset.title,
      okText: formTemplate.dataset.okText,
      type: "destructive",
    }).then(() => {}, () => {});
  });
});