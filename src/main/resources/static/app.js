(() => {
  'use strict';

  const FONT = 'system-ui, -apple-system, "Segoe UI", "Apple SD Gothic Neo", "Malgun Gothic", sans-serif';
  const fmt = new Intl.NumberFormat('ko-KR');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const state = { district: '', category: '' };
  const charts = {};
  let requestSeq = 0; // 필터를 빠르게 바꿨을 때 늦게 도착한 옛 응답을 버리기 위한 번호
  let latest = null;  // 마지막으로 그린 데이터 (다크/라이트 전환 시 다시 그리기용)

  const $ = (id) => document.getElementById(id);
  const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

  /** 현재 테마의 색 토큰. 차트는 캔버스라 CSS 변수를 직접 못 쓰므로 그릴 때마다 읽는다. */
  function tokens() {
    return {
      surface: css('--surface-1'), ink: css('--text-primary'), secondary: css('--text-secondary'),
      muted: css('--text-muted'), grid: css('--grid'), baseline: css('--baseline'),
      s1: css('--series-1'), s2: css('--series-2'),
    };
  }

  /** 호버한 막대를 팔레트 색에서 살짝 밝힌다. (Chart.js 기본 보정은 팔레트에 없는 쨍한 색이 된다) */
  function lighten(color, amount = 0.2) {
    const m = /^#([0-9a-f]{6})$/i.exec(color);
    if (!m) return color;
    const n = parseInt(m[1], 16);
    const ch = (shift) => Math.round(((n >> shift) & 255) * (1 - amount) + 255 * amount);
    return `rgb(${ch(16)}, ${ch(8)}, ${ch(0)})`;
  }

  const signed = (n) => (n > 0 ? '+' : n < 0 ? '−' : '') + fmt.format(Math.abs(n));

  async function api(path, params = {}) {
    const res = await fetch(`/api/${path}?${new URLSearchParams(params)}`);
    if (!res.ok) throw new Error(`${path}: HTTP ${res.status}`);
    return res.json();
  }

  // ───────────── 막대 끝(또는 꼭대기)에 값을 적는 작은 플러그인 ─────────────
  const valueLabels = {
    id: 'valueLabels',
    afterDatasetsDraw(chart, _args, opts) {
      if (!opts || !opts.enabled) return;
      const { ctx } = chart;
      const horizontal = chart.options.indexAxis === 'y';
      ctx.save();
      ctx.font = `600 12px ${FONT}`;
      ctx.fillStyle = opts.color; // 글자는 시리즈 색이 아니라 텍스트 토큰
      chart.data.datasets.forEach((ds, di) => {
        const meta = chart.getDatasetMeta(di);
        let indices = ds.data.map((_, i) => i);
        if (opts.mode === 'max') { // 전체에 숫자를 붙이면 읽을 수 없으니 최댓값만
          const max = Math.max(...ds.data);
          indices = max > 0 ? [ds.data.indexOf(max)] : [];
        }
        indices.forEach((i) => {
          const bar = meta.data[i];
          const text = opts.format(ds.data[i]);
          if (horizontal) {
            ctx.textAlign = 'left'; ctx.textBaseline = 'middle';
            ctx.fillText(text, bar.x + 6, bar.y);
          } else {
            ctx.textAlign = 'center'; ctx.textBaseline = 'bottom';
            ctx.fillText(text, bar.x, bar.y - 4);
          }
        });
      });
      ctx.restore();
    },
  };

  function tooltipBase(t) {
    return {
      backgroundColor: t.surface, titleColor: t.ink, bodyColor: t.ink, footerColor: t.secondary,
      borderColor: t.baseline, borderWidth: 1, padding: 10, cornerRadius: 8,
      boxWidth: 12, boxHeight: 3, boxPadding: 6, // 시리즈 키는 상자가 아니라 짧은 선
      titleFont: { weight: '600' }, bodyFont: { weight: '600' }, footerFont: { weight: '400' },
    };
  }

  function destroy(key) {
    if (charts[key]) { charts[key].destroy(); delete charts[key]; }
  }

  /** 데이터가 없으면 캔버스 대신 안내 문구를 보여준다. */
  function setEmpty(plotId, canvasId, empty, message = '표시할 데이터가 없어요.') {
    const plot = $(plotId);
    const canvas = $(canvasId);
    plot.querySelectorAll('.empty-msg').forEach((el) => el.remove());
    canvas.hidden = empty;
    if (empty) {
      const p = document.createElement('p');
      p.className = 'note empty-msg';
      p.textContent = message;
      plot.appendChild(p);
      plot.style.height = 'auto';
    }
  }

  // ───────────── 표(차트의 접근 가능한 쌍둥이) ─────────────
  function renderTable(wrapId, headers, rows) {
    const wrap = $(wrapId);
    wrap.replaceChildren();
    const table = document.createElement('table');
    const thead = table.createTHead().insertRow();
    headers.forEach((h) => {
      const th = document.createElement('th');
      th.scope = 'col';
      th.textContent = h;
      thead.appendChild(th);
    });
    const tbody = table.createTBody();
    rows.forEach((row) => {
      const tr = tbody.insertRow();
      row.forEach((cell, i) => {
        const td = tr.insertCell();
        td.textContent = cell;
        if (i > 0) td.className = 'num';
      });
    });
    wrap.appendChild(table);
  }

  const scopeText = (...parts) => parts.filter(Boolean).join(' · ');
  const districtLabel = () => state.district || '김포시 전체';
  const categoryLabel = () => state.category || '전체 업종';

  // ───────────── 1. 연도별 개업·폐업 ─────────────
  function renderTrend(trend) {
    const points = trend.points;
    $('scope-trend').textContent = scopeText(districtLabel(), categoryLabel(), `최근 ${points.length}년`);
    renderTable('table-trend', ['연도', '개업', '폐업', '순증감'],
      points.map((p) => [`${p.year}년`, fmt.format(p.opened), fmt.format(p.closed), signed(p.net)]));

    const asOf = trend.asOf ? new Date(trend.asOf) : null;
    const partialYear = asOf && !(asOf.getMonth() === 11 && asOf.getDate() === 31) ? asOf.getFullYear() : null;
    $('note-trend').textContent = partialYear
      ? `* ${partialYear}년은 기준일(${trend.asOf})까지의 집계예요. 값은 막대 위 최댓값과 마우스를 올렸을 때 확인할 수 있어요.`
      : '막대 위 숫자는 각 계열의 최댓값이고, 나머지는 마우스를 올리거나 ‘표로 보기’에서 확인할 수 있어요.';

    destroy('trend');
    const empty = points.length === 0;
    setEmpty('plot-trend', 'chart-trend', empty);
    if (empty) return;
    $('plot-trend').style.height = '300px';

    const t = tokens();
    const bar = (label, key, color) => ({
      label,
      data: points.map((p) => p[key]),
      backgroundColor: color,
      hoverBackgroundColor: lighten(color),
      borderColor: t.surface,
      hoverBorderColor: t.surface,
      borderWidth: { left: 1, right: 1, top: 0, bottom: 0 }, // 이웃한 막대 사이 2px 간격
      borderRadius: { topLeft: 4, topRight: 4 },
      borderSkipped: 'start',
      maxBarThickness: 24,
    });

    charts.trend = new Chart($('chart-trend'), {
      type: 'bar',
      data: {
        labels: points.map((p) => (p.year === partialYear ? `${p.year}*` : String(p.year))),
        datasets: [bar('개업', 'opened', t.s1), bar('폐업', 'closed', t.s2)],
      },
      plugins: [valueLabels],
      options: {
        responsive: true, maintainAspectRatio: false,
        layout: { padding: { top: 22 } },
        interaction: { mode: 'index', intersect: false }, // 한 툴팁에 두 계열 모두
        scales: {
          x: { grid: { display: false }, border: { color: t.baseline }, ticks: { color: t.muted } },
          y: {
            beginAtZero: true, grid: { color: t.grid }, border: { display: false },
            ticks: { color: t.muted, callback: (v) => fmt.format(v) },
          },
        },
        plugins: {
          legend: { display: false }, // 범례는 카드 머리의 HTML 범례
          valueLabels: { enabled: true, mode: 'max', color: t.ink, format: (v) => fmt.format(v) },
          tooltip: {
            ...tooltipBase(t),
            callbacks: {
              title: (items) => `${points[items[0].dataIndex].year}년`,
              label: (ctx) => `${fmt.format(ctx.parsed.y)}곳  ${ctx.dataset.label}`, // 값이 먼저
              footer: (items) => `순증감 ${signed(points[items[0].dataIndex].net)}`,
            },
          },
        },
      },
    });
  }

  // ───────────── 가로 막대 (영업 기간, TOP 업종 공용) ─────────────
  function renderHbar({ key, canvasId, plotId, labels, values, color, tickSuffix, format, tooltipLines, emptyMessage }) {
    destroy(key);
    const empty = labels.length === 0;
    setEmpty(plotId, canvasId, empty, emptyMessage);
    if (empty) return;
    // 막대 줄 수 + x축 눈금 띠까지 포함해 높이를 잡는다 (축 라벨이 잘리지 않게)
    $(plotId).style.height = `${labels.length * 30 + 40}px`;

    const t = tokens();
    charts[key] = new Chart($(canvasId), {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          data: values, backgroundColor: color, hoverBackgroundColor: lighten(color),
          borderRadius: { topRight: 4, bottomRight: 4 }, borderSkipped: 'start',
          maxBarThickness: 18,
        }],
      },
      plugins: [valueLabels],
      options: {
        indexAxis: 'y', responsive: true, maintainAspectRatio: false,
        layout: { padding: { right: 64 } }, // 막대 끝 숫자가 잘리지 않을 여백
        scales: {
          x: {
            beginAtZero: true, grid: { color: t.grid }, border: { display: false },
            ticks: { color: t.muted, callback: (v) => `${fmt.format(v)}${tickSuffix}` },
          },
          y: { grid: { display: false }, border: { color: t.baseline }, ticks: { color: t.secondary } },
        },
        plugins: {
          legend: { display: false },
          valueLabels: { enabled: true, mode: 'all', color: t.ink, format },
          tooltip: {
            ...tooltipBase(t), displayColors: false,
            callbacks: { title: (items) => items[0].label, label: (ctx) => tooltipLines[ctx.dataIndex] },
          },
        },
      },
    });
  }

  // ───────────── 2. 업종별 영업 기간 ─────────────
  function renderSurvival(rows) {
    $('scope-survival').textContent = scopeText(districtLabel(), '업종별 비교');
    const sorted = [...rows].sort((a, b) => b.medianYears - a.medianYears);
    renderTable('table-survival', ['업종', '폐업 표본', '중앙값(년)', '평균(년)'],
      sorted.map((r) => [r.category, `${fmt.format(r.closedCount)}곳`, r.medianYears.toFixed(1), r.avgYears.toFixed(1)]));
    renderHbar({
      key: 'survival', canvasId: 'chart-survival', plotId: 'plot-survival',
      labels: sorted.map((r) => r.category), values: sorted.map((r) => r.medianYears),
      color: tokens().s1, tickSuffix: '년', format: (v) => `${v.toFixed(1)}년`,
      tooltipLines: sorted.map((r) => [
        `중앙값 ${r.medianYears.toFixed(1)}년`, `평균 ${r.avgYears.toFixed(1)}년`, `폐업 ${fmt.format(r.closedCount)}곳 기준`,
      ]),
      emptyMessage: '폐업 표본이 10곳 이상인 업종이 없어요.',
    });
  }

  // ───────────── 3. TOP 업종 + KPI ─────────────
  function renderRanking(r) {
    $('scope-ranking').textContent = scopeText(districtLabel(), '업종별 비교',
      r.asOf ? `${r.from} 초과 ~ ${r.asOf}` : '');
    $('kpi-opened').textContent = fmt.format(r.openedTotal);
    $('kpi-closed').textContent = fmt.format(r.closedTotal);
    const net = r.openedTotal - r.closedTotal;
    $('kpi-net').textContent = signed(net);
    document.querySelectorAll('.kpi-value').forEach((el) => {
      const unit = document.createElement('small');
      unit.textContent = '곳';
      el.appendChild(unit);
    });

    const rows = Array.from({ length: Math.max(r.opened.length, r.closed.length) }, (_, i) => [
      `${i + 1}`,
      r.opened[i] ? r.opened[i].category : '', r.opened[i] ? `${fmt.format(r.opened[i].count)}곳` : '',
      r.closed[i] ? r.closed[i].category : '', r.closed[i] ? `${fmt.format(r.closed[i].count)}곳` : '',
    ]);
    renderTable('table-ranking', ['순위', '개업 업종', '개업 수', '폐업 업종', '폐업 수'], rows);

    const t = tokens();
    const base = { tickSuffix: '', format: (v) => `${fmt.format(v)}곳` };
    renderHbar({
      ...base, key: 'opened', canvasId: 'chart-opened', plotId: 'plot-opened',
      labels: r.opened.map((c) => c.category), values: r.opened.map((c) => c.count), color: t.s1,
      tooltipLines: r.opened.map((c) => `개업 ${fmt.format(c.count)}곳`),
      emptyMessage: '이 기간에 개업한 곳이 없어요.',
    });
    renderHbar({
      ...base, key: 'closed', canvasId: 'chart-closed', plotId: 'plot-closed',
      labels: r.closed.map((c) => c.category), values: r.closed.map((c) => c.count), color: t.s2,
      tooltipLines: r.closed.map((c) => `폐업 ${fmt.format(c.count)}곳`),
      emptyMessage: '이 기간에 폐업한 곳이 없어요.',
    });
  }

  // ───────────── 4. 읍·면·동 비교 표 ─────────────
  function renderDistricts(d) {
    $('scope-districts').textContent = scopeText('김포시 읍·면·동 비교', categoryLabel(),
      d.asOf ? `${d.from} 초과 ~ ${d.asOf}` : '');
    renderTable('table-districts', ['읍·면·동', '개업', '폐업', '순증감'],
      d.rows.map((r) => [r.district, fmt.format(r.opened), fmt.format(r.closed), signed(r.net)]));
  }

  function renderAll(data) {
    renderTrend(data.trend);
    renderSurvival(data.survival);
    renderRanking(data.ranking);
    renderDistricts(data.districts);
  }

  // ───────────── 데이터 로딩 ─────────────
  async function load() {
    const seq = ++requestSeq;
    const app = $('app');
    app.classList.add('loading');
    try {
      const [trend, survival, ranking, districts] = await Promise.all([
        api('trend', { district: state.district, category: state.category }),
        api('survival', { district: state.district }),
        api('ranking', { district: state.district, months: 12 }),
        api('districts', { category: state.category, months: 12 }),
      ]);
      if (seq !== requestSeq) return; // 그 사이 필터가 바뀜
      latest = { trend, survival, ranking, districts };
      renderAll(latest);
    } catch (e) {
      $('asof').textContent = `데이터를 불러오지 못했어요 (${e.message})`;
    } finally {
      if (seq === requestSeq) app.classList.remove('loading');
    }
  }

  function fillSelect(select, items, nameKey) {
    items.forEach((item) => {
      const opt = document.createElement('option');
      opt.value = item[nameKey];
      opt.textContent = `${item[nameKey]} (${fmt.format(item.count)})`;
      select.appendChild(opt);
    });
  }

  function bindToggles() {
    document.querySelectorAll('.toggle').forEach((btn) => {
      btn.addEventListener('click', () => {
        const target = btn.dataset.target;
        const showTable = btn.getAttribute('aria-pressed') !== 'true';
        btn.setAttribute('aria-pressed', String(showTable));
        btn.textContent = showTable ? '차트로 보기' : '표로 보기';
        $(`plot-${target}`).hidden = showTable;
        $(`table-${target}`).hidden = !showTable;
      });
    });
  }

  async function init() {
    if (reducedMotion) Chart.defaults.animation = false;
    Chart.defaults.font.family = FONT;
    Chart.defaults.font.size = 12;

    const meta = await api('meta');
    if (meta.total === 0) {
      $('empty').hidden = false;
      $('content').hidden = true;
      return;
    }
    $('asof').textContent = `기준일 ${meta.asOf} · 적재 ${fmt.format(meta.total)}곳`;
    fillSelect($('f-district'), meta.districts, 'district');
    fillSelect($('f-category'), meta.categories, 'category');

    $('f-district').addEventListener('change', (e) => { state.district = e.target.value; load(); });
    $('f-category').addEventListener('change', (e) => { state.category = e.target.value; load(); });
    bindToggles();
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => latest && renderAll(latest));

    await load();
  }

  init().catch((e) => { $('asof').textContent = `초기화 실패: ${e.message}`; });
})();
