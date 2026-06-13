function Paginacion({ total, porPagina, pagina, onChange }) {
  const totalPaginas = Math.ceil(total / porPagina)
  if (totalPaginas <= 1) return null

  function paginas() {
    const rango = []
    const delta = 2
    const left = Math.max(2, pagina - delta)
    const right = Math.min(totalPaginas - 1, pagina + delta)

    rango.push(1)
    if (left > 2) rango.push('...')
    for (let i = left; i <= right; i++) rango.push(i)
    if (right < totalPaginas - 1) rango.push('...')
    if (totalPaginas > 1) rango.push(totalPaginas)

    return rango
  }

  const btnBase = {
    minWidth: 32,
    height: 32,
    border: '1px solid #ccc',
    background: 'white',
    cursor: 'pointer',
    borderRadius: 4,
    fontSize: 13,
    padding: '0 6px',
  }

  return (
    <div style={{ display: 'flex', gap: 4, alignItems: 'center', margin: '16px 0', flexWrap: 'wrap' }}>
      <button
        style={btnBase}
        disabled={pagina === 1}
        onClick={() => onChange(pagina - 1)}
      >
        ←
      </button>

      {paginas().map((p, i) =>
        p === '...'
          ? <span key={`e${i}`} style={{ padding: '0 4px' }}>…</span>
          : (
            <button
              key={p}
              style={{
                ...btnBase,
                background: p === pagina ? '#333' : 'white',
                color: p === pagina ? 'white' : 'inherit',
                fontWeight: p === pagina ? 700 : 400,
                borderColor: p === pagina ? '#333' : '#ccc',
              }}
              onClick={() => onChange(p)}
            >
              {p}
            </button>
          )
      )}

      <button
        style={btnBase}
        disabled={pagina === totalPaginas}
        onClick={() => onChange(pagina + 1)}
      >
        →
      </button>
    </div>
  )
}

export default Paginacion
