function frame_transform() {
  ((document.getElementById("game_frame").style.transform =
    `scale(${window.innerWidth / 1200})`),
    (document.getElementById("game_frame").style.transformOrigin = "top left"));
}
window.addEventListener("load", () => {
  var targetNode = document.getElementById("root");
  var config = { childList: true, subtree: true };
  var callback = (mutationList, observer) => {
    for (const mutation of mutationList) {
      if (mutation.type === "childList") {
        mutation.addedNodes.forEach((node) => {
          if (
            (node.nodeType === Node.ELEMENT_NODE) &
            (node.parentNode.className == "gamesResetStyle")
          ) {
            if (node.tagName.toLowerCase() == "main") {
              node.style.cssText += "margin:0; padding:0";
              let n = 0;
              (window.addEventListener("resize", () => {
                (clearTimeout(n), (n = setTimeout(frame_transform, 10)));
              }),
                frame_transform());
            } else {
              node.style.display = "none";
            }
          }
        });
      }
    }
  };
  var observer = new MutationObserver(callback);
  observer.observe(targetNode, config);
});
