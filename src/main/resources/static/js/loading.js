(function() {
    'use strict';

    var overlay = document.getElementById('loadingOverlay');
    var pendingRequests = 0;

    function showOverlay(title, subtitle) {
        if (!overlay) return;
        var titleEl = overlay.querySelector('.loading-title');
        var subtitleEl = overlay.querySelector('.loading-subtitle');
        if (titleEl) titleEl.textContent = title || 'Loading...';
        if (subtitleEl) subtitleEl.textContent = subtitle || 'Please wait...';
        overlay.classList.add('active');
    }

    function hideOverlay() {
        if (!overlay) return;
        if (pendingRequests > 0) return;
        overlay.classList.remove('active');
    }

    function initNavLoading() {
        document.addEventListener('click', function(e) {
            var link = e.target.closest('a');
            if (!link) return;
            if (link.getAttribute('href') === '#' || link.getAttribute('href') === '' || link.getAttribute('target') === '_blank') return;
            if (link.hasAttribute('data-no-loading')) return;
            if (link.hostname !== window.location.hostname && link.href.startsWith('http')) return;
            if (e.ctrlKey || e.metaKey || e.shiftKey) return;
            if (link.getAttribute('href') && link.getAttribute('href').startsWith('javascript:')) return;
            showOverlay('Loading...', 'Navigating to page');
        });
    }

    function initFormLoading() {
        document.addEventListener('submit', function(e) {
            var form = e.target;
            if (form.hasAttribute('data-no-loading')) return;
            var submitBtn = form.querySelector('[type="submit"]');
            if (submitBtn && !submitBtn.disabled) {
                submitBtn.disabled = true;
                var originalText = submitBtn.innerHTML;
                submitBtn.dataset.originalText = originalText;
                submitBtn.innerHTML = '<span class="spinner"></span> Processing...';
                submitBtn.classList.add('loading');
            }
            showOverlay('Processing...', 'Please wait while we process your request');
        });
    }

    function initPageLoad() {
        window.addEventListener('pageshow', function() {
            hideOverlay();
            var buttons = document.querySelectorAll('[type="submit"][disabled]');
            buttons.forEach(function(btn) {
                btn.disabled = false;
                if (btn.dataset.originalText) {
                    btn.innerHTML = btn.dataset.originalText;
                }
                btn.classList.remove('loading');
            });
        });
        if (document.readyState === 'complete') {
            hideOverlay();
        } else {
            window.addEventListener('load', hideOverlay);
        }
    }

    function fetchPage(url, targetSelector, title) {
        showOverlay(title || 'Loading...');
        pendingRequests++;
        return fetch(url, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        }).then(function(response) {
            if (!response.ok) throw new Error('Page load failed');
            return response.text();
        }).then(function(html) {
            var parser = new DOMParser();
            var doc = parser.parseFromString(html, 'text/html');
            var target = document.querySelector(targetSelector);
            var newContent = doc.querySelector(targetSelector);
            if (target && newContent) {
                target.innerHTML = newContent.innerHTML;
            }
            var titleTag = doc.querySelector('title');
            if (titleTag) document.title = titleTag.textContent;
            history.pushState({url: url, target: targetSelector}, '', url);
            window.scrollTo({top: 0, behavior: 'smooth'});
            hideOverlay();
        }).catch(function(err) {
            console.error('Async navigation failed:', err);
            window.location.href = url;
        }).finally(function() {
            pendingRequests--;
            if (pendingRequests <= 0) hideOverlay();
        });
    }

    function initAsyncNav(linkSelector, targetSelector) {
        document.addEventListener('click', function(e) {
            var link = e.target.closest(linkSelector || 'a.async-nav');
            if (!link) return;
            var href = link.getAttribute('href');
            if (!href || href === '#' || href.startsWith('javascript:')) return;
            if (e.ctrlKey || e.metaKey || e.shiftKey) return;
            e.preventDefault();
            fetchPage(href, targetSelector || '#main-content');
        });
    }

    window.addEventListener('popstate', function(e) {
        if (e.state && e.state.url) {
            fetchPage(e.state.url, e.state.targetSelector || '#main-content');
        }
    });

    document.addEventListener('DOMContentLoaded', function() {
        initNavLoading();
        initFormLoading();
        initPageLoad();
    });

    window.showLoadingOverlay = showOverlay;
    window.hideLoadingOverlay = hideOverlay;
    window.fetchPage = fetchPage;
    window.initAsyncNav = initAsyncNav;
})();
