// Shared word matching for all content scripts.
globalThis.ywf = {
  // "word" or "two words" match as whole words; "/regex/" is used as-is.
  toSource(p) {
    p = p.trim();
    if (p.length > 2 && p.startsWith('/') && p.endsWith('/')) return p.slice(1, -1);
    const escaped = p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\s+/g, '\\s+');
    return `(?<![\\p{L}\\p{N}])${escaped}(?![\\p{L}\\p{N}])`;
  },

  buildRegex(patterns) {
    const sources = (patterns || []).filter((p) => p && p.trim()).map(ywf.toSource);
    return sources.length ? new RegExp(sources.join('|'), 'iu') : null;
  },

  // Calls back with the policy config now and whenever the policy changes.
  // Without a system policy (e.g. Windows/Mac, loaded unpacked), falls back to default-words.txt.
  onConfig(callback) {
    const fallback = (cfg) =>
      fetch(chrome.runtime.getURL('default-words.txt'))
        .then((r) => r.text())
        .then((text) => callback({ ...cfg, patterns: text.split('\n').filter((l) => !l.trim().startsWith('#')) }))
        .catch(() => callback(cfg));
    const load = () =>
      chrome.storage.managed.get(null, (cfg) => {
        cfg = cfg || {};
        if (chrome.runtime.lastError || !cfg.patterns) fallback(cfg);
        else callback(cfg);
      });
    chrome.storage.onChanged.addListener((_, area) => area === 'managed' && load());
    load();
  },
};
