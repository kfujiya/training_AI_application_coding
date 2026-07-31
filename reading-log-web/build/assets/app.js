(() => {
    const tabButtons = document.querySelectorAll('.tab-button');
    const panels = document.querySelectorAll('.tab-panel');
    let activeTab = document.body.dataset.activeTab || 'records';

    function selectTab(tab) {
        activeTab = tab;
        tabButtons.forEach(button => button.classList.toggle('active', button.dataset.tab === tab));
        panels.forEach(panel => panel.classList.toggle('active', panel.id === `${tab}-panel`));
        const url = new URL(window.location.href);
        url.searchParams.set('tab', tab);
        history.replaceState(null, '', url);
    }

    tabButtons.forEach(button => button.addEventListener('click', () => selectTab(button.dataset.tab)));
    selectTab(activeTab);

    document.getElementById('open-create').addEventListener('click', () => {
        document.getElementById(activeTab === 'books' ? 'book-dialog' : 'record-dialog').showModal();
    });

    document.querySelectorAll('.close-button').forEach(button => {
        button.addEventListener('click', () => button.closest('dialog').close());
    });

    document.querySelectorAll('dialog').forEach(dialog => {
        dialog.addEventListener('click', event => {
            if (event.target === dialog) dialog.close();
        });
    });

    document.querySelectorAll('.delete-form').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm(`「${form.dataset.label}」を削除しますか？\nこの操作は元に戻せません。`)) event.preventDefault();
        });
    });
})();
