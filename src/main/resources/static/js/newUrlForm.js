const newUrlForm = document.getElementById('newUrlForm')

const generateSlugBtn = document.getElementById('generate_slug')
const urlTextField = document.getElementById('url')
const slugTextField = document.getElementById('slug')

const errorMessageElement = document.getElementById('invalidSlug')
const generateSlugUrl = '/user/urls/generate'
const EMPTY = ''

function getFirstLine(input) {
    const split = (input).split('\n')
    let text = ''
    while (text === '') {
        text = split.shift()
    }
    return text
}

async function fetchSlug(url, params) {
    if (!params.get('url')) return Promise.reject(EMPTY)
    if (!params) return Promise.reject(EMPTY)

    const csrfToken = document.querySelector("meta[name='_csrf']").content
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").content

    try {
        const response = await fetch(`${url}?${params}`, {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken,
                'Content-Type': 'application/json'
            },
        })

        if (response.ok) {
            const line = getFirstLine(await response.text())
            if (!line) return Promise.reject(EMPTY)

            return Promise.resolve(line ? line : EMPTY)
        }

        return Promise.reject(EMPTY)
    } catch (error) {
        console.log(error)
    }
}

function setSlugField(line) {
    errorMessageElement.innerHTML = EMPTY
    urlTextField.classList.remove('is-invalid')

    slugTextField.value = line ? line : EMPTY
}

function errorFetchingSlug() {
    const invalidSlugMessage = document.querySelector("meta[name='invalidSlugMessage']").content
    errorMessageElement.innerHTML = invalidSlugMessage
    urlTextField.classList.add('is-invalid')
}

generateSlugBtn.onclick = (event) => {
    event.preventDefault()

    const params = new URLSearchParams({
        url: urlTextField.value
    })
    fetchSlug(generateSlugUrl, params).then(setSlugField, errorFetchingSlug)
}
