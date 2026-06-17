(function() {
    'use strict';

    var overlay = document.getElementById('loadingOverlay');
    var pendingRequests = 0;
    var progressInterval = null;
    var navTimeout = null;

    function showOverlay(title, subtitle) {
        if (!overlay) return;
        var titleEl = overlay.querySelector('.loading-title');
        var subtitleEl = overlay.querySelector('.loading-subtitle');
        var progressBar = overlay.querySelector('.loading-progress .progress-bar');
        if (titleEl) titleEl.textContent = title || 'Loading...';
        if (subtitleEl) subtitleEl.textContent = subtitle || 'Please wait...';
        overlay.classList.add('active');
        if (progressBar) {
            progressBar.style.width = '0%';
            clearInterval(progressInterval);
            progressInterval = setInterval(function() {
                var current = parseFloat(progressBar.style.width) || 0;
                if (current < 30) progressBar.style.width = (current + 5) + '%';
                else if (current < 60) progressBar.style.width = (current + 2) + '%';
                else if (current < 85) progressBar.style.width = (current + 0.5) + '%';
            }, 300);
        }
    }

    function hideOverlay() {
        if (!overlay) return;
        if (pendingRequests > 0) return;
        var progressBar = overlay.querySelector('.loading-progress .progress-bar');
        if (progressBar) {
            progressBar.style.width = '100%';
            clearInterval(progressInterval);
            setTimeout(function() {
                overlay.classList.remove('active');
                progressBar.style.width = '0%';
            }, 300);
        } else {
            overlay.classList.remove('active');
        }
    }

    function getPageTitle(href) {
        if (!href) return 'Loading...';
        var name = href.replace(/.*\//, '').replace(/[?#].*/, '') || 'Dashboard';
        return 'Loading ' + name.charAt(0).toUpperCase() + name.slice(1) + '...';
    }

    function initNavLoading() {
        document.addEventListener('click', function(e) {
            var link = e.target.closest('a');
            if (!link) return;
            var href = link.getAttribute('href');
            if (!href || href === '#' || href === '') return;
            if (link.hasAttribute('data-no-loading')) return;
            if (link.hasAttribute('target')) return;
            if (link.hostname && link.hostname !== window.location.hostname) return;
            if (e.ctrlKey || e.metaKey || e.shiftKey || e.defaultPrevented) return;
            if (href.startsWith('javascript:') || href.startsWith('mailto:') || href.startsWith('tel:')) return;
            var linkClass = link.className || '';
            if (linkClass.includes('btn-delete') || linkClass.includes('btn-danger') || linkClass.includes('delete')) {
                if (!link.hasAttribute('data-instant')) return;
            }
            if (href.startsWith('/logout') || href.startsWith('/debts/delete') || href.startsWith('/goals/delete') || href.startsWith('/transactions/delete') || href.startsWith('/bills/delete') || href.startsWith('/recurring/delete') || href.startsWith('/investments/delete') || href.startsWith('/accounts/delete') || href.startsWith('/excel/delete')) {
                return;
            }
            showOverlay(getPageTitle(href), 'Please wait...');
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
                var icon = submitBtn.querySelector('i');
                if (icon && icon.parentNode === submitBtn) {
                    submitBtn.innerHTML = '<span class="spinner"></span> Processing...';
                } else {
                    submitBtn.innerHTML = '<span class="spinner"></span> Processing...';
                }
                submitBtn.classList.add('loading');
            }
            var formAction = form.getAttribute('action') || '';
            var formTitle = 'Processing...';
            if (formAction.includes('delete') || formAction.includes('remove')) formTitle = 'Deleting...';
            else if (formAction.includes('save') || formAction.includes('add') || formAction.includes('create') || formAction.includes('edit') || formAction.includes('update')) formTitle = 'Saving...';
            showOverlay(formTitle, 'Please wait while we process your request');
        });
    }

    function resetButtons() {
        var buttons = document.querySelectorAll('[type="submit"][disabled]');
        buttons.forEach(function(btn) {
            btn.disabled = false;
            if (btn.dataset.originalText) {
                btn.innerHTML = btn.dataset.originalText;
            }
            btn.classList.remove('loading');
        });
    }

    function initPageLoad() {
        if (document.readyState === 'complete') {
            hideOverlay();
        } else {
            window.addEventListener('load', hideOverlay);
        }
        window.addEventListener('pageshow', function(e) {
            if (e.persisted) {
                hideOverlay();
                resetButtons();
            }
        });
    }

    function fetchPage(url, targetSelector, title) {
        if (navTimeout) return Promise.reject('Navigation in progress');
        showOverlay(title || getPageTitle(url));
        pendingRequests++;
        navTimeout = setTimeout(function() {
            navTimeout = null;
        }, 5000);
        return fetch(url, {
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'text/html' }
        }).then(function(response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.text();
        }).then(function(html) {
            var parser = new DOMParser();
            var doc = parser.parseFromString(html, 'text/html');
            var target = document.querySelector(targetSelector || '#main-content');
            var newContent = doc.querySelector(targetSelector || '#main-content');
            if (target && newContent) {
                target.innerHTML = newContent.innerHTML;
                target.classList.remove('content-transition');
                void target.offsetWidth;
                target.classList.add('content-transition');
            }
            var titleTag = doc.querySelector('title');
            if (titleTag) document.title = titleTag.textContent;
            history.pushState({url: url, target: targetSelector || '#main-content'}, '', url);
            window.scrollTo({top: 0, behavior: 'smooth'});
            hideOverlay();
        }).catch(function(err) {
            console.error('Async navigation failed, falling back to full load:', err);
            window.location.href = url;
        }).finally(function() {
            pendingRequests--;
            clearTimeout(navTimeout);
            navTimeout = null;
            if (pendingRequests <= 0) hideOverlay();
        });
    }

    function updateActiveNav(url) {
        var navLinks = document.querySelectorAll('.sidebar .nav-link');
        navLinks.forEach(function(link) {
            var href = link.getAttribute('href');
            link.classList.remove('active');
            if (href && url.indexOf(href) === 0) {
                var afterBase = url.substring(href.length);
                if (afterBase === '' || afterBase === '/' || afterBase.startsWith('?')) {
                    link.classList.add('active');
                }
            }
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
            updateActiveNav(href);
            fetchPage(href, targetSelector || '#main-content');
        });
    }

    function initLogoutHandler() {
        var originalLogout = window.confirmLogout;
        if (typeof originalLogout === 'function') {
            window.confirmLogout = function() {
                showDeleteModal('Are you sure you want to logout?', function() {
                    showOverlay('Logging out...', 'Please wait...');
                    window.location.href = '/logout';
                }, 'Yes, Logout');
            };
        }
    }

    window.addEventListener('popstate', function(e) {
        if (e.state && e.state.url) {
            fetchPage(e.state.url, e.state.target || '#main-content');
        }
    });

    document.addEventListener('DOMContentLoaded', function() {
        initNavLoading();
        initFormLoading();
        initPageLoad();
        initLogoutHandler();
    });

    window.showLoadingOverlay = showOverlay;
    window.hideLoadingOverlay = hideOverlay;
    window.fetchPage = fetchPage;
    window.initAsyncNav = initAsyncNav;

})();
