import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMisCompras } from '../api/venta'
import { formatMonto } from '../utils/formatMonto'
import { useAuth } from '../context/AuthContext'

const ESTADO_LABEL = {
  COMPLETADA: 'Pendiente de envío',
  ENVIADA: 'Enviada',
}

function MisCompras() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [compras, setCompras] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user && (user.role !== 'USER' || user.activeProfile !== 'ADOPTANTE')) {
      navigate('/')
      return
    }
    getMisCompras()
      .then(res => setCompras(res.data))
      .catch(() => setError('No se pudieron cargar las compras.'))
      .finally(() => setCargando(false))
  }, [user, navigate])

  if (cargando) return <p>Cargando...</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '0 16px' }}>
      <h2>Mis compras</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {compras.length === 0 && <p>Todavía no compraste nada en ninguna tienda.</p>}

      {compras.map(c => (
        <div key={c.id} style={{ border: '1px solid #ddd', padding: 16, margin: '12px 0', borderRadius: 6 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
            <strong>{c.otroUsuarioNombre}</strong>
            <span style={{ fontSize: 13, color: '#555' }}>{ESTADO_LABEL[c.estado]}</span>
          </div>
          <ul style={{ margin: '8px 0' }}>
            {c.items.map(it => (
              <li key={it.itemId}>{it.titulo} x{it.cantidad} · ${formatMonto(it.precioUnitario)} c/u</li>
            ))}
          </ul>
          <p style={{ margin: '4px 0', fontWeight: 600 }}>Total: ${formatMonto(c.montoTotal)}</p>
        </div>
      ))}
    </div>
  )
}

export default MisCompras
