const lang_list = ["ko", "ja", "en"];

const queryString = location.search;
const urlParams = new URLSearchParams(queryString);

var menu_data = {};
var currentPage = urlParams.has("page") ? urlParams.get("page") : "index";
var currentLang = urlParams.has("hl") ? urlParams.get("hl") : "en";
if (!lang_list.includes(currentLang)) currentLang = "en";

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

function setPage(lang, page) {
    // language settings
    if (!menu_data.hasOwnProperty(lang)) {
        const menu_url = location.origin + location.pathname + "md/" + currentLang + "/menu.json";
        fetch(menu_url)
            .then((response) => response.json())
            .then((json) => {
                menu_data[lang] = json;
                setPage(lang, page);
            })
            .catch((error) => console.log(error));
    } else {
        const langTextData = menu_data[lang]["text"];
        Object.entries(langTextData).forEach(([k, v]) => {
            document.querySelector("span[data-text='" + k + "']").innerText = v;
        });
        document.getElementById("lang-code").innerText = document.querySelector(".dropdown-item[data-lang='" + lang + "']").innerText;

        // page settings
        document.querySelectorAll(".item.lv2").forEach(e => e.classList.remove("active"));
        const selectedPage = document.querySelector(".item.lv2[data-page='"+ page +"']");
        selectedPage.classList.add("active");
        document.querySelector("#page-name-text").innerText = selectedPage.firstChild.innerText;

        const markdown_url = location.origin + location.pathname + "md/" + currentLang + "/" + currentPage + ".md";
        fetch(markdown_url)
            .then((response) => response.text())
            .then((text) => {
                document.getElementById('rendered-md-content').innerHTML = marked.parse(text);
                Array.from(document.querySelectorAll("#rendered-md-content span.link")).forEach((el) => {
                    el.addEventListener('click', (event) => {
                        currentPage = event.currentTarget.getAttribute("data-move");
                        setPage(currentLang, currentPage);
                    });
                });
            })
            .catch((error) => console.log(error));
        window.scrollTo({top: 0, behavior: 'instant'});
        document.title = langTextData["nav_app_brand"] + " | " + selectedPage.firstChild.innerText;
        window.history.replaceState(null, null, location.pathname + "?hl=" + lang + "&page=" + page);
    }                
}

const langDropdown = document.querySelectorAll(".dropdown-item");
Array.from(langDropdown).forEach(el => {
    el.addEventListener('click', (event) => {
        currentLang = event.currentTarget.getAttribute("data-lang");
        setPage(currentLang, currentPage);
    });
});

const sidebarItems = document.querySelectorAll(".sidebar .item.lv2");
Array.from(sidebarItems).forEach(el => {
    el.addEventListener('click', (event) => {
        currentPage = event.currentTarget.getAttribute("data-page");
        setPage(currentLang, currentPage);
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

setPage(currentLang, currentPage);