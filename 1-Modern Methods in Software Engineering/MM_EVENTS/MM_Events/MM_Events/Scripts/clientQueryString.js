window.onpaint = preloadFunc();

function preloadFunc() {
    if (localStorage["redirectFlag"] == "0") {
        localStorage["redirectFlag"] = "1";
        var test = localStorage["querystrings"];
        document.location = test;
    }

}