
/**
 * This function takes an element and replaces it with a frame holding SnapCode in embed mode.
 */
function addSnapCodeToElement(parentElement)
{
    // Get java string from parent element and encode in lzwstring
    const javaStr = 'todo'; //parentElement.innerText;
    var javaStrLzw = javaStr + 'todo'; //lzwencode(javaStr)

    // Create iframe for parent element
    const iframe = document.createElement("iframe");
    iframe.src = 'http://localhost:8080#embed' + '#java:' + javaStrLzw;
    iframe.style.border = "none";
    iframe.style.width = "100%";
    iframe.style.height = "100%";
    iframe.style.display = "block"; // avoids scrollbar glitches

    // Append iframe to container
    container.appendChild(iframe);

    // Ensure the iframe always matches the container size
    const resizeObserver = new ResizeObserver(() => {
        iframe.style.width = container.clientWidth + "px";
        iframe.style.height = container.clientHeight + "px";
    });
    resizeObserver.observe(container);
}