import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getNotificaciones, marcarLeida, eliminarNotificacion } from '../api/notificacion'

function NotificacionBell() {
  const [notificaciones, setNotificaciones] = useState([])
  const [abierto, setAbierto] = useState(false)
  const navigate = useNavigate()
  const dropdownRef = useRef(null)

  const noLeidas = notificaciones.filter(n => !n.leida).length

  useEffect(() => {
    const cargar = () =>
      getNotificaciones().then(r => setNotificaciones(r.data)).catch(() => {})
    cargar()
    const intervalo = setInterval(cargar, 15000)
    return () => clearInterval(intervalo)
  }, [])

  useEffect(() => {
    function handleClickAfuera(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setAbierto(false)
      }
    }
    document.addEventListener('mousedown', handleClickAfuera)
    return () => document.removeEventListener('mousedown', handleClickAfuera)
  }, [])

  async function handleClick(n) {
    if (!n.leida) {
      await marcarLeida(n.id).catch(() => {})
      setNotificaciones(prev => prev.map(x => x.id === n.id ? { ...x, leida: true } : x))
    }
    setAbierto(false)
    navigate(n.url)
  }

  async function handleEliminar(e, id) {
    e.stopPropagation()
    await eliminarNotificacion(id).catch(() => {})
    setNotificaciones(prev => prev.filter(x => x.id !== id))
  }

  return (
    <div ref={dropdownRef} style={{ position: 'relative' }}>
      <button
        onClick={() => setAbierto(prev => !prev)}
        style={{ position: 'relative' }}
      >
        Notificaciones
        {noLeidas > 0 && (
          <span style={{
            position: 'absolute', top: -4, right: -4,
            background: '#e53935', color: '#fff',
            borderRadius: 10, fontSize: 10, padding: '1px 4px', lineHeight: 1.4,
            pointerEvents: 'none'
          }}>
            {noLeidas}
          </span>
        )}
      </button>

      {abierto && (
        <div style={{
          position: 'absolute', right: 0, top: '100%', marginTop: 4,
          background: '#fff', border: '1px solid #ddd', borderRadius: 6,
          boxShadow: '0 4px 12px rgba(0,0,0,0.12)', minWidth: 280, maxWidth: 340,
          maxHeight: 400, overflowY: 'auto', zIndex: 1000
        }}>
          {notificaciones.length === 0 ? (
            <p style={{ padding: '12px 16px', margin: 0, color: '#888', fontSize: 13, background: '#fff' }}>
              Sin notificaciones
            </p>
          ) : (
            notificaciones.map(n => (
              <div
                key={n.id}
                onClick={() => handleClick(n)}
                style={{
                  padding: '10px 12px',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: 'pointer',
                  background: n.leida ? '#fff' : '#f0f7ff',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  gap: 8
                }}
              >
                <span style={{ fontSize: 13, flex: 1, lineHeight: 1.4, color: '#222' }}>{n.mensaje}</span>
                <button
                  onClick={e => handleEliminar(e, n.id)}
                  style={{
                    background: 'none', border: 'none', cursor: 'pointer',
                    color: '#bbb', fontSize: 14, padding: 0, flexShrink: 0,
                    lineHeight: 1
                  }}
                  title="Eliminar notificación"
                >
                  Eliminar
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

export default NotificacionBell
