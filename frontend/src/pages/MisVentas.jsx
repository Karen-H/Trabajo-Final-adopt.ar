import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getMisVentas, marcarVentaEnviada } from '../api/venta'
import { formatMonto } from '../utils/formatMonto'

const ESTADO_LABEL = {
  COMPLETADA: 'Pendiente de enviar',
  ENVIADA: 'Enviada',
}

function MisVentas() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [ventas, setVentas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [enviandoId, setEnviandoId] = useState(null)

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user?.activeProfile !== 'RESCATISTA') {
      navigate('/')
      return
    }
    if (!user?.tieneTienda) {
      navigate('/abrir-tienda')
      return
    }
    cargar()
  }, [user, navigate])

  async function cargar() {
    try {
      const res = await getMisVentas()
      setVentas(res.data)
    } catch {
      setError('No se pudieron cargar las ventas.')
    } finally {
      setCargando(false)
    }
  }

  async function handleMarcarEnviada(id) {
    setEnviandoId(id)
    try {
      await marcarVentaEnviada(id)
      setVentas(prev => prev.map(v => v.id === id ? { ...v, estado: 'ENVIADA' } : v))
    } catch (err) {
      alert(err.response?.data || 'No se pudo marcar como enviada.')
    } finally {
      setEnviandoId(null)
    }
  }

  if (cargando) return <p>Cargando...</p>

  const pendientes = ventas.filter(v => v.estado === 'COMPLETADA')
  const enviadas = ventas.filter(v => v.estado === 'ENVIADA')

  function VentaCard({ venta }) {
    return (
      <div style={{ border: '1px solid #ddd', padding: 16, margin: '12px 0', borderRadius: 6 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <strong>{venta.otroUsuarioNombre}</strong>
          <span style={{ fontSize: 13, color: '#555' }}>{ESTADO_LABEL[venta.estado]}</span>
        </div>
        <ul style={{ margin: '8px 0' }}>
          {venta.items.map(it => (
            <li key={it.itemId}>{it.titulo} x{it.cantidad} · ${formatMonto(it.precioUnitario)} c/u</li>
          ))}
        </ul>
        <p style={{ margin: '4px 0', fontWeight: 600 }}>Total: ${formatMonto(venta.montoTotal)}</p>
        {venta.estado === 'COMPLETADA' && (
          <button onClick={() => handleMarcarEnviada(venta.id)} disabled={enviandoId === venta.id}>
            {enviandoId === venta.id ? 'Marcando...' : 'Marcar como enviada'}
          </button>
        )}
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '0 16px' }}>
      <h2>Mis ventas</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <h3>Pendientes de enviar</h3>
      {pendientes.length === 0 && <p>No tenés ventas pendientes de enviar.</p>}
      {pendientes.map(v => <VentaCard key={v.id} venta={v} />)}

      <h3>Enviadas</h3>
      {enviadas.length === 0 && <p>Todavía no enviaste ninguna venta.</p>}
      {enviadas.map(v => <VentaCard key={v.id} venta={v} />)}
    </div>
  )
}

export default MisVentas
