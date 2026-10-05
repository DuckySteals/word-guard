// Every site except YouTube: block pages whose address or title matches a blocked word.
(() => {
  let re = null;

  function urlText(href) {
    let decoded = href;
    try {
      decoded = decodeURIComponent(href.replace(/\+/g, ' '));
    } catch {}
    return `${decoded} ${decoded.replace(/[_\-/=&?.#:]+/g, ' ')}`;
  }

  function check() {
    if (!re) return;
    if (re.test(urlText(location.href)) || re.test(document.title || '')) {
      location.replace(chrome.runtime.getURL('blocked.html'));
    }
  }

  ywf.onConfig((cfg) => {
    re = ywf.buildRegex(cfg.patterns);
    check();
  });

  // Catch titles set after load and in-page navigation on single-page sites
  let last = '';
  setInterval(() => {
    const now = location.href + document.title;
    if (now !== last) {
      last = now;
      check();
    }
  }, 400);
})();
