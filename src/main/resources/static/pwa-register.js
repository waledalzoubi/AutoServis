if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/service-worker.js").catch((error) => {
      console.error("AutoServis çevrimdışı sayfası kaydedilemedi:", error);
    });
  });
}

let installPrompt = null;
window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  installPrompt = event;
  document.querySelectorAll("[data-install-app]").forEach((button) => {
    button.hidden = false;
  });
});

window.addEventListener("appinstalled", () => {
  installPrompt = null;
  document.querySelectorAll("[data-install-app]").forEach((button) => {
    button.hidden = true;
  });
});

document.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-install-app]");
  if (!button || !installPrompt) return;
  installPrompt.prompt();
  await installPrompt.userChoice;
  installPrompt = null;
  button.hidden = true;
});
