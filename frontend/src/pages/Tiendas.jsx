import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getTiendas, getTodosLosItems } from '../api/item'
import { getProvincias } from '../api/georef'
import { agregarAlCarrito } from '../api/carrito'
import { useAuth } from '../context/AuthContext'
import { formatMonto } from '../utils/formatMonto'

const TIPOS_ITEM = {
  INDUMENTARIA: 'Indumentaria',
  ACCESORIO: 'Accesorio',
  ALIMENTO: 'Alimento',
  JUGUETE: 'Juguete',
  HIGIENE: 'Higiene',
  CAMA: 'Cama',
  TRANSPORTE: 'Transporte',
  OTRO: 'Otro',
}

const API_BASE = import.meta.env.VITE_API_URL?.replace('/api', '') ?? 'http://localhost:8080'

function Tiendas() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [vista, setVista] = useState('items') // 'rescatistas' | 'items'

  const [tiendas, setTiendas] = useState([])
  const [provincias, setProvincias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [filtroQ, setFiltroQ] = useState('')
  const [filtroProvincia, setFiltroProvincia] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')

  const [items, setItems] = useState([])
  const [cantidades, setCantidades] = useState({})
  const [agregandoId, setAgregandoId] = useState(null)
  const [mensaje, setMensaje] = useState('')

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user && (user.role !== 'USER' || user.activeProfile !== 'ADOPTANTE')) {
      navigate('/')
      return
    }
    getProvincias().then(setProvincias).catch(() => [])
  }, [user, navigate])

  useEffect(() => {
    if (vista === 'rescatistas') {
      cargarTiendas(filtroQ, filtroProvincia)
    } else {
      cargarItems(filtroQ, filtroTipo, filtroProvincia)
    }
  }, [vista, filtroQ, filtroProvincia, filtroTipo])

  async function cargarTiendas(q, provincia) {
    setCargando(true)
    setError('')
    try {
      const res = await getTiendas({ q, provincia })
      setTiendas(res.data)
    } catch {
      setError('No se pudo cargar el listado.')
    } finally {
      setCargando(false)
    }
  }

  async function cargarItems(q, tipo, provincia) {
    setCargando(true)
    setError('')
    try {
      const res = await getTodosLosItems({ q, tipo, provincia })
      setItems(res.data)
    } catch {
      setError('No se pudo cargar el listado.')
    } finally {
      setCargando(false)
    }
  }

  function handleCambiarVista(nuevaVista) {
    setVista(nuevaVista)
    setError('')
  }

  async function handleAgregar(item) {
    const cantidad = cantidades[item.id] || 1
    setAgregandoId(item.id)
    setMensaje('')
    try {
      await agregarAlCarrito(item.id, cantidad)
      setMensaje(`Agregaste "${item.titulo}" al carrito.`)
    } catch (err) {
      setMensaje(err.response?.data || 'No se pudo agregar al carrito.')
    } finally {
      setAgregandoId(null)
    }
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '0 16px' }}>
      <h2>Tiendas de rescatistas</h2>
      <p>Comprá artículos para mascotas y ayudá directamente a los rescatistas.</p>

      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <button
          onClick={() => handleCambiarVista('items')}
          style={{ fontWeight: vista === 'items' ? 700 : 400 }}
        >
          Ver todos los items
        </button>
        <button
          onClick={() => handleCambiarVista('rescatistas')}
          style={{ fontWeight: vista === 'rescatistas' ? 700 : 400 }}
        >
          Ver por rescatista
        </button>
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 }}>
        <input
          type="text"
          placeholder={vista === 'rescatistas' ? 'Buscar por nombre u organización...' : 'Buscar por item, rescatista u organización...'}
          value={filtroQ}
          onChange={e => setFiltroQ(e.target.value)}
          style={{ flex: 1, minWidth: 200 }}
        />
        {vista === 'items' && (
          <select value={filtroTipo} onChange={e => setFiltroTipo(e.target.value)}>
            <option value="">Todos los tipos</option>
            {Object.entries(TIPOS_ITEM).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>{etiqueta}</option>
            ))}
          </select>
        )}
        <select value={filtroProvincia} onChange={e => setFiltroProvincia(e.target.value)}>
          <option value="">Todas las provincias</option>
          {provincias.map(p => (
            <option key={p.id} value={p.nombre}>{p.nombre}</option>
          ))}
        </select>
      </div>

      {error && <p style={{ color: 'red' }}>{error}</p>}
      {mensaje && vista === 'items' && <p>{mensaje}</p>}
      {cargando && <p>Cargando...</p>}

      {vista === 'rescatistas' && !cargando && tiendas.length === 0 && (
        <p>No hay tiendas disponibles en este momento.</p>
      )}
      {vista === 'items' && !cargando && items.length === 0 && (
        <p>No hay items disponibles en este momento.</p>
      )}

      {vista === 'rescatistas' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
          {tiendas.map(t => (
            <div key={t.id} style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: 4 }}>
              <h3 style={{ margin: '0 0 4px' }}>{t.organizacion || `${t.nombre} ${t.apellido}`}</h3>
              {t.organizacion && <p style={{ margin: '0 0 4px', fontSize: 13, color: '#555' }}>{t.nombre} {t.apellido}</p>}
              {(t.ciudad || t.provincia) && (
                <p style={{ margin: '0 0 12px', fontSize: 13 }}>
                  {[t.ciudad, t.provincia].filter(Boolean).join(', ')}
                </p>
              )}
              <Link to={`/tiendas/${t.id}`}>Ver items</Link>
            </div>
          ))}
        </div>
      )}

      {vista === 'items' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 16 }}>
          {items.map(item => {
            const fotoUrl = item.fotos?.find(f => f.estado === 'APROBADA')?.url
            const sinStock = item.stock <= 0
            return (
              <div key={item.id} style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: 4 }}>
                {fotoUrl && (
                  <img
                    src={`${API_BASE}${fotoUrl}`}
                    alt={item.titulo}
                    style={{ width: '100%', height: 140, objectFit: 'cover', borderRadius: 4, marginBottom: 8 }}
                  />
                )}
                <strong>{item.titulo}</strong>
                <p style={{ margin: '4px 0', fontSize: 13, color: '#555' }}>{TIPOS_ITEM[item.tipo] ?? item.tipo}</p>
                <p style={{ margin: '4px 0', fontSize: 13, color: '#777' }}>{item.rescatistaOrganizacion || item.rescatistaNombre}</p>
                {(item.rescatistaCiudad || item.rescatistaProvincia) && (
                  <p style={{ margin: '4px 0', fontSize: 12, color: '#999' }}>
                    {[item.rescatistaCiudad, item.rescatistaProvincia].filter(Boolean).join(', ')}
                  </p>
                )}
                {item.descripcion && <p style={{ margin: '4px 0', fontSize: 14 }}>{item.descripcion}</p>}
                {item.precio != null && <p style={{ margin: '4px 0', fontWeight: 600 }}>${formatMonto(item.precio)}</p>}
                <p style={{ margin: '4px 0', fontSize: 13, color: sinStock ? 'red' : '#555' }}>
                  {sinStock ? 'Sin stock' : `Stock: ${item.stock}`}
                </p>

                {!sinStock && (
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginTop: 8 }}>
                    <input
                      type="number"
                      min={1}
                      max={item.stock}
                      value={cantidades[item.id] || 1}
                      onChange={e => setCantidades(c => ({ ...c, [item.id]: Number(e.target.value) }))}
                      style={{ width: 60 }}
                    />
                    <button onClick={() => handleAgregar(item)} disabled={agregandoId === item.id}>
                      {agregandoId === item.id ? 'Agregando...' : 'Agregar al carrito'}
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default Tiendas
