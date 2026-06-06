import { useState, useEffect } from 'react'
import { listarRescatistas, crearPreferencia, confirmarPorDonacion } from '../api/donacion'
import { getProvincias } from '../api/georef'

function Donar() {
  const [rescatistas, setRescatistas] = useState([])
  const [provincias, setProvincias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [filtroQ, setFiltroQ] = useState('')
  const [filtroProvincia, setFiltroProvincia] = useState('')

  // modal de donacion
  const [seleccionado, setSeleccionado] = useState(null)
  const [monto, setMonto] = useState('')
  const [creando, setCreando] = useState(false)
  const [errorDonacion, setErrorDonacion] = useState('')
  const [donacionPendienteId, setDonacionPendienteId] = useState(null)
  const [confirmando, setConfirmando] = useState(false)
  const [exitoDonacion, setExitoDonacion] = useState('')

  useEffect(() => {
    getProvincias().then(setProvincias).catch(() => [])
    cargar()
  }, [])

  async function cargar(q, provincia) {
    setCargando(true)
    setError('')
    try {
      const res = await listarRescatistas({ q, provincia })
      setRescatistas(res.data)
    } catch {
      setError('No se pudo cargar el listado.')
    } finally {
      setCargando(false)
    }
  }

  function handleBuscar(e) {
    e.preventDefault()
    cargar(filtroQ, filtroProvincia)
  }

  function abrirModal(rescatista) {
    setSeleccionado(rescatista)
    setMonto('')
    setErrorDonacion('')
  }

  function cerrarModal() {
    setSeleccionado(null)
    setErrorDonacion('')
    setDonacionPendienteId(null)
    setExitoDonacion('')
  }

  async function handleDonar(e) {
    e.preventDefault()
    setErrorDonacion('')
    const montoNum = Number(monto)
    if (!monto || isNaN(montoNum) || montoNum < 100) {
      setErrorDonacion('El monto mínimo es $100.')
      return
    }
    setCreando(true)
    try {
      const res = await crearPreferencia({ rescatistaId: seleccionado.id, monto: montoNum })
      // redirigir al checkout de MercadoPago
      window.open(res.data.checkoutUrl, '_blank')
      setDonacionPendienteId(res.data.id)
    } catch (err) {
      setErrorDonacion(err.response?.data || 'No se pudo procesar la donación.')
    } finally {
      setCreando(false)
    }
  }

  async function handleYaPague() {
    if (!donacionPendienteId) return
    setConfirmando(true)
    setErrorDonacion(null)
    try {
      const res = await confirmarPorDonacion(donacionPendienteId)
      const estado = res.data?.estado
      if (estado === 'COMPLETADA') {
        setExitoDonacion('¡Gracias por tu donación!')
        setDonacionPendienteId(null)
      } else if (estado === 'FALLIDA') {
        setErrorDonacion('El pago fue rechazado o cancelado.')
      } else {
        setErrorDonacion('Todavía no encontramos tu pago. Si ya pagaste, esperá unos segundos y volvé a intentar.')
      }
    } catch {
      setErrorDonacion('No pudimos verificar el pago. Intentá de nuevo.')
    } finally {
      setConfirmando(false)
    }
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '0 16px' }}>
      <h2>Apoyá a los rescatistas</h2>
      <p>Cada donación apoya directamente a los rescatistas que salvan animales.</p>

      <form onSubmit={handleBuscar} style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 }}>
        <input
          type="text"
          placeholder="Buscar por nombre u organización..."
          value={filtroQ}
          onChange={e => setFiltroQ(e.target.value)}
          style={{ flex: 1, minWidth: 200 }}
        />
        <select value={filtroProvincia} onChange={e => setFiltroProvincia(e.target.value)}>
          <option value="">Todas las provincias</option>
          {provincias.map(p => (
            <option key={p.id} value={p.nombre}>{p.nombre}</option>
          ))}
        </select>
        <button type="submit">Buscar</button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}
      {cargando && <p>Cargando...</p>}

      {!cargando && rescatistas.length === 0 && (
        <p>No hay rescatistas aceptando donaciones en este momento.</p>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
        {rescatistas.map(r => (
          <div key={r.id} style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: 4 }}>
            <h3 style={{ margin: '0 0 4px' }}>{r.organizacion || `${r.nombre} ${r.apellido}`}</h3>
            {r.organizacion && <p style={{ margin: '0 0 4px', fontSize: 13, color: '#555' }}>{r.nombre} {r.apellido}</p>}
            {(r.ciudad || r.provincia) && (
              <p style={{ margin: '0 0 8px', fontSize: 13 }}>
                {[r.ciudad, r.provincia].filter(Boolean).join(', ')}
              </p>
            )}
            {r.descripcionDonacion && (
              <p style={{ margin: '0 0 12px', fontSize: 14 }}>{r.descripcionDonacion}</p>
            )}
            <button onClick={() => abrirModal(r)}>Donar</button>
          </div>
        ))}
      </div>

      {/* Modal */}
      {seleccionado && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div style={{ background: '#fff', padding: '2rem', borderRadius: 4, width: 340, maxWidth: '90%' }}>
            <h3 style={{ marginTop: 0 }}>
              Donar a {seleccionado.organizacion || `${seleccionado.nombre} ${seleccionado.apellido}`}
            </h3>

            {exitoDonacion ? (
              <div>
                <p>{exitoDonacion}</p>
                <button onClick={cerrarModal}>Cerrar</button>
              </div>
            ) : donacionPendienteId ? (
              <div>
                <p>Completá el pago en la ventana de MercadoPago que se abrió. Cuando termines, hacé click en "Ya pagué".</p>
                {errorDonacion && <p style={{ color: 'red' }}>{errorDonacion}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button onClick={handleYaPague} disabled={confirmando}>
                    {confirmando ? 'Verificando...' : 'Ya pagué'}
                  </button>
                  <button onClick={cerrarModal}>Cancelar</button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleDonar}>
                <label>Monto (mínimo $100)</label><br />
                <input
                  type="number"
                  min={100}
                  step={1}
                  value={monto}
                  onChange={e => setMonto(e.target.value)}
                  placeholder="Ej: 500"
                  style={{ width: '100%', marginTop: 4, marginBottom: 12, boxSizing: 'border-box' }}
                  required
                />
                {errorDonacion && <p style={{ color: 'red', marginBottom: 8 }}>{errorDonacion}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" disabled={creando}>
                    {creando ? 'Procesando...' : 'Continuar con MercadoPago'}
                  </button>
                  <button type="button" onClick={cerrarModal} disabled={creando}>Cancelar</button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default Donar
