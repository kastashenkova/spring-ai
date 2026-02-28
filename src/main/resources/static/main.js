const API = 'http://localhost:8088/api';
const PAGE_SIZE = 10;

let docs = [];
let activeDocId = null;
let currentPage = 0;
let lastQuery = '';

const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const fileNameEl = document.getElementById('fileName');
const uploadBtn = document.getElementById('uploadBtn');
const docsList = document.getElementById('docsList');
const docCountBadge = document.getElementById('docCountBadge');
const scopePill = document.getElementById('scopePill');
const searchInput  = document.getElementById('searchInput');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const pageNum = document.getElementById('pageNum');
const searchBtn = document.getElementById('searchBtn');
const results = document.getElementById('results');

// docs
async function loadDocs() {
    try {
        const res = await fetch(`${API}/docs`);
        if (!res.ok) {
            throw new Error(await res.text());
        }
        const data = await res.json();
        docs = (data.content ?? data).map(d => ({ id: d.id, name: d.fileName ?? d.title ?? String(d.id) }));
        renderDocs();
    } catch (e) {
        toast('Failed to load documents: ' + e.message, 'err');
    }
}

function renderDocs() {
    docCountBadge.textContent = docs.length ? `(${docs.length})` : '';
    if (!docs.length) {
        docsList.innerHTML = '<div class="no-docs">No documents uploaded</div>';
        return;
    }
    docsList.innerHTML = docs.map((d, i) => `
      <div class="doc-item ${activeDocId === d.id ? 'active' : ''}" data-idx="${i}">
        <div class="doc-dot"></div>
        <div class="doc-name" title="${esc(d.name)}">${esc(d.name)}</div>
        <button class="doc-del" data-idx="${i}" title="Delete">✕</button>
      </div>
    `).join('');

    docsList.querySelectorAll('.doc-item').forEach(el => {
        el.addEventListener('click', e => {
            if (e.target.classList.contains('doc-del')) {
                return;
            }
            const d = docs[Number(el.dataset.idx)];
            activeDocId = (activeDocId === d.id) ? null : d.id;
            updateScope();
            renderDocs();
            if (lastQuery) {
                doSearch(0);
            }
        });
    });

    docsList.querySelectorAll('.doc-del').forEach(btn => {
        btn.addEventListener('click', async e => {
            e.stopPropagation();
            const idx = Number(btn.dataset.idx);
            const d = docs[idx];
            if (!confirm(`Delete "${d.name}"?`)) {
                return;
            }
            try {
                const res = await fetch(`${API}/docs/${d.id}`, { method: 'DELETE' });
                if (!res.ok) {
                    throw new Error(await res.text());
                }
            } catch (err) {
                return toast('Delete failed: ' + err.message, 'err');
            }
            if (activeDocId === d.id) {
                activeDocId = null;
                updateScope();
            }
            toast('Document removed');
            await loadDocs();
        });
    });
}

function updateScope() {
    if (activeDocId !== null) {
        const d = docs.find(x => x.id === activeDocId);
        scopePill.textContent = `Doc: ${d ? d.name : '#' + activeDocId}`;
        scopePill.classList.add('filtered');
    } else {
        scopePill.textContent = 'All docs';
        scopePill.classList.remove('filtered');
    }
}

// upload
fileInput.addEventListener('change', () => {
    const files = Array.from(fileInput.files);
    fileNameEl.textContent = files.length
        ? files.map(f => f.name).join(', ')
        : '';
    uploadBtn.disabled = !files.length;
});

dropZone.addEventListener('dragover', e => {
    e.preventDefault(); dropZone.classList.add('drag');
});

dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag'));

dropZone.addEventListener('drop', e => {
    e.preventDefault(); dropZone.classList.remove('drag');
    const files = Array.from(e.dataTransfer.files)
        .filter(f => f.name.toLowerCase().endsWith('.fb2'));
    if (!files.length) {
        return toast('Only .fb2 files supported', 'err');
    }
    const dt = new DataTransfer();
    files.forEach(f => dt.items.add(f));
    fileInput.files = dt.files;
    fileNameEl.textContent = files.map(f => f.name).join(', ');
    uploadBtn.disabled = false;
});

uploadBtn.addEventListener('click', async () => {
    const files = Array.from(fileInput.files);
    if (!files.length) {
        return;
    }
    uploadBtn.disabled = true;
    uploadBtn.innerHTML = '<span class="spin"></span> Uploading…';

    let success = 0;
    let failed = 0;

    for (const file of files) {
        const form = new FormData();
        form.append('file', file);
        try {
            const res = await fetch(`${API}/docs`, { method: 'POST', body: form });
            if (!res.ok) {
                throw new Error(await res.text());
            }
            success++;
        } catch (e) {
            failed++;
            toast(`Failed: ${file.name} — ${e.message}`, 'err');
        }
    }

    fileInput.value = '';
    fileNameEl.textContent = '';
    if (success > 0) {
        toast(`Uploaded ${success} file${success !== 1 ? 's' : ''}${failed > 0 ? `, ${failed} failed` : ''}`);
    }
    await loadDocs();
    uploadBtn.innerHTML = 'Upload';
    uploadBtn.disabled = true;
});

// search
searchBtn.addEventListener('click', () => doSearch(0));
searchInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
        doSearch(0);
    }
});
prevBtn.addEventListener('click', () => doSearch(currentPage - 1));
nextBtn.addEventListener('click', () => doSearch(currentPage + 1));

async function doSearch(page) {
    const query = searchInput.value.trim();
    if (!query) {
        return toast('Enter a search query', 'err');
    }
    lastQuery = query;
    currentPage = page;

    searchBtn.disabled = true;
    searchBtn.innerHTML = '<span class="spin"></span>';
    results.innerHTML = '';

    try {
        const body = { query, page, size: PAGE_SIZE };
        if (activeDocId !== null) {
            body.docId = activeDocId;
        }
        const res = await fetch(`${API}/docs/search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            throw new Error(await res.text());
        }
        const data = await res.json();
        renderResults(data.content ?? data, page, data.last ?? true);
    } catch (e) {
        results.innerHTML = `<div class="empty"><div class="empty-icon">⚠️</div><p>${esc(e.message)}</p></div>`;
    }
    searchBtn.innerHTML = 'Search';
    searchBtn.disabled = false;
}

function renderResults(items, page, isLast) {
    pageNum.textContent = page + 1;
    prevBtn.disabled = page === 0;
    nextBtn.disabled = isLast;

    if (!items.length) {
        results.innerHTML = `<div class="empty"><p>Nothing found</p></div>`;
        return;
    }

    const meta = document.createElement('div');
    meta.className = 'results-meta';
    meta.innerHTML = `<span>${items.length} fragment${items.length !== 1 ? 's' : ''}</span><span class="query-echo">${esc(lastQuery)}</span>`;
    results.appendChild(meta);

    items.forEach((item, i) => {
        const card = document.createElement('div');
        card.className = 'card';
        card.style.animationDelay = `${i * 0.04}s`;

        const author = item.author ? ` · ${esc(item.author)}` : '';

        card.innerHTML = `
            <div class="card-num">Fragment ${page * PAGE_SIZE + i + 1}</div>
            <div class="card-source">${esc(item.title ?? '')}${author}</div>
            <div class="card-text">${esc(item.fragment)}</div>
        `;
        results.appendChild(card);
    });
}

function esc(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

let toastTimer;
function toast(msg, type = 'ok') {
    document.querySelectorAll('.toast').forEach(t => t.remove());
    clearTimeout(toastTimer);
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = msg;
    document.body.appendChild(el);
    toastTimer = setTimeout(() => el.remove(), 3000);
}

loadDocs();
