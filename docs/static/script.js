const lang_list = ["ko", "ja", "en"];

const queryString = location.search;
const urlParams = new URLSearchParams(queryString);

var menu_data = {};
var currentPage = urlParams.has("page") ? urlParams.get("page") : "index";
var currentLang = urlParams.has("hl") ? urlParams.get("hl") : get_default_language();
if (!lang_list.includes(currentLang)) currentLang = "en";

var fromApp = (urlParams.has("from_app") && urlParams.get("from_app") == "true");

// Labels that are not in menu.json
const UI_TEXT = {
    ko: { toc: "이 페이지에서", prev: "이전", next: "다음" },
    ja: { toc: "このページの内容", prev: "前へ", next: "次へ" },
    en: { toc: "On this page", prev: "Previous", next: "Next" },
};

function get_default_language() {
    const navLang = navigator.language;
    if (navLang.includes("ko-KR") || navLang.includes("ko")) return "ko";
    else if (navLang.includes("ja-JP") || navLang.includes("ja")) return "ja";
    else return "en";
}

function pageOrder() {
    return Array.from(document.querySelectorAll(".nav-page")).map(e => e.getAttribute("data-page"));
}

function pageTitle(page) {
    const el = document.querySelector(".nav-page[data-page='" + page + "']");
    return el ? el.innerText : page;
}

function groupTitle(page) {
    const el = document.querySelector(".nav-page[data-page='" + page + "']");
    const title = el && el.closest(".nav-group").querySelector(".nav-group-title");
    return title ? title.innerText : "";
}

/** Post-process the rendered markdown: tables, images, links and the page outline. */
function enhanceContent(root) {
    root.querySelectorAll("table").forEach(table => {
        table.classList.add("table");
        const wrap = document.createElement("div");
        wrap.className = "table-wrap";
        table.parentNode.insertBefore(wrap, table);
        wrap.appendChild(table);
    });

    // Screenshots open at full size, which helps on phones.
    root.querySelectorAll("img").forEach(img => {
        if (img.closest("a")) return;
        const link = document.createElement("a");
        link.href = img.getAttribute("src");
        link.target = "_blank";
        link.rel = "noopener";
        link.className = "figure-link";
        img.parentNode.insertBefore(link, img);
        link.appendChild(img);
    });

    root.querySelectorAll("a[href]").forEach(a => {
        if (!a.classList.contains("figure-link")) a.target = "_blank";
    });

    root.querySelectorAll("span.link").forEach(el => {
        el.addEventListener("click", event => {
            setPage(currentLang, event.currentTarget.getAttribute("data-move"), true);
        });
    });

    // Outline from the section headings
    const toc = document.getElementById("toc");
    toc.innerHTML = "";
    const headings = Array.from(root.querySelectorAll("h5"));
    headings.forEach((h, i) => {
        h.id = "section-" + (i + 1);
        const li = document.createElement("li");
        const a = document.createElement("a");
        a.textContent = h.textContent;
        a.href = "#" + h.id;
        a.addEventListener("click", event => {
            event.preventDefault();
            h.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        li.appendChild(a);
        toc.appendChild(li);
    });
    document.getElementById("toc-panel").classList.toggle("is-empty", headings.length < 2);
    watchSections(headings);
}

var sectionObserver = null;
function watchSections(headings) {
    if (sectionObserver) sectionObserver.disconnect();
    if (!("IntersectionObserver" in window) || headings.length === 0) return;
    sectionObserver = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) return;
            document.querySelectorAll("#toc a").forEach(a =>
                a.classList.toggle("active", a.getAttribute("href") === "#" + entry.target.id));
        });
    }, { rootMargin: "-80px 0px -70% 0px" });
    headings.forEach(h => sectionObserver.observe(h));
}

function renderPager(page) {
    const order = pageOrder();
    const i = order.indexOf(page);
    const text = UI_TEXT[currentLang];
    const pager = document.getElementById("pager");
    pager.innerHTML = "";
    [[order[i - 1], "prev", text.prev, "bi-arrow-left"], [order[i + 1], "next", text.next, "bi-arrow-right"]].forEach(([target, kind, label, icon]) => {
        const card = document.createElement(target ? "a" : "span");
        card.className = "pager-card pager-" + kind + (target ? "" : " is-empty");
        if (target) {
            card.href = "?hl=" + currentLang + "&page=" + target;
            card.innerHTML = '<span class="pager-label">' + (kind === "prev" ? '<i class="bi ' + icon + '"></i>' : "") +
                label + (kind === "next" ? '<i class="bi ' + icon + '"></i>' : "") + "</span>" +
                '<span class="pager-title"></span>';
            card.querySelector(".pager-title").textContent = pageTitle(target);
            card.addEventListener("click", event => {
                event.preventDefault();
                setPage(currentLang, target, true);
            });
        }
        pager.appendChild(card);
    });
}

function setPage(lang, page, isNext) {
    if (!menu_data.hasOwnProperty(lang)) {
        const menu_url = location.origin + location.pathname + "md/" + lang + "/menu.json";
        fetch(menu_url)
            .then((response) => response.json())
            .then((json) => {
                menu_data[lang] = json;
                setPage(lang, page, isNext);
            })
            .catch((error) => console.log(error));
        return;
    }
    if (!pageOrder().includes(page)) page = "index";
    currentLang = lang;
    currentPage = page;
    document.documentElement.lang = lang;

    const langTextData = menu_data[lang]["text"];
    Object.entries(langTextData).forEach(([k, v]) => {
        document.querySelectorAll("[data-text='" + k + "']").forEach(el => el.innerText = v);
    });
    document.getElementById("lang-code").innerText =
        document.querySelector(".dropdown-item[data-lang='" + lang + "']").innerText;
    document.getElementById("toc-title").innerText = UI_TEXT[lang].toc;

    document.querySelectorAll(".nav-page").forEach(e => e.classList.toggle("active", e.getAttribute("data-page") === page));
    document.getElementById("breadcrumb").innerText = groupTitle(page);

    const content = document.getElementById("rendered-md-content");
    const markdown_url = location.origin + location.pathname + "md/" + currentLang + "/" + currentPage + ".md";
    fetch(markdown_url)
        .then((response) => response.text())
        .then((text) => {
            content.innerHTML = marked.parse(text);
            enhanceContent(content);
            renderPager(page);
        })
        .catch((error) => console.log(error));
    window.scrollTo({ top: 0, behavior: "instant" });

    const state = { "hl": lang, "page": page };
    const url = location.pathname + "?hl=" + lang + "&page=" + page;
    if (!fromApp && isNext) {
        history.pushState(state, "", url);
    } else {
        history.replaceState(state, "", url);
    }
    document.title = langTextData["nav_app_brand"] + " | " + pageTitle(page);

    // Close the drawer after picking a page on small screens
    const drawer = bootstrap.Offcanvas.getInstance(document.getElementById("sidebar"));
    if (drawer) drawer.hide();
}

window.addEventListener("popstate", (event) => {
    if (event.state != null) {
        setPage(event.state.hl, event.state.page, false);
    }
});

document.querySelectorAll(".dropdown-item[data-lang]").forEach(el => {
    el.addEventListener("click", event => {
        setPage(event.currentTarget.getAttribute("data-lang"), currentPage, true);
    });
});

document.querySelectorAll(".nav-page").forEach(el => {
    el.setAttribute("href", "?page=" + el.getAttribute("data-page"));
    el.addEventListener("click", event => {
        event.preventDefault();
        setPage(currentLang, event.currentTarget.getAttribute("data-page"), true);
    });
});

document.getElementById("brand-link").addEventListener("click", event => {
    event.preventDefault();
    setPage(currentLang, "index", true);
});

// Theme
function applyTheme(theme) {
    document.documentElement.setAttribute("data-bs-theme", theme);
    document.querySelector("#theme-toggle i").className = theme === "dark" ? "bi bi-sun" : "bi bi-moon-stars";
}

function savedTheme() {
    try { return localStorage.getItem("theme"); } catch (e) { return null; }
}

applyTheme(document.documentElement.getAttribute("data-bs-theme"));

document.getElementById("theme-toggle").addEventListener("click", () => {
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
