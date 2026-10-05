(() => {
  const TILES = [
    'ytd-rich-item-renderer',
    'ytd-video-renderer',
    'ytd-compact-video-renderer',
    'ytd-grid-video-renderer',
    'ytd-playlist-renderer',
    'ytd-compact-playlist-renderer',
    'ytd-radio-renderer',
    'ytd-compact-radio-renderer',
    'ytd-channel-renderer',
    'ytd-movie-renderer',
    'ytd-reel-item-renderer',
    'ytd-playlist-panel-video-renderer',
    'ytd-comment-thread-renderer',
    'ytd-comment-view-model',
    'yt-lockup-view-model',
    'ytm-shorts-lockup-view-model',
    'ytm-shorts-lockup-view-model-v2',
    'ytm-video-with-context-renderer',
    'ytm-compact-video-renderer',
    '[role="listbox"] [role="option"]',
  ].join(',');

  const root = document.documentElement;
  root.classList.add('ywf-loading');

  let re = null;
  let hideShorts = true;

  function configure(cfg) {
    re = ywf.buildRegex(cfg.patterns);
    hideShorts = cfg.hideShorts !== false;
    root.classList.toggle('ywf-no-shorts', hideShorts);
    root.classList.remove('ywf-loading');
    scan();
  }

  function textOf(el) {
    const parts = [el.textContent];
    el.querySelectorAll('[aria-label],[title],a[href^="/@"]').forEach((n) => {
      parts.push(n.getAttribute('aria-label') || '', n.getAttribute('title') || '', n.getAttribute('href') || '');
    });
    return parts.join(' ');
  }

  function matches(text) {
    return !!(re && text && re.test(text));
  }

  function showBlock(message) {
    let box = document.getElementById('ywf-block');
    if (!box) {
      box = document.createElement('div');
      box.id = 'ywf-block';
      box.innerHTML = '<div></div><a href="https://www.youtube.com/">Go to YouTube home</a>';
      document.documentElement.appendChild(box);
    }
    box.firstChild.textContent = message;
    document.querySelectorAll('video').forEach((v) => {
      v.pause();
      v.muted = true;
    });
  }

  function clearBlock() {
    document.getElementById('ywf-block')?.remove();
  }

  function checkPage() {
    const url = new URL(location.href);

    if (hideShorts && url.pathname.startsWith('/shorts/')) {
      const id = url.pathname.split('/')[2];
      if (id) location.replace(`/watch?v=${id}`);
      return;
    }

    if (url.pathname === '/results' && matches(url.searchParams.get('search_query'))) {
      return showBlock('This search is blocked.');
    }

    if (url.pathname === '/watch') {
      const meta = document.querySelector('ytd-watch-metadata');
      const info = [
        meta?.querySelector('h1')?.textContent,
        meta?.querySelector('#owner')?.textContent,
        meta?.querySelector('#description')?.textContent,
        document.querySelector('meta[name="title"]')?.content,
      ].join(' ');
      if (matches(info)) return showBlock('This video is blocked.');
    }

    clearBlock();
  }

  function scan() {
    if (root.classList.contains('ywf-loading')) return;
    document.querySelectorAll(TILES).forEach((el) => {
      if (matches(textOf(el))) el.setAttribute('data-ywf-hidden', '');
      else el.removeAttribute('data-ywf-hidden');
    });
    checkPage();
  }

  let pending = false;
  function schedule() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(() => {
      pending = false;
      scan();
    });
  }

  new MutationObserver(schedule).observe(document, {
    childList: true,
    subtree: true,
    characterData: true,
    attributes: true,
    attributeFilter: ['title', 'aria-label', 'href'],
  });
  window.addEventListener('yt-navigate-finish', schedule);
  window.addEventListener('popstate', schedule);

  ywf.onConfig(configure);
})();
