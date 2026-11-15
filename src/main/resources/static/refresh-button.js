(() => {
    const style = document.createElement('style');
    style.textContent = `#autoservis-refresh{position:fixed;right:18px;bottom:18px;z-index:2147483000;border:0;border-radius:999px;padding:12px 17px;background:#172337;color:#fff;font:600 15px Arial,sans-serif;box-shadow:0 4px 16px #0003;cursor:pointer}#autoservis-refresh:hover{background:#263c5b}#autoservis-refresh:focus-visible{outline:3px solid #60a5fa;outline-offset:3px}@media(max-width:520px){#autoservis-refresh{right:12px;bottom:12px;padding:11px 14px}}`;
    document.head.appendChild(style);

    const button = document.createElement('button');
    button.id = 'autoservis-refresh';
    button.type = 'button';
    button.setAttribute('aria-label', 'Sayfayı yenile');
    button.textContent = '⟳ Yenile';
    button.addEventListener('click', () => {
        if (document.body.dataset.refreshMode === 'approval-status' && typeof window.refreshApprovalStatus === 'function') {
            window.refreshApprovalStatus();
            return;
        }
        const path = window.location.pathname;
        const lastPost = sessionStorage.getItem('autoservis-last-post');
        const mappings = {
            '/kayit-yap': '/kayit', '/giris-yap': '/giris', '/cikis': '/giris',
            '/arac-kaydet': '/araclar', '/arac-guncelle': '/araclar',
            '/servis-kaydet': '/servisler', '/servis-guncelle': '/servisler',
            '/sifre-kodu-gonder': '/sifre-unuttum', '/sifre-yenile': '/sifre-unuttum',
            '/musteri-sorgula': '/musteri', '/hesap-onay-durumu': '/giris'
        };
        let destination = mappings[path] || (lastPost === path ? mappings[path] : null);
        if (path === '/hesap-onay') {
            const token = document.querySelector('input[name="token"]')?.value;
            destination = token ? `${path}?token=${encodeURIComponent(token)}` : '/giris';
        }
        if (destination) {
            sessionStorage.removeItem('autoservis-last-post');
            window.location.assign(destination);
        } else {
            window.location.reload();
        }
    });
    document.addEventListener('submit', event => {
        const actionPath = new URL(event.target.action, window.location.href).pathname;
        sessionStorage.setItem('autoservis-last-post', actionPath);
    });
    if (sessionStorage.getItem('autoservis-last-post') !== window.location.pathname) {
        sessionStorage.removeItem('autoservis-last-post');
    }
    document.body.appendChild(button);
})();
