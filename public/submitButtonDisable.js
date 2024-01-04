let form = document.querySelector('[disable-on-submit="true"]')
if(form !== null) {
    form.addEventListener('submit', function () {
        let button = document.querySelector('form > .govuk-button')
        if(button != null) {
            button.setAttribute('disabled', '')
            button.setAttribute('aria-disabled', 'true')
        }
    })
}