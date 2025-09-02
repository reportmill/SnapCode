
import LZString from './lz-string.min.js';

const SnapCode = {

    addSnapCodeToElementForId,
    addPlayButtonToElementForId
};

//
// Adds SnapCode to given element.
//
function addSnapCodeToElementForId(containerId)
{
    // Get container and ensure it can host absolutely positioned children
    const container = document.getElementById(containerId);
    if (getComputedStyle(container).position === "static")
        container.style.position = "relative";

    // Get java string from parent element and Base64 encode with lzwstring
    const javaStr = container.innerText;
    var javaStrLzw = LZString.compressToBase64(javaStr);

    // Create iframe for parent element
    const iframe = document.createElement("iframe");
    iframe.src = 'http://localhost:8080#embed:' + javaStrLzw;
    iframe.style.border = "none";
    iframe.style.position = 'absolute';
    iframe.style.top = '0';
    iframe.style.left = '0';
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.zIndex = '9999';
    iframe.style.display = "block"; // avoids scrollbar glitches

    // Append iframe to container
    container.appendChild(iframe);
}

//
// Adds a SnapCode play button to element for given id.
//
function addPlayButtonToElementForId(containerId)
{
    addFontAwesome();

    // Get container and ensure it can host absolutely positioned children
    const container = document.getElementById(containerId);
    if (getComputedStyle(container).position === "static")
        container.style.position = "relative";

    // Create the button
    const button = document.createElement("button");
    button.style.fontSize = "18px";
    button.style.border = "none";
    button.style.background = "none";
    button.style.color = "lightgray";
    button.style.cursor = "pointer";
    button.style.transition = "color 0.3s";
    button.style.position = "absolute";
    button.style.top = "10px";
    button.style.right = "10px";

    // Add the icon
    const icon = document.createElement("i");
    icon.className = "fa-solid fa-play";
    button.appendChild(icon);

    // Hover effect
    button.addEventListener("mouseover", () => { button.style.color = "green"; });
    button.addEventListener("mouseout", () => { button.style.color = "lightgray"; });

    // Click action
    button.addEventListener("click", () => { addSnapCodeToElementForId(containerId); });

    // Append to container
    container.appendChild(button);
}

// Adds this: <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
function addFontAwesome()
{
    if (window.isFontAwesome) return; window.isFontAwesome = true;
    const linkElement = document.createElement('link');
    linkElement.rel = 'stylesheet';
    linkElement.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css';
    document.head.appendChild(linkElement);
}

export default SnapCode;