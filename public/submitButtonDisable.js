let button = document.querySelector('[disable-on-submit="true"]')
if(button !== null) {
    button.addEventListener('click', function () {
        button.setAttribute('disabled', '')
        button.setAttribute('aria-disabled', 'true')
    })
}