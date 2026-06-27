import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { getItemsDeTienda } from '../api/item'
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

function TiendaDetalle() {
  const { rescatistaId } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [cantidades, setCantidades] = useState({})
  const [agregandoId, setAgregandoId] = useState(null)
  const [mensaje, setMensaje] = useState('')
  const [filtroQ, setFiltroQ] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user && (user.role !== 'USER' || user.activeProfile !== 'ADOPTANTE')) {
      navigate('/')
      return
    }
    getItemsDeTienda(rescatistaId)
      .then(res => setItems(res.data))
      .catch(() => setError('No se pudo cargar la tienda.'))
      .finally(() => setCargando(false))
  }, [rescatistaId, user, navigate])

  async function handleAgregar(item) {
    if (!localStorage.getItem('token')) {
      setMensaje('Tenés que iniciar sesión para comprar.')
      return
    }
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

  if (cargando) return <p>Cargando...</p>

  const itemsFiltrados = items.filter(item => {
    const matchQ = !filtroQ || item.titulo.toLowerCase().includes(filtroQ.toLowerCase())
    const matchTipo = !filtroTipo || item.tipo === filtroTipo
    return matchQ && matchTipo
  })

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '0 16px' }}>
      <Link to="/tiendas">Volver a tiendas</Link>
      <h2>Items en venta</h2>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 }}>
        <input
          type="text"
          placeholder="Buscar por item..."
          value={filtroQ}
          onChange={e => setFiltroQ(e.target.value)}
          style={{ flex: 1, minWidth: 200 }}
        />
        <select value={filtroTipo} onChange={e => setFiltroTipo(e.target.value)}>
          <option value="">Todos los tipos</option>
          {Object.entries(TIPOS_ITEM).map(([valor, etiqueta]) => (
            <option key={valor} value={valor}>{etiqueta}</option>
          ))}
        </select>
      </div>

      {error && <p style={{ color: 'red' }}>{error}</p>}
      {mensaje && <p>{mensaje}</p>}

      {items.length === 0 && !error && <p>Esta tienda no tiene items disponibles.</p>}
      {items.length > 0 && itemsFiltrados.length === 0 && <p>No hay items que coincidan con la búsqueda.</p>}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 16 }}>
        {itemsFiltrados.map(item => {
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
    </div>
  )
}

export default TiendaDetalle
