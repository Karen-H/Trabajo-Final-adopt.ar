import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { listarRescatistas, crearPreferencia, confirmarPorDonacion } from '../api/donacion'
import { getProvincias } from '../api/georef'
import { useAuth } from '../context/AuthContext'
import { formatMonto } from '../utils/formatMonto'

function Donar() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [rescatistas, setRescatistas] = useState([])
  const [provincias, setProvincias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [filtroQ, setFiltroQ] = useState('')
  const [filtroProvincia, setFiltroProvincia] = useState('')

  // modal de donacion
  const [seleccionado, setSeleccionado] = useState(null)
  const [monto, setMonto] = useState('')
  const [montoPersonalizado, setMontoPersonalizado] = useState(false)
  const [creando, setCreando] = useState(false)
  const [errorDonacion, setErrorDonacion] = useState('')
  const [donacionPendienteId, setDonacionPendienteId] = useState(null)
  const [exitoDonacion, setExitoDonacion] = useState('')

  useEffect(() => {
    if (user && (user.role !== 'USER' || user.activeProfile !== 'ADOPTANTE')) {
      navigate('/')
      return
    }
    getProvincias().then(setProvincias).catch(() => [])
    cargar()
  }, [user, navigate])

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
    setMontoPersonalizado(false)
    setErrorDonacion('')
  }

  function cerrarModal() {
    setSeleccionado(null)
    setMonto('')
    setMontoPersonalizado(false)
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

  // polling automatico mientras hay pago pendiente
  useEffect(() => {
    if (!donacionPendienteId) return
    const intervalo = setInterval(async () => {
      try {
        const res = await confirmarPorDonacion(donacionPendienteId)
        const estado = res.data?.estado
        if (estado === 'COMPLETADA') {
          setExitoDonacion('¡Gracias por tu donación!')
          setDonacionPendienteId(null)
        } else if (estado === 'FALLIDA') {
          setErrorDonacion('El pago fue rechazado o cancelado.')
          setDonacionPendienteId(null)
        }
      } catch {
        // si falla la consulta, seguir intentando
      }
    }, 5000)
    return () => clearInterval(intervalo)
  }, [donacionPendienteId])

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
          <div style={{ background: '#fff', color: '#222', padding: '2rem', borderRadius: 4, width: 340, maxWidth: '90%' }}>
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
                <p>Completá el pago en la ventana de MercadoPago que se abrió. El estado se actualizará automáticamente.</p>
                {errorDonacion && <p style={{ color: 'red', fontSize: 13 }}>{errorDonacion}</p>}
                <button onClick={cerrarModal}>Cancelar</button>
              </div>
            ) : (
              <form onSubmit={handleDonar}>
                <p style={{ margin: '0 0 10px', fontWeight: 500 }}>Elegí un monto</p>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
                  {[1000, 2000, 5000, 10000].map(m => (
                    <button
                      key={m}
                      type="button"
                      onClick={() => { setMonto(m); setMontoPersonalizado(false) }}
                      style={{
                        padding: '6px 14px',
                        border: '1px solid #ccc',
                        borderRadius: 4,
                        background: monto === m && !montoPersonalizado ? '#1a73e8' : '#fff',
                        color: monto === m && !montoPersonalizado ? '#fff' : '#333',
                        cursor: 'pointer'
                      }}
                    >
                      ${formatMonto(m)}
                    </button>
                  ))}
                  <button
                    type="button"
                    onClick={() => { setMontoPersonalizado(true); setMonto('') }}
                    style={{
                      padding: '6px 14px',
                      border: '1px solid #ccc',
                      borderRadius: 4,
                      background: montoPersonalizado ? '#1a73e8' : '#fff',
                      color: montoPersonalizado ? '#fff' : '#333',
                      cursor: 'pointer'
                    }}
                  >
                    Otro monto
                  </button>
                </div>
                {montoPersonalizado && (
                  <input
                    type="number"
                    min={100}
                    step={1}
                    value={monto}
                    onChange={e => setMonto(e.target.value)}
                    placeholder="Ingresá el monto (mínimo $100)"
                    style={{ width: '100%', marginBottom: 12, boxSizing: 'border-box' }}
                    autoFocus
                    required
                  />
                )}
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
