import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getTiendas } from '../api/item'
import { getProvincias } from '../api/georef'
import { useAuth } from '../context/AuthContext'

function Tiendas() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [tiendas, setTiendas] = useState([])
  const [provincias, setProvincias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const [filtroQ, setFiltroQ] = useState('')
  const [filtroProvincia, setFiltroProvincia] = useState('')

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
    cargar()
  }, [user, navigate])

  async function cargar(q, provincia) {
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

  function handleBuscar(e) {
    e.preventDefault()
    cargar(filtroQ, filtroProvincia)
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '0 16px' }}>
      <h2>Tiendas de rescatistas</h2>
      <p>Comprá artículos para mascotas y ayudá directamente a los rescatistas.</p>

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

      {!cargando && tiendas.length === 0 && (
        <p>No hay tiendas disponibles en este momento.</p>
      )}

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
    </div>
  )
}

export default Tiendas
