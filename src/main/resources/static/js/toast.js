// Toast Notification System
function showToast(message, type = 'info', title = '') {
    const toast = document.createElement('div');
    toast.className = 'toast toast-' + type;

    let icon = '';
    if (type === 'success') icon = '<i class="fas fa-check-circle"></i>';
    else if (type === 'error') icon = '<i class="fas fa-exclamation-circle"></i>';
    else if (type === 'warning') icon = '<i class="fas fa-exclamation-triangle"></i>';
    else icon = '<i class="fas fa-info-circle"></i>';

    toast.innerHTML = `
        <div class="toast-icon">${icon}</div>
        <div class="toast-content">
            ${title ? `<div class="toast-title">${title}</div>` : ''}
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function showInfoToast(message, title = 'Info') {
    showToast(message, 'info', title);
}

function showSuccessToast(message, title = 'Success') {
    showToast(message, 'success', title);
}

function showErrorToast(message, title = 'Error') {
    showToast(message, 'error', title);
}

function showWarningToast(message, title = 'Warning') {
    showToast(message, 'warning', title);
}
