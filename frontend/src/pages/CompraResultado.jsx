import { useSearchParams, Link } from 'react-router-dom'
import { useEffect } from 'react'
import { confirmarPagoVenta } from '../api/venta'

// MP redirige a /compra/exito, /compra/fallo o /compra/pendiente con query params:
// payment_id, status, external_reference

function CompraResultado({ estado }) {
  const [params] = useSearchParams()
  const paymentId = params.get('payment_id')
  const status = params.get('status')

  useEffect(() => {
    if (paymentId) {
      confirmarPagoVenta(paymentId).catch(() => {})
    }
  }, [paymentId])

  if (estado === 'exito') {
    return (
      <div style={{ maxWidth: 500, margin: '2rem auto', padding: '0 16px', textAlign: 'center' }}>
        <h2>¡Compra confirmada!</h2>
        <p>Tu pago fue procesado con éxito. Ya podés coordinar la entrega por chat con el rescatista.</p>
        {paymentId && <p style={{ fontSize: 13, color: '#555' }}>ID de pago: {paymentId}</p>}
        <Link to="/chats">Ir a mis chats</Link>
        {' · '}
        <Link to="/">Ir al inicio</Link>
      </div>
    )
  }

  if (estado === 'pendiente') {
    return (
      <div style={{ maxWidth: 500, margin: '2rem auto', padding: '0 16px', textAlign: 'center' }}>
        <h2>Pago pendiente</h2>
        <p>Tu pago está siendo procesado. Te notificaremos cuando se confirme.</p>
        {paymentId && <p style={{ fontSize: 13, color: '#555' }}>ID de pago: {paymentId}</p>}
        <Link to="/carrito">Volver al carrito</Link>
        {' · '}
        <Link to="/">Ir al inicio</Link>
      </div>
    )
  }

  // fallo
  return (
    <div style={{ maxWidth: 500, margin: '2rem auto', padding: '0 16px', textAlign: 'center' }}>
      <h2>No se pudo completar la compra</h2>
      <p>El pago fue rechazado o cancelado. Podés intentarlo de nuevo desde tu carrito.</p>
      {status && <p style={{ fontSize: 13, color: '#555' }}>Estado: {status}</p>}
      <Link to="/carrito">Volver al carrito</Link>
      {' · '}
      <Link to="/">Ir al inicio</Link>
    </div>
  )
}

export default CompraResultado
