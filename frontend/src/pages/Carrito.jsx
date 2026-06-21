import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCarrito, actualizarCantidadCarrito, eliminarDelCarrito } from '../api/carrito'
import { crearPreferenciaVenta, confirmarPorVenta, getVentasPendientesDePago } from '../api/venta'
import { useAuth } from '../context/AuthContext'
import { formatMonto } from '../utils/formatMonto'

const API_BASE = import.meta.env.VITE_API_URL?.replace('/api', '') ?? 'http://localhost:8080'

function Carrito() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [grupos, setGrupos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  // ventaId pendiente de confirmar pago, por rescatistaId
  const [pagosPendientes, setPagosPendientes] = useState({})
  const [pagando, setPagando] = useState(null)
  const [mensajes, setMensajes] = useState({})

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    if (user && (user.role !== 'USER' || user.activeProfile !== 'ADOPTANTE')) {
      navigate('/')
      return
    }
    cargar()
    // retoma el polling de pagos que quedaron pendientes
    getVentasPendientesDePago().then(res => setPagosPendientes(res.data)).catch(() => {})
  }, [navigate, user])

  const cargar = useCallback(async () => {
    try {
      const res = await getCarrito()
      setGrupos(res.data)
    } catch {
      setError('No se pudo cargar el carrito.')
    } finally {
      setCargando(false)
    }
  }, [])

  async function handleCantidad(itemId, cantidad) {
    if (cantidad < 1) return
    try {
      const res = await actualizarCantidadCarrito(itemId, cantidad)
      setGrupos(res.data)
    } catch (err) {
      alert(err.response?.data || 'No se pudo actualizar la cantidad.')
    }
  }

  async function handleEliminar(itemId) {
    try {
      const res = await eliminarDelCarrito(itemId)
      setGrupos(res.data)
    } catch {
      alert('No se pudo quitar el ítem.')
    }
  }

  async function handlePagar(rescatistaId) {
    setPagando(rescatistaId)
    setMensajes(m => ({ ...m, [rescatistaId]: '' }))
    try {
      const res = await crearPreferenciaVenta(rescatistaId)
      window.open(res.data.checkoutUrl, '_blank')
      setPagosPendientes(p => ({ ...p, [rescatistaId]: res.data.id }))
    } catch (err) {
      setMensajes(m => ({ ...m, [rescatistaId]: err.response?.data || 'No se pudo iniciar el pago.' }))
    } finally {
      setPagando(null)
    }
  }

  // polling de los pagos pendientes, uno por rescatista
  useEffect(() => {
    const rescatistaIds = Object.keys(pagosPendientes)
    if (rescatistaIds.length === 0) return

    const intervalo = setInterval(async () => {
      for (const rescatistaId of rescatistaIds) {
        const ventaId = pagosPendientes[rescatistaId]
        try {
          const res = await confirmarPorVenta(ventaId)
          const estado = res.data?.estado
          if (estado === 'COMPLETADA') {
            setMensajes(m => ({ ...m, [rescatistaId]: '¡Compra confirmada! Te vas a poder contactar con el rescatista por chat.' }))
            setPagosPendientes(p => { const next = { ...p }; delete next[rescatistaId]; return next })
            cargar()
          } else if (estado === 'FALLIDA') {
            setMensajes(m => ({ ...m, [rescatistaId]: 'El pago fue rechazado o cancelado.' }))
            setPagosPendientes(p => { const next = { ...p }; delete next[rescatistaId]; return next })
          }
        } catch {
          // si falla la consulta, seguir intentando
        }
      }
    }, 5000)
    return () => clearInterval(intervalo)
  }, [pagosPendientes, cargar])

  if (cargando) return <p>Cargando...</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '0 16px' }}>
      <h2>Mi carrito</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {grupos.length === 0 && (
        <p>Tu carrito está vacío. <a href="/tiendas">Ver tiendas</a></p>
      )}

      {grupos.map(grupo => (
        <div key={grupo.rescatistaId} style={{ border: '1px solid #ccc', padding: 16, margin: '16px 0', borderRadius: 6 }}>
          <h3 style={{ marginTop: 0 }}>{grupo.rescatistaNombre}</h3>

          {grupo.items.map(item => (
            <div key={item.itemId} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0', borderBottom: '1px solid #eee' }}>
              {item.fotoUrl && (
                <img src={`${API_BASE}${item.fotoUrl}`} alt={item.titulo} style={{ width: 56, height: 56, objectFit: 'cover', borderRadius: 4 }} />
              )}
              <div style={{ flex: 1 }}>
                <strong>{item.titulo}</strong>
                <p style={{ margin: '2px 0', fontSize: 13, color: '#555' }}>
                  ${formatMonto(item.precio)} c/u
                </p>
              </div>
              <input
                type="number"
                min={1}
                max={item.stock}
                value={item.cantidad}
                onChange={e => handleCantidad(item.itemId, Number(e.target.value))}
                style={{ width: 56 }}
              />
              <button onClick={() => handleEliminar(item.itemId)}>Eliminar</button>
            </div>
          ))}

          <p style={{ textAlign: 'right', fontWeight: 600, margin: '12px 0 8px' }}>
            Total: ${formatMonto(grupo.total)}
          </p>

          {mensajes[grupo.rescatistaId] && <p>{mensajes[grupo.rescatistaId]}</p>}

          {pagosPendientes[grupo.rescatistaId] ? (
            <p style={{ fontSize: 13, color: '#555' }}>Completá el pago en la ventana de MercadoPago que se abrió. El estado se actualizará automáticamente.</p>
          ) : (
            <button onClick={() => handlePagar(grupo.rescatistaId)} disabled={pagando === grupo.rescatistaId}>
              {pagando === grupo.rescatistaId ? 'Procesando...' : `Pagar a ${grupo.rescatistaNombre}`}
            </button>
          )}
        </div>
      ))}
    </div>
  )
}

export default Carrito
