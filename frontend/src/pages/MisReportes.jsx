import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getMisReportes, resolverReporte } from '../api/reporte'
import { agregarFotosAnimal } from '../api/animal'

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

const ETIQUETA_ESTADO = {
  PERDIDO: 'Perdido',
  ENCONTRADO: 'Encontrado',
  RESUELTO: 'Resuelto',
}

const ETIQUETA_FOTO_ESTADO = {
  PENDIENTE: 'Pendiente de revision',
  RECHAZADA: 'Rechazada',
}

function estadoRevision(reporte) {
  if (reporte.rechazado) return { texto: 'Rechazado', color: '#c00' }
  if (reporte.aprobado) return { texto: 'Aprobado', color: '#080' }
  return { texto: 'En revision', color: '#888' }
}

function MisReportes() {
  const navigate = useNavigate()
  const [reportes, setReportes] = useState([])
  const [error, setError] = useState('')
  const [fotosNuevas, setFotosNuevas] = useState({})

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    getMisReportes()
      .then(res => setReportes(res.data))
      .catch(() => setError('No se pudieron cargar tus reportes.'))
  }, [navigate])

  async function handleResolver(id) {
    setError('')
    try {
      const res = await resolverReporte(id)
      setReportes(prev => prev.map(r => r.id === id ? res.data : r))
    } catch (err) {
      setError(err.response?.data || 'No se pudo marcar como resuelto.')
    }
  }

  async function handleAgregarFotos(id) {
    setError('')
    const files = fotosNuevas[id]
    if (!files || files.length === 0) return
    const formData = new FormData()
    Array.from(files).forEach(f => formData.append('fotos', f))
    try {
      const res = await agregarFotosAnimal(id, formData)
      setReportes(prev => prev.map(r => r.id === id ? res.data : r))
      setFotosNuevas(prev => ({ ...prev, [id]: null }))
    } catch (err) {
      setError(err.response?.data || 'No se pudieron agregar las fotos.')
    }
  }

  return (
    <div>
      <h2>Mis reportes</h2>
      <Link to="/agregar-reporte">+ Publicar perdido / encontrado</Link>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {reportes.length === 0 ? (
        <p>Todavia no publicaste ningun reporte.</p>
      ) : (
        reportes.map(reporte => {
          const revision = estadoRevision(reporte)
          const resuelto = reporte.estado === 'RESUELTO'
          return (
            <div key={reporte.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
              <h3>
                {ETIQUETA_TIPO[reporte.tipo]}
                {'  '}
                <span style={{ fontSize: '0.85rem', color: revision.color }}>
                  [{revision.texto}]
                </span>
              </h3>

              {reporte.rechazado && reporte.motivoRechazo && (
                <p style={{ color: '#c00' }}>Motivo de rechazo: {reporte.motivoRechazo}</p>
              )}

              <p>
                Estado:{' '}
                <strong style={{ color: resuelto ? '#080' : undefined }}>
                  {ETIQUETA_ESTADO[reporte.estado]}
                </strong>
              </p>

              {reporte.direccion && <p>Visto en: {reporte.direccion}</p>}
              {reporte.estado === 'ENCONTRADO' && <p>En posesión del publicador: {reporte.enPosesionDelPublicador ? 'Sí' : 'No'}</p>}
              {reporte.descripcion && <p>Descripción: {reporte.descripcion}</p>}

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '0.5rem 0' }}>
                {reporte.fotos.map(foto => (
                  <div key={foto.id} style={{ textAlign: 'center' }}>
                    <img
                      src={foto.url}
                      alt="foto del reporte"
                      style={{ width: 120, height: 120, objectFit: 'cover', display: 'block' }}
                    />
                    {ETIQUETA_FOTO_ESTADO[foto.estado] && (
                      <small style={{ color: foto.estado === 'RECHAZADA' ? '#c00' : '#888' }}>
                        {ETIQUETA_FOTO_ESTADO[foto.estado]}
                        {foto.estado === 'RECHAZADA' && foto.motivoRechazo && ': ' + foto.motivoRechazo}
                      </small>
                    )}
                  </div>
                ))}
              </div>

              {!reporte.rechazado && reporte.fotos.length < 5 && (
                <div style={{ margin: '0.5rem 0' }}>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={e => setFotosNuevas(prev => ({ ...prev, [reporte.id]: e.target.files }))}
                  />
                  <button
                    onClick={() => handleAgregarFotos(reporte.id)}
                    disabled={!fotosNuevas[reporte.id] || fotosNuevas[reporte.id].length === 0}
                    style={{ marginLeft: 8 }}
                  >
                    Agregar fotos
                  </button>
                </div>
              )}

              {reporte.aprobado && !resuelto && (
                <div style={{ marginTop: '0.5rem' }}>
                  <button onClick={() => handleResolver(reporte.id)}>
                    Marcar como resuelto
                  </button>
                </div>
              )}
            </div>
          )
        })
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default MisReportes
