import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import AdminDashboard from './AdminDashboard'
import {
  getUsuarios, eliminarUsuario, actualizarRol,
  getTiendasActivas, revocarTienda,
} from '../api/admin'
import {
  getSolicitudesAdmin, aceptarSolicitud, editarLinkSolicitud,
  aprobarSolicitud, rechazarSolicitud, reprogramarSolicitudAdmin,
} from '../api/tienda'
import { getDenunciasPendientes, desestimar, eliminarPublicacionDenuncia } from '../api/denuncia'

const MOTIVO_REPROGRAMACION_OPCIONES = [
  { value: 'PROBLEMA_TECNICO', label: 'Problema técnico' },
  { value: 'RESCATISTA_NO_SE_PRESENTO', label: 'Rescatista no se presentó' },
  { value: 'ADMINISTRADOR_NO_SE_PRESENTO', label: 'Administrador no se presentó' },
  { value: 'ADMINISTRADOR_NO_PODRA_PRESENTARSE', label: 'Administrador no podrá presentarse' },
  { value: 'RESCATISTA_SIN_EVIDENCIA', label: 'Rescatista sin evidencia disponible' },
  { value: 'ERROR_EN_HORARIO', label: 'Error en el horario' },
]

const MOTIVO_REPROGRAMACION_LABEL = Object.fromEntries(
  MOTIVO_REPROGRAMACION_OPCIONES.map(o => [o.value, o.label])
)

const ESTADO_SOLICITUD_LABEL = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  REPROGRAMADA: 'Reprogramada',
}

function formatFecha(fecha) {
  return new Date(fecha + 'T00:00:00').toLocaleDateString('es-AR')
}

function formatHora(hora) {
  return hora.substring(0, 5) + 'hs'
}

function llamadaTermino(s) {
  const [h, m] = s.horaPreferida.split(':').map(Number)
  const endMs = new Date(`${s.fechaPreferida}T${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:00`).getTime() + 30 * 60 * 1000
  return Date.now() >= endMs
}

function horaFinSlot(s) {
  const [h, m] = s.horaPreferida.split(':').map(Number)
  const endMin = h * 60 + m + 30
  return `${String(Math.floor(endMin / 60)).padStart(2, '0')}:${String(endMin % 60).padStart(2, '0')}`
}


const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_ESTADO = { PERDIDO: 'Perdido', ENCONTRADO: 'Encontrado' }

function AdminPanel() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [tab, setTab] = useState('tiendas')
  const [error, setError] = useState('')

  // tienda
  const [solicitudes, setSolicitudes] = useState([])
  const [linkMap, setLinkMap] = useState({})
  const [motivoRechazarMap, setMotivoRechazarMap] = useState({})
  const [motivoReprogramarMap, setMotivoReprogramarMap] = useState({})
  const [llamadaEstado, setLlamadaEstado] = useState({})

  // denuncias
  const [denuncias, setDenuncias] = useState([])

  // usuarios
  const [usuarios, setUsuarios] = useState([])

  // tiendas activas
  const [tiendasActivas, setTiendasActivas] = useState([])

  const esAdmin = user?.role === 'ADMIN'

  useEffect(() => {
    const role = localStorage.getItem('role')
    if (role !== 'ADMIN' && role !== 'MODERADOR') {
      navigate('/')
      return
    }
    cargarSolicitudesTienda()
    cargarDenuncias()
    cargarTiendasActivas()
    if (role === 'ADMIN') cargarUsuarios()
  }, [navigate])

  function cargarDenuncias() {
    getDenunciasPendientes()
      .then(res => setDenuncias(res.data))
      .catch(() => {})
  }

  function cargarUsuarios() {
    getUsuarios()
      .then(res => setUsuarios(res.data))
      .catch(() => {})
  }

  function cargarTiendasActivas() {
    getTiendasActivas()
      .then(res => setTiendasActivas(res.data))
      .catch(() => {})
  }

  function cargarSolicitudesTienda() {
    getSolicitudesAdmin()
      .then(res => setSolicitudes(res.data))
      .catch(() => {})
  }

  async function handleAceptarSolicitud(id) {
    const link = linkMap[id]
    if (!link?.trim()) { setError('Ingresá el link de llamada antes de aceptar'); return }
    setError('')
    try {
      const res = await aceptarSolicitud(id, link)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al aceptar la solicitud')
    }
  }

  async function handleEditarLink(id) {
    const link = linkMap[id]
    if (!link?.trim()) { setError('Ingresá el nuevo link'); return }
    setError('')
    try {
      const res = await editarLinkSolicitud(id, link)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al editar el link')
    }
  }

  async function handleAprobarSolicitud(id) {
    if (!confirm('¿Verificar a este rescatista? Vas a habilitarle tanto la tienda como las donaciones.')) return
    setError('')
    try {
      const res = await aprobarSolicitud(id)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al aprobar la solicitud')
    }
  }

  async function handleRechazarSolicitud(id) {
    const motivo = motivoRechazarMap[id]
    if (!motivo?.trim()) { setError('Ingresá un motivo de rechazo'); return }
    setError('')
    try {
      const res = await rechazarSolicitud(id, motivo)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al rechazar la solicitud')
    }
  }

  async function handleReprogramarSolicitud(id) {
    const motivo = motivoReprogramarMap[id]
    if (!motivo) { setError('Elegí un motivo de reprogramación'); return }
    setError('')
    try {
      const res = await reprogramarSolicitudAdmin(id, motivo)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al reprogramar')
    }
  }

  async function handleReprogramarConMotivo(id, motivo) {
    setError('')
    try {
      const res = await reprogramarSolicitudAdmin(id, motivo)
      setSolicitudes(prev => prev.map(s => s.id === id ? res.data : s))
    } catch (e) {
      setError(e.response?.data || 'Error al reprogramar')
    }
  }

  return (
    <div>
      <h2>Panel de administracion</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div>
        <button onClick={() => setTab('tiendas')} disabled={tab === 'tiendas'}>
          Verificaciones ({tiendasActivas.length} verificados · {solicitudes.length} solicitudes)
        </button>
        {' '}
        <button onClick={() => setTab('denuncias')} disabled={tab === 'denuncias'}>
          Denuncias ({denuncias.length})
        </button>
        {esAdmin && (
          <>
            {' '}
            <button onClick={() => setTab('usuarios')} disabled={tab === 'usuarios'}>
              Usuarios ({usuarios.length})
            </button>
            {' '}
            <button onClick={() => setTab('dashboard')} disabled={tab === 'dashboard'}>
              Dashboard
            </button>
          </>
        )}
      </div>

      <br />
      <Link to="/">Volver al inicio</Link>

      {tab === 'tiendas' && (
        <div>

          {/* Solicitudes de verificación */}
          <h3>Solicitudes de verificación ({solicitudes.length})</h3>
          {solicitudes.length === 0 && <p>No hay solicitudes de verificación.</p>}
          {solicitudes.map(s => {
            const esMia = s.adminAsignadoId === Number(localStorage.getItem('id') ?? 0) || s.adminAsignadoId != null
            const puedoActuar = s.adminAsignadoId != null && s.adminAsignadoId === Number(localStorage.getItem('id') ?? -1)
            return (
              <div key={s.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
                <p><strong>Estado:</strong> {ESTADO_SOLICITUD_LABEL[s.estado]}</p>
                <p><strong>Rescatista:</strong> {s.rescatistaNombre} {s.rescatistaApellido} · {s.rescatistaEmail} · {s.rescatistaTel}</p>
                {s.rescatistaOrganizacion && <p><strong>Organización:</strong> {s.rescatistaOrganizacion}</p>}
                <p><strong>Turno solicitado:</strong> {formatFecha(s.fechaPreferida)} a las {formatHora(s.horaPreferida)}</p>
                {s.adminAsignadoNombre
                  ? <p><strong>Asignado a:</strong> {s.adminAsignadoNombre} {s.adminAsignadoApellido}</p>
                  : <p><strong>Asignado a:</strong> sin asignar (pendiente de reprogramación por rescatista)</p>
                }
                {s.linkLlamada && <p><strong>Link:</strong> <a href={s.linkLlamada} target="_blank" rel="noreferrer">{s.linkLlamada}</a></p>}
                {s.motivoReprogramacion && <p><strong>Motivo reprogramación:</strong> {MOTIVO_REPROGRAMACION_LABEL[s.motivoReprogramacion]}</p>}
                {s.motivoRechazo && <p><strong>Motivo rechazo:</strong> {s.motivoRechazo}</p>}

                {/* acciones: solo si esta solicitud está asignada a este admin */}
                {s.estado === 'PENDIENTE' && s.adminAsignadoId != null && (
                  <div style={{ marginTop: 8 }}>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
                      <input
                        type="text"
                        placeholder="Link de videollamada"
                        value={linkMap[s.id] || ''}
                        onChange={e => setLinkMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                        style={{ width: 300 }}
                      />
                      <button onClick={() => handleAceptarSolicitud(s.id)}>Aceptar llamada</button>
                    </div>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                      <input
                        type="text"
                        placeholder="Motivo de rechazo"
                        value={motivoRechazarMap[s.id] || ''}
                        onChange={e => setMotivoRechazarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                        style={{ width: 250 }}
                      />
                      <button onClick={() => handleRechazarSolicitud(s.id)}>Rechazar</button>
                      <select
                        value={motivoReprogramarMap[s.id] || ''}
                        onChange={e => setMotivoReprogramarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                      >
                        <option value="">-- Motivo reprogramación --</option>
                        {MOTIVO_REPROGRAMACION_OPCIONES.map(o => (
                          <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                      </select>
                      <button onClick={() => handleReprogramarSolicitud(s.id)}>Reprogramar</button>
                    </div>
                  </div>
                )}

                {s.estado === 'ACEPTADA' && s.adminAsignadoId != null && (() => {
                  const paso = llamadaEstado[s.id]
                  const terminada = llamadaTermino(s)
                  const setPaso = v => setLlamadaEstado(prev => ({ ...prev, [s.id]: v }))
                  return (
                    <div style={{ marginTop: 8 }}>

                      {/* Editar link, siempre visible */}
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
                        <input
                          type="text"
                          placeholder="Editar link de llamada"
                          value={linkMap[s.id] || s.linkLlamada || ''}
                          onChange={e => setLinkMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                          style={{ width: 300 }}
                        />
                        <button onClick={() => handleEditarLink(s.id)}>Actualizar link</button>
                      </div>

                      {/* Reprogramación anticipada (solo si no estamos en el flujo post-llamada) */}
                      {!paso && (
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 8 }}>
                          <select
                            value={motivoReprogramarMap[s.id] || ''}
                            onChange={e => setMotivoReprogramarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                          >
                            <option value="">-- Reprogramar antes de la llamada --</option>
                            {MOTIVO_REPROGRAMACION_OPCIONES.filter(o =>
                              ['ADMINISTRADOR_NO_PODRA_PRESENTARSE', 'ERROR_EN_HORARIO', 'PROBLEMA_TECNICO'].includes(o.value)
                            ).map(o => (
                              <option key={o.value} value={o.value}>{o.label}</option>
                            ))}
                          </select>
                          <button onClick={() => handleReprogramarSolicitud(s.id)}>Reprogramar</button>
                        </div>
                      )}

                      {/* Botón principal: habilitado solo cuando terminó el slot */}
                      {!paso && (
                        <div style={{ marginTop: 4 }}>
                          <button
                            disabled={!terminada}
                            onClick={() => setPaso('pregunta')}
                            title={!terminada ? `Disponible a partir de las ${horaFinSlot(s)}hs` : ''}
                          >
                            ¿La llamada se llevó a cabo?
                          </button>
                          {!terminada && (
                            <span style={{ marginLeft: 8, color: '#888', fontSize: '0.85em' }}>
                              Habilitado a las {horaFinSlot(s)}hs del {formatFecha(s.fechaPreferida)}
                            </span>
                          )}
                        </div>
                      )}

                      {/* Paso 1: Sí / No */}
                      {paso === 'pregunta' && (
                        <div style={{ marginTop: 8 }}>
                          <strong>¿La llamada se llevó a cabo?</strong>
                          <div style={{ marginTop: 6, display: 'flex', gap: 8 }}>
                            <button onClick={() => setPaso('si')}>Sí</button>
                            <button onClick={() => setPaso('no')}>No</button>
                            <button onClick={() => setPaso(null)} style={{ color: '#888' }}>Cancelar</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 2a: No → ¿Quién no se presentó? */}
                      {paso === 'no' && (
                        <div style={{ marginTop: 8 }}>
                          <strong>¿Quién no se presentó?</strong>
                          <div style={{ marginTop: 6, display: 'flex', gap: 8 }}>
                            <button onClick={() => setPaso('no_admin')}>El administrador</button>
                            <button onClick={() => setPaso('no_rescatista')}>El rescatista</button>
                            <button onClick={() => setPaso('pregunta')} style={{ color: '#888' }}>Volver</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 3a: Admin no se presentó → solo Reprogramar */}
                      {paso === 'no_admin' && (
                        <div style={{ marginTop: 8 }}>
                          <p style={{ margin: '0 0 6px' }}>El administrador no se presentó. La solicitud volverá a estar disponible para el rescatista.</p>
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button onClick={() => handleReprogramarConMotivo(s.id, 'ADMINISTRADOR_NO_SE_PRESENTO')}>Reprogramar</button>
                            <button onClick={() => setPaso('no')} style={{ color: '#888' }}>Volver</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 3b: Rescatista no se presentó → Reprogramar o Rechazar */}
                      {paso === 'no_rescatista' && (
                        <div style={{ marginTop: 8 }}>
                          <p style={{ margin: '0 0 6px' }}>El rescatista no se presentó.</p>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                            <button onClick={() => handleReprogramarConMotivo(s.id, 'RESCATISTA_NO_SE_PRESENTO')}>Reprogramar</button>
                            <input
                              type="text"
                              placeholder="Motivo de rechazo"
                              value={motivoRechazarMap[s.id] || ''}
                              onChange={e => setMotivoRechazarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                              style={{ width: 250 }}
                            />
                            <button onClick={() => handleRechazarSolicitud(s.id)}>Rechazar</button>
                            <button onClick={() => setPaso('no')} style={{ color: '#888' }}>Volver</button>
                          </div>
                        </div>
                      )}

                      {/* Paso 2b: Sí → todos los botones */}
                      {paso === 'si' && (
                        <div style={{ marginTop: 8 }}>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                            <button onClick={() => handleAprobarSolicitud(s.id)}>Aprobar verificación</button>
                            <input
                              type="text"
                              placeholder="Motivo de rechazo"
                              value={motivoRechazarMap[s.id] || ''}
                              onChange={e => setMotivoRechazarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                              style={{ width: 250 }}
                            />
                            <button onClick={() => handleRechazarSolicitud(s.id)}>Rechazar</button>
                            <select
                              value={motivoReprogramarMap[s.id] || ''}
                              onChange={e => setMotivoReprogramarMap(prev => ({ ...prev, [s.id]: e.target.value }))}
                            >
                              <option value="">-- Motivo reprogramación --</option>
                              {MOTIVO_REPROGRAMACION_OPCIONES.map(o => (
                                <option key={o.value} value={o.value}>{o.label}</option>
                              ))}
                            </select>
                            <button onClick={() => handleReprogramarSolicitud(s.id)}>Reprogramar</button>
                          </div>
                        </div>
                      )}

                    </div>
                  )
                })()}
              </div>
            )
          })}

          <hr style={{ margin: '2rem 0' }} />

          {/* Rescatistas verificados */}
          <h3>Rescatistas verificados ({tiendasActivas.length})</h3>
          <p style={{ fontSize: 13, color: '#666' }}>
            La verificación habilita al rescatista a vender en tienda y/o aceptar donaciones libremente, sin nueva aprobación.
          </p>
          {tiendasActivas.length === 0 && <p>No hay rescatistas verificados.</p>}
          {tiendasActivas.map(t => (
            <div key={t.usuarioId} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
              <p><strong>{t.nombre} {t.apellido}</strong> · {t.email} · {t.tel}</p>
              {t.organizacion && <p>Organización: {t.organizacion}</p>}
              {t.provincia && <p>Ubicación: {t.ciudad}, {t.provincia}</p>}
              <p style={{ fontSize: 13, color: t.aceptaDonaciones ? '#1a6e2e' : '#888' }}>
                {t.aceptaDonaciones ? 'Acepta donaciones actualmente' : 'No tiene activadas las donaciones'}
              </p>
              <button
                style={{ color: '#c00', marginTop: 8 }}
                onClick={async () => {
                  if (!confirm(`¿Revocar la verificación de ${t.nombre} ${t.apellido}? Esto le quita tanto la tienda como la posibilidad de aceptar donaciones.`)) return
                  try {
                    await revocarTienda(t.usuarioId)
                    setTiendasActivas(prev => prev.filter(x => x.usuarioId !== t.usuarioId))
                  } catch {
                    alert('No se pudo revocar la verificación')
                  }
                }}
              >
                Revocar verificación
              </button>
            </div>
          ))}
        </div>
      )}


      {tab === 'usuarios' && esAdmin && (
        <div>
          {usuarios.length === 0 && <p>No hay usuarios registrados.</p>}
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
            <thead>
              <tr style={{ background: '#f0f0f0' }}>
                <th style={{ textAlign: 'left', padding: '8px', border: '1px solid #ccc' }}>Nombre</th>
                <th style={{ textAlign: 'left', padding: '8px', border: '1px solid #ccc' }}>Email</th>
                <th style={{ textAlign: 'left', padding: '8px', border: '1px solid #ccc' }}>Rol</th>
                <th style={{ textAlign: 'left', padding: '8px', border: '1px solid #ccc' }}>Registrado</th>
                <th style={{ textAlign: 'left', padding: '8px', border: '1px solid #ccc' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(u => (
                <tr key={u.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '8px', border: '1px solid #eee' }}>{u.nombre} {u.apellido}</td>
                  <td style={{ padding: '8px', border: '1px solid #eee' }}>{u.email}</td>
                  <td style={{ padding: '8px', border: '1px solid #eee' }}>
                    <span style={{
                      background: u.role === 'ADMIN' ? '#c00' : u.role === 'MODERADOR' ? '#c7700a' : '#555',
                      color: '#fff',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 12,
                    }}>{u.role}</span>
                  </td>
                  <td style={{ padding: '8px', border: '1px solid #eee' }}>
                    {new Date(u.createdAt).toLocaleDateString('es-AR')}
                  </td>
                  <td style={{ padding: '8px', border: '1px solid #eee', display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    {u.role === 'USER' && (
                      <button
                        onClick={async () => {
                          if (!confirm(`¿Hacer moderador a ${u.nombre} ${u.apellido}?`)) return
                          try {
                            const res = await actualizarRol(u.id, 'MODERADOR')
                            setUsuarios(prev => prev.map(x => x.id === u.id ? res.data : x))
                          } catch {
                            alert('No se pudo actualizar el rol')
                          }
                        }}
                      >
                        Hacer moderador
                      </button>
                    )}

                    {u.id !== user?.id && (
                    <button
                      style={{ color: '#c00' }}
                      onClick={async () => {
                        if (!confirm(`¿Eliminar la cuenta de ${u.nombre} ${u.apellido}? Esto también eliminará sus publicaciones.`)) return
                        try {
                          await eliminarUsuario(u.id)
                          setUsuarios(prev => prev.filter(x => x.id !== u.id))
                        } catch {
                          alert('No se pudo eliminar el usuario')
                        }
                      }}
                    >
                      Eliminar
                    </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'denuncias' && (
        <div>
          {denuncias.length === 0 && <p>No hay denuncias pendientes.</p>}
          {denuncias.map(d => (
            <div key={d.id} style={{ border: '1px solid #ccc', margin: '1rem 0', padding: '1rem' }}>
              <h4>
                {d.animalCategoria === 'ADOPCION'
                  ? `Adopción: ${d.animalNombre || d.animalTipo}`
                  : `${ETIQUETA_TIPO[d.animalTipo]} (${ETIQUETA_ESTADO[d.animalEstado] || d.animalEstado})`}
              </h4>
              <p>Publicado por: {d.publicadorNombre}</p>
              <p>Denunciado por: {d.denuncianteNombre}</p>
              <p>Razón: <strong>{d.razon.replace(/_/g, ' ')}</strong></p>
              {d.descripcion && <p>Detalle: {d.descripcion}</p>}
              <p style={{ fontSize: 12, color: '#666' }}>Recibida: {new Date(d.creadoEn).toLocaleString('es-AR')}</p>
              <div style={{ display: 'flex', gap: 8, marginTop: '0.5rem' }}>
                <button onClick={async () => {
                  await desestimar(d.id)
                  setDenuncias(prev => prev.filter(x => x.id !== d.id))
                }}>
                  Desestimar
                </button>
                <button
                  style={{ color: '#c00' }}
                  onClick={async () => {
                    if (!confirm('¿Eliminar la publicación por esta denuncia?')) return
                    await eliminarPublicacionDenuncia(d.id)
                    setDenuncias(prev => prev.filter(x => x.animalId !== d.animalId))
                  }}
                >
                  Eliminar publicación
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {tab === 'dashboard' && esAdmin && <AdminDashboard />}
    </div>
  )
}

export default AdminPanel
