const lang_list = ["ko", "ja", "en"];

const queryString = location.search;
const urlParams = new URLSearchParams(queryString);

var menu_data = {};
var currentPage = urlParams.has("page") ? urlParams.get("page") : "index";
var currentLang = urlParams.has("hl") ? urlParams.get("hl") : get_default_language();
if (!lang_list.includes(currentLang)) currentLang = "en";

var fromApp = (urlParams.has("from_app") && urlParams.get("from_app") == "true");

var renderer = new marked.Renderer();
renderer.link = function(href, title, text) {
    var link = marked.Renderer.prototype.link.call(this, href, title, text);
    return link.replace("<a","<a target='_blank' ");
};
renderer.table = function(token) {
    var link = marked.Renderer.prototype.table.call(this, token);
    return link.replace("<table","<table class='table'");
};
marked.setOptions({ renderer: renderer });

function get_default_language() {
    const navLang = navigator.language;
    if (navLang.includes("ko-KR") || navLang.includes("ko")) return "ko";
    else if (navLang.includes("ja-JP") || navLang.includes("ja")) return "ja";
    else return "en";
}

function setPage(lang, page, isNext) {
    // language settings
    if (!menu_data.hasOwnProperty(lang)) {
        const menu_url = location.origin + location.pathname + "md/" + lang + "/menu.json";
        fetch(menu_url)
            .then((response) => response.json())
            .then((json) => {
                menu_data[lang] = json;
                setPage(lang, page, isNext);
            })
            .catch((error) => console.log(error));
    } else {
        currentLang = lang;
        currentPage = page;

        const langTextData = menu_data[lang]["text"];
        Object.entries(langTextData).forEach(([k, v]) => {
            document.querySelector("span[data-text='" + k + "']").innerText = v;
        });
        document.getElementById("lang-code").innerText = document.querySelector(".dropdown-item[data-lang='" + lang + "']").innerText;

        // page settings
        document.getElementById('rendered-md-content').innerHTML = "";
        document.querySelectorAll(".item.lv2").forEach(e => e.classList.remove("active"));
        const selectedPage = document.querySelector(".item.lv2[data-page='"+ page +"']");
        selectedPage.classList.add("active");
        
        const markdown_url = location.origin + location.pathname + "md/" + currentLang + "/" + currentPage + ".md";
        fetch(markdown_url)
            .then((response) => response.text())
            .then((text) => {
                document.getElementById('rendered-md-content').innerHTML = marked.parse(text);
                document.querySelector("#page-name-text").innerText = selectedPage.firstChild.innerText;
                Array.from(document.querySelectorAll("#rendered-md-content span.link")).forEach((el) => {
                    el.addEventListener('click', (event) => {
                        currentPage = event.currentTarget.getAttribute("data-move");
                        setPage(currentLang, currentPage, true);
                    });
                });
            })
            .catch((error) => console.log(error));
        window.scrollTo({top: 0, behavior: 'instant'});

        const state = {"hl": lang, "page": page};
        const url = location.pathname + "?hl=" + lang + "&page=" + page;
        if (!fromApp && isNext) {
            history.pushState(state, "", url);
        } else {
            history.replaceState(state, "", url);
        }
        document.title = langTextData["nav_app_brand"] + " | " + selectedPage.firstChild.innerText;
    }                
}

window.addEventListener('popstate', (event) => {
    if (event.state != null) {
        setPage(event.state.hl, event.state.page, false);
    }    
});

const langDropdown = document.querySelectorAll(".dropdown-item");
Array.from(langDropdown).forEach(el => {
    el.addEventListener('click', (event) => {
        currentLang = event.currentTarget.getAttribute("data-lang");
        setPage(currentLang, currentPage, true);
    });
});

const sidebarItems = document.querySelectorAll(".sidebar .item.lv2");
Array.from(sidebarItems).forEach(el => {
    el.addEventListener('click', (event) => {
        currentPage = event.currentTarget.getAttribute("data-page");
        setPage(currentLang, currentPage, true);
        document.querySelector("#page-name-text").innerText = event.currentTarget.innerText;
        document.querySelector(".page-group-list").classList.add("d-none");
        document.querySelector(".page-show-icon").classList.add("bi-chevron-down");
        document.querySelector(".page-show-icon").classList.remove("bi-chevron-up");
    });
});

document.querySelector(".page-name").addEventListener('click', (event) => {
    document.querySelector(".page-group-list").classList.toggle("d-none");
    ["bi-chevron-down", "bi-chevron-up"].forEach(x => document.querySelector(".page-show-icon").classList.toggle(x));
})

function applyTheme(theme) {
    document.documentElement.setAttribute("data-bs-theme", theme);
    // The language menu sits in the always-dark navbar; let it follow the page theme.
    document.getElementById("lang-dropdown").setAttribute("data-bs-theme", theme);
    document.querySelector("#theme-toggle i").className = theme === "dark" ? "bi bi-sun" : "bi bi-moon-stars";
}

function savedTheme() {
    try { return localStorage.getItem("theme"); } catch (e) { return null; }
}

applyTheme(document.documentElement.getAttribute("data-bs-theme"));

document.getElementById("theme-toggle").addEventListener("click", (event) => {
    event.preventDefault();
    const next = document.documentElement.getAttribute("data-bs-theme") === "dark" ? "light" : "dark";
    try { localStorage.setItem("theme", next); } catch (e) {}
    applyTheme(next);
});

// Follow system changes until the visitor picks a theme.
window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (event) => {
    const saved = savedTheme();
    if (saved !== "light" && saved !== "dark" && !urlParams.has("theme")) {
        applyTheme(event.matches ? "dark" : "light");
    }
});

setPage(currentLang, currentPage, false);