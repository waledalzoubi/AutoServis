(() => {
    const style = document.createElement('style');
    style.textContent = `#autoservis-navigation{position:fixed;right:18px;bottom:18px;z-index:2147483000;display:flex;gap:8px}#autoservis-navigation button{position:static;border:0;border-radius:999px;padding:12px 17px;background:#172337;color:#fff;font:600 15px Arial,sans-serif;box-shadow:0 4px 16px #0003;cursor:pointer}#autoservis-navigation button:hover{background:#263c5b}#autoservis-navigation button:focus-visible{outline:3px solid #60a5fa;outline-offset:3px}@media(max-width:520px){#autoservis-navigation{right:12px;bottom:12px;gap:6px}#autoservis-navigation button{padding:11px 14px}}`;
    document.head.appendChild(style);

    const navigation = document.createElement('div');
    navigation.id = 'autoservis-navigation';

    const backButton = document.createElement('button');
    backButton.id = 'autoservis-back';
    backButton.type = 'button';
    backButton.setAttribute('aria-label', 'Önceki sayfaya dön');
    backButton.textContent = '← Geri';
    backButton.addEventListener('click', () => {
        let sameSiteReferrer = false;
        try {
            sameSiteReferrer = Boolean(document.referrer)
                && new URL(document.referrer).origin === window.location.origin;
        } catch (_) {
            sameSiteReferrer = false;
        }

        if (sameSiteReferrer && window.history.length > 1) {
            window.history.back();
            return;
        }

        const path = window.location.pathname;
        let fallback = '/giris';
        if (path.startsWith('/musteri/takip/')) fallback = '/musteri';
        else if (path.startsWith('/arac-duzenle/') || path.startsWith('/takip/')) fallback = '/araclar';
        else if (path.startsWith('/servis-duzenle/')) fallback = '/servisler';
        else if (path === '/arac-ekle' || path === '/arac-guncelle' || path === '/arac-kaydet') fallback = '/araclar';
        else if (path === '/servis-ekle' || path === '/servis-guncelle' || path === '/servis-kaydet') fallback = '/servisler';
        else if (path === '/araclar') fallback = '/';
        else if (path === '/servisler') fallback = '/araclar';
        else if (path === '/sifre-unuttum' || path === '/sifre-kodu-gonder' || path === '/sifre-yenile') fallback = '/giris';
        else if (path === '/musteri') fallback = '/giris';
        window.location.assign(fallback);
    });

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
    navigation.append(backButton, button);
    document.body.appendChild(navigation);
})();
