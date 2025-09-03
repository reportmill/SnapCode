
import LZString from './lz-string.min.js';

//const SNAPCODE_URL = 'https://reportmill.com/SnapCode/app/app09?loader=none#embed:';
const SNAPCODE_URL = 'http://localhost:8080?loader=none#embed:';

const SnapCode = {

    addSnapCodeToElementForId,
    addPlayButtonToElementForId
};

//
// Adds SnapCode to given element.
//
function addSnapCodeToElementForId(containerId)
{
    // Remove any existing runner
    removeExistingSnapCodeRunner();

    // Get container and ensure it can host absolutely positioned children
    const container = document.getElementById(containerId);
    if (getComputedStyle(container).position === "static")
        container.style.position = "relative";

    // Remove play button
    const playButton = container.querySelector('#SnapCodeRunnerPlayButton');
    if (playButton !== null)
        container.removeChild(playButton);;

    // Make sure it's at least 200px tall
    const containerOriginalHeight = container.style.height;
    const containerOriginalClientHeight = container.clientHeight;
    if (containerOriginalClientHeight < 240)
        container.style.height = '240px';

    // Get java string from parent element and Base64 encode with lzwstring
    const javaStr = container.innerText;
    var javaStrLzw = LZString.compressToBase64(javaStr);

    // Create iframe for parent element
    const iframe = document.createElement("iframe");
    iframe.id = 'SnapCodeRunner';
    iframe.originalHeight = containerOriginalHeight;
    iframe.originalClientHeight = containerOriginalClientHeight;
    iframe.src = SNAPCODE_URL + javaStrLzw;
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

    // Create close button, add action and add to container
    const closeButton = getCloseButton();
    closeButton.addEventListener("click", () => { removeExistingSnapCodeRunner(containerId); });
    container.appendChild(closeButton);
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

    // Get play button, add action and add to container
    const playButton = getPlayButton();
    playButton.addEventListener("click", () => { addSnapCodeToElementForId(containerId); });
    container.appendChild(playButton);
}

//
// Returns a play button.
//
function getPlayButton()
{
    // Create the button
    const button = document.createElement("button");
    button.id = 'SnapCodeRunnerPlayButton';
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

    // Return
    return button;
}

//
// Returns a close button.
//
function getCloseButton()
{
    // Create the button
    const button = document.createElement("button");
    button.id = 'SnapCodeRunnerCloseButton';
    button.style.fontSize = "18px";
    button.style.border = "none";
    button.style.background = "none";
    button.style.color = "lightgray";
    button.style.cursor = "pointer";
    button.style.transition = "color 0.3s";
    button.style.position = "absolute";
    button.style.top = "10px";
    button.style.right = "10px";
    button.style.zIndex = '99999';

    // Add the icon
    const icon = document.createElement("i");
    icon.className = "fa-solid fa-xmark";
    button.appendChild(icon);

    // Hover effect
    button.addEventListener("mouseover", () => { button.style.color = "close"; });
    button.addEventListener("mouseout", () => { button.style.color = "lightgray"; });

    // Return
    return button;
}

//
// Removes existing SnapCode runner
//
function removeExistingSnapCodeRunner()
{
    // Get old SnapCode runner (just return if not found)
    const oldSnapCodeRunner = document.getElementById('SnapCodeRunner');
    if (oldSnapCodeRunner === null)
        return;

    // If old runner was too small, restore original parent container height
    if (oldSnapCodeRunner.originalClientHeight < 200)
        oldSnapCodeRunner.parentNode.style.height = oldSnapCodeRunner.originalHeight;

    // Remove old runner from its parent container
    const container = oldSnapCodeRunner.parentNode;
    container.removeChild(oldSnapCodeRunner);

    // Remove close button
    const closeButton = document.getElementById('SnapCodeRunnerCloseButton');
    if (closeButton !== null)
        container.removeChild(closeButton);

    // Add play button back
    addPlayButtonToElementForId(container.id);
}

//
// Adds this: <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
//
function addFontAwesome()
{
    if (window.isFontAwesome) return; window.isFontAwesome = true;
    const linkElement = document.createElement('link');
    linkElement.rel = 'stylesheet';
    linkElement.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css';
    document.head.appendChild(linkElement);

    // Create iframe to load SnapCode to make sure it's hot
    const hiddenIframe = document.createElement('iframe');
    hiddenIframe.src = SNAPCODE_URL.slice(0, -1);
    hiddenIframe.style.display = 'none';
    document.body.appendChild(hiddenIframe);
    hiddenIframe.onload = function() { document.body.removeChild(hiddenIframe); };
}

export default SnapCode;