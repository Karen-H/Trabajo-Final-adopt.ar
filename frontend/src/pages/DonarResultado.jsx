import { useSearchParams, Link } from 'react-router-dom'
import { useEffect } from 'react'
import { confirmarPago } from '../api/donacion'

// MP redirige a /donar/exito, /donar/fallo o /donar/pendiente con query params:
// payment_id, status, external_reference

function DonarResultado({ estado }) {
  const [params] = useSearchParams()
  const paymentId = params.get('payment_id')
  const status = params.get('status')

  useEffect(() => {
    if (paymentId) {
      confirmarPago(paymentId).catch(() => {})
    }
  }, [paymentId])

  if (estado === 'exito') {
    return (
      <div style={{ maxWidth: 500, margin: '2rem auto', padding: '0 16px', textAlign: 'center' }}>
        <h2>¡Gracias por tu donación!</h2>
        <p>Tu pago fue procesado con éxito.</p>
        {paymentId && <p style={{ fontSize: 13, color: '#555' }}>ID de pago: {paymentId}</p>}
        <Link to="/donar">Volver al listado</Link>
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
        <Link to="/donar">Volver al listado</Link>
        {' · '}
        <Link to="/">Ir al inicio</Link>
      </div>
    )
  }

  // fallo
  return (
    <div style={{ maxWidth: 500, margin: '2rem auto', padding: '0 16px', textAlign: 'center' }}>
      <h2>No se pudo completar la donación</h2>
      <p>El pago fue rechazado o cancelado. Podés intentarlo de nuevo.</p>
      {status && <p style={{ fontSize: 13, color: '#555' }}>Estado: {status}</p>}
      <Link to="/donar">Intentar de nuevo</Link>
      {' · '}
      <Link to="/">Ir al inicio</Link>
    </div>
  )
}

export default DonarResultado
