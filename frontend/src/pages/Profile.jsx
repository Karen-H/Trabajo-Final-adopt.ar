import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getProfile, updateProfile } from '../api/user'
import { getProvincias, getMunicipios } from '../api/georef'
import { getDisponibilidadPropia, agregarDisponibilidad, eliminarDisponibilidad } from '../api/tienda'
import { configurarDonaciones, getMisDonaciones } from '../api/donacion'
import { useAuth } from '../context/AuthContext'

const ETIQUETA_PREFERENCIA = {
  ADOPTANTE: 'Solo adoptar',
  RESCATISTA: 'Solo publicar',
  AMBOS: 'Adoptar y publicar',
}

function Profile() {
  const navigate = useNavigate()
  const { user, setActiveProfile, setPreferencia, setTieneTienda } = useAuth()
  const [perfil, setPerfil] = useState(null)
  const [editando, setEditando] = useState(false)
  const [form, setForm] = useState({ email: '', tel: '', organizacion: '', provincia: '', ciudad: '' })
  const [provincias, setProvincias] = useState([])
  const [municipios, setMunicipios] = useState([])
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')

  // disponibilidad (solo admin)
  const [disponibilidad, setDisponibilidad] = useState([])
  const [nuevaDisponibilidad, setNuevaDisponibilidad] = useState({ diaSemana: '', horaInicio: '', horaFin: '' })
  const [errorDisp, setErrorDisp] = useState('')

  // preferencia de rol
  const [editandoPreferencia, setEditandoPreferencia] = useState(false)
  const [nuevaPreferencia, setNuevaPreferencia] = useState('')
  const [confirmPref1, setConfirmPref1] = useState(false)
  const [confirmPref2, setConfirmPref2] = useState(false)
  const [errorPref, setErrorPref] = useState('')

  // donaciones (solo USER)
  const [donConfig, setDonConfig] = useState({ aceptaDonaciones: false, descripcionDonacion: '' })
  const [errorDon, setErrorDon] = useState('')
  const [exitoDon, setExitoDon] = useState('')
  const [misDonaciones, setMisDonaciones] = useState([])
  const [cargandoDon, setCargandoDon] = useState(false)

  const HORAS_DISPONIBLES = Array.from({ length: 48 }, (_, i) => {
    const h = String(Math.floor(i / 2)).padStart(2, '0')
    const m = i % 2 === 0 ? '00' : '30'
    return `${h}:${m}`
  })

  const DIAS_SEMANA = [
    { value: 'LUNES', label: 'Lunes' },
    { value: 'MARTES', label: 'Martes' },
    { value: 'MIERCOLES', label: 'Miércoles' },
    { value: 'JUEVES', label: 'Jueves' },
    { value: 'VIERNES', label: 'Viernes' },
    { value: 'SABADO', label: 'Sábado' },
    { value: 'DOMINGO', label: 'Domingo' },
  ]
  const DIA_LABEL = Object.fromEntries(DIAS_SEMANA.map(d => [d.value, d.label]))

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    getProfile()
      .then(res => {
        setPerfil(res.data)
        setForm({
          email: res.data.email || '',
          tel: res.data.tel || '',
          organizacion: res.data.organizacion || '',
          provincia: res.data.provincia || '',
          ciudad: res.data.ciudad || '',
        })
        if (res.data.role === 'ADMIN') {
          getDisponibilidadPropia()
            .then(r => setDisponibilidad(r.data))
            .catch(() => {})
        }
        if (res.data.role === 'USER') {
          setDonConfig({
            aceptaDonaciones: res.data.aceptaDonaciones || false,
            descripcionDonacion: res.data.descripcionDonacion || '',
          })
          getMisDonaciones().then(r => setMisDonaciones(r.data)).catch(() => {})
        }
      })
      .catch(() => navigate('/login'))
    getProvincias().then(setProvincias).catch(() => setProvincias([]))
  }, [navigate])

  // cargar municipios cuando se abre el editor y ya hay provincia guardada
  useEffect(() => {
    if (editando && form.provincia) {
      getMunicipios(form.provincia).then(setMunicipios).catch(() => setMunicipios([]))
    }
  }, [editando])

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleProvinciaChange(e) {
    const provincia = e.target.value
    setForm({ ...form, provincia, ciudad: '' })
    if (provincia) {
      const munis = await getMunicipios(provincia).catch(() => [])
      setMunicipios(munis)
    } else {
      setMunicipios([])
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setExito('')
    try {
      const res = await updateProfile(form)
      setPerfil(res.data)
      setForm({
        email: res.data.email || '',
        tel: res.data.tel || '',
        organizacion: res.data.organizacion || '',
        provincia: res.data.provincia || '',
        ciudad: res.data.ciudad || '',
      })
      setEditando(false)
      setExito('Perfil actualizado.')
    } catch (err) {
      const status = err.response?.status
      if (status === 409) {
        const msg = err.response?.data?.error
        setError(msg || 'Ya existe una cuenta con ese email o teléfono.')
      } else if (status === 400) {
        const violations = err.response?.data?.errors || err.response?.data
        if (Array.isArray(violations) && violations.length > 0) {
          setError(violations[0].defaultMessage || violations[0])
        } else {
          setError('Revisá los datos ingresados.')
        }
      } else if (!err.response) {
        setError('No se pudo conectar con el servidor.')
      } else {
        setError('Ocurrió un error. Intentá de nuevo.')
      }
    }
  }

  async function handleAgregarDisponibilidad() {
    const { diaSemana, horaInicio, horaFin } = nuevaDisponibilidad
    if (!diaSemana || !horaInicio || !horaFin) { setErrorDisp('Completá todos los campos'); return }
    setErrorDisp('')
    try {
      const res = await agregarDisponibilidad({ diaSemana, horaInicio: horaInicio + ':00', horaFin: horaFin + ':00' })
      // res.data es la lista fusionada del día; reemplazar bloques de ese día
      setDisponibilidad(prev => [...prev.filter(d => d.diaSemana !== diaSemana), ...res.data])
      setNuevaDisponibilidad({ diaSemana: '', horaInicio: '', horaFin: '' })
    } catch (e) {
      setErrorDisp(e.response?.data || 'Error al agregar disponibilidad')
    }
  }

  async function handleEliminarDisponibilidad(id) {
    try {
      await eliminarDisponibilidad(id)
      setDisponibilidad(prev => prev.filter(d => d.id !== id))
    } catch {
      setErrorDisp('Error al eliminar la disponibilidad')
    }
  }

  // 'rescatista' = pierde rol rescatista, 'adoptante' = pierde rol adoptante, null = no destructivo
  function tipoCambio(nueva) {
    const vieja = perfil?.preferencia
    if (nueva === 'ADOPTANTE' && vieja !== 'ADOPTANTE') return 'rescatista'
    if (nueva === 'RESCATISTA' && vieja !== 'RESCATISTA') return 'adoptante'
    return null
  }

  async function handleGuardarPreferencia() {
    setErrorPref('')
    try {
      const res = await updateProfile({ preferencia: nuevaPreferencia })
      setPerfil(res.data)
      setPreferencia(res.data.preferencia)
      setActiveProfile(res.data.activeProfile)
      if (res.data.preferencia === 'ADOPTANTE') setTieneTienda(false)
      setEditandoPreferencia(false)
      setNuevaPreferencia('')
      setConfirmPref1(false)
      setConfirmPref2(false)
      setExito('Preferencia actualizada.')
    } catch {
      setErrorPref('No se pudo guardar la preferencia.')
    }
  }

  async function handleGuardarDonaciones(e) {
    e.preventDefault()
    setErrorDon('')
    setExitoDon('')
    setCargandoDon(true)
    try {
      await configurarDonaciones(donConfig)
      setPerfil(prev => ({ ...prev, aceptaDonaciones: donConfig.aceptaDonaciones }))
      setExitoDon('Configuración guardada.')
    } catch (err) {
      setErrorDon(err.response?.data || 'Error al guardar.')
    } finally {
      setCargandoDon(false)
    }
  }

  function cancelar() {
    setEditando(false)
    setError('')
    setExito('')
    setForm({
      email: perfil.email || '',
      tel: perfil.tel || '',
      organizacion: perfil.organizacion || '',
      provincia: perfil.provincia || '',
      ciudad: perfil.ciudad || '',
    })
  }

  if (!perfil) return null

  return (
    <div>
      <h2>Mi perfil</h2>

      <div>
        <p><strong>Nombre:</strong> {perfil.nombre} {perfil.apellido}</p>
        <p><strong>DNI:</strong> {perfil.dni}</p>
        <p><strong>Rol:</strong> {perfil.role}</p>
        {perfil.role === 'USER' && (
          <p><strong>Perfil activo:</strong> {user?.activeProfile}</p>
        )}
        <p><strong>Miembro desde:</strong> {new Date(perfil.createdAt).toLocaleDateString('es-AR')}</p>
      </div>

      {!editando ? (
        <div>
          <p><strong>Email:</strong> {perfil.email}</p>
          <p><strong>Teléfono:</strong> {perfil.tel}</p>
          <p><strong>Organización:</strong> {perfil.organizacion || '-'}</p>
          <p><strong>Provincia:</strong> {perfil.provincia || '-'}</p>
          <p><strong>Ciudad:</strong> {perfil.ciudad || '-'}</p>
          {exito && <p>{exito}</p>}
          <button onClick={() => { setEditando(true); setExito('') }}>Editar</button>
        </div>
      ) : (
        <form onSubmit={handleSubmit}>
          <div>
            <label>Email</label><br />
            <input type="email" name="email" value={form.email} onChange={handleChange} required />
          </div>
          <div>
            <label>Teléfono</label><br />
            <input name="tel" value={form.tel} onChange={handleChange} required />
          </div>
          <div>
            <label>Organización (opcional)</label><br />
            <input name="organizacion" value={form.organizacion} onChange={handleChange} />
          </div>
          <div>
            <label>Provincia</label><br />
            <select name="provincia" value={form.provincia} onChange={handleProvinciaChange}>
              <option value="">Seleccioná una provincia</option>
              {provincias.map(p => (
                <option key={p.id} value={p.nombre}>{p.nombre}</option>
              ))}
            </select>
          </div>
          {municipios.length > 0 && (
            <div>
              <label>Ciudad</label><br />
              <select name="ciudad" value={form.ciudad} onChange={handleChange}>
                <option value="">Seleccioná una ciudad</option>
                {municipios.map(m => (
                  <option key={m.id} value={m.nombre}>{m.nombre}</option>
                ))}
              </select>
            </div>
          )}
          {error && <p>{error}</p>}
          <button type="submit">Guardar</button>
          <button type="button" onClick={cancelar}>Cancelar</button>
        </form>
      )}

      {perfil.role === 'ADMIN' && (
        <div style={{ marginTop: 24 }}>
          <h3>Mi disponibilidad para videollamadas</h3>
          <p>Estos bloques horarios serán los que verán los rescatistas al solicitar una tienda.</p>

          {disponibilidad.length === 0 && <p>No tenés bloques de disponibilidad cargados.</p>}
          {disponibilidad.map(d => (
            <div key={d.id} style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 6 }}>
              <span>{DIA_LABEL[d.diaSemana]} — {d.horaInicio.substring(0, 5)}hs a {d.horaFin.substring(0, 5)}hs</span>
              <button onClick={() => handleEliminarDisponibilidad(d.id)} style={{ color: '#c00' }}>Eliminar</button>
            </div>
          ))}

          <div style={{ marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            <select
              value={nuevaDisponibilidad.diaSemana}
              onChange={e => setNuevaDisponibilidad(prev => ({ ...prev, diaSemana: e.target.value }))}
            >
              <option value="">-- Día --</option>
              {DIAS_SEMANA.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
            </select>
            <select
              value={nuevaDisponibilidad.horaInicio}
              onChange={e => setNuevaDisponibilidad(prev => ({ ...prev, horaInicio: e.target.value, horaFin: '' }))}
            >
              <option value="">-- Inicio --</option>
              {HORAS_DISPONIBLES.map(h => <option key={h} value={h}>{h}</option>)}
            </select>
            <span>a</span>
            <select
              value={nuevaDisponibilidad.horaFin}
              onChange={e => setNuevaDisponibilidad(prev => ({ ...prev, horaFin: e.target.value }))}
              disabled={!nuevaDisponibilidad.horaInicio}
            >
              <option value="">-- Fin --</option>
              {HORAS_DISPONIBLES
                .filter(h => h > nuevaDisponibilidad.horaInicio)
                .map(h => <option key={h} value={h}>{h}</option>)}
            </select>
            <button onClick={handleAgregarDisponibilidad}>Agregar bloque</button>
          </div>
          {errorDisp && <p style={{ color: 'red' }}>{errorDisp}</p>}
        </div>
      )}

      {perfil.role === 'USER' && (
        <div style={{ marginTop: 24 }}>
          <h3>Preferencia de rol</h3>
          {!editandoPreferencia ? (
            <div>
              <p><strong>Rol actual:</strong> {ETIQUETA_PREFERENCIA[perfil.preferencia] || perfil.preferencia}</p>
              <button onClick={() => { setEditandoPreferencia(true); setNuevaPreferencia(perfil.preferencia); setErrorPref('') }}>
                Cambiar preferencia
              </button>
            </div>
          ) : (
            <div>
              <div style={{ marginBottom: 8 }}>
                <label>Nueva preferencia:</label><br />
                <select
                  value={nuevaPreferencia}
                  onChange={e => { setNuevaPreferencia(e.target.value); setConfirmPref1(false); setConfirmPref2(false) }}
                >
                  <option value="ADOPTANTE">Solo adoptar</option>
                  <option value="RESCATISTA">Solo publicar</option>
                  <option value="AMBOS">Adoptar y publicar</option>
                </select>
              </div>

              {tipoCambio(nuevaPreferencia) === 'rescatista' && (
                <div style={{ background: '#fff3cd', border: '1px solid #ffc107', borderRadius: 6, padding: 12, marginBottom: 8 }}>
                  <p style={{ margin: '0 0 8px', fontWeight: 600 }}>⚠️ Atención: esta acción es irreversible</p>
                  <p style={{ margin: '0 0 8px', fontSize: 14 }}>
                    Al cambiar a "Solo adoptar" se eliminarán permanentemente todos tus animales publicados en adopción,
                    se cancelarán sus reservas activas, y se eliminará tu tienda (si tenías una).
                  </p>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 14 }}>
                    <input type="checkbox" checked={confirmPref1} onChange={e => setConfirmPref1(e.target.checked)} style={{ marginRight: 6 }} />
                    Entiendo que mis publicaciones en adopción y mi tienda serán eliminadas permanentemente.
                  </label>
                  <label style={{ display: 'block', fontSize: 14 }}>
                    <input type="checkbox" checked={confirmPref2} onChange={e => setConfirmPref2(e.target.checked)} style={{ marginRight: 6 }} />
                    Confirmo que esta acción no tiene vuelta atrás.
                  </label>
                </div>
              )}

              {tipoCambio(nuevaPreferencia) === 'adoptante' && (
                <div style={{ background: '#fff3cd', border: '1px solid #ffc107', borderRadius: 6, padding: 12, marginBottom: 8 }}>
                  <p style={{ margin: '0 0 8px', fontWeight: 600 }}>⚠️ Atención</p>
                  <p style={{ margin: '0 0 8px', fontSize: 14 }}>
                    Al cambiar a "Solo publicar" se cancelarán automáticamente tus reservas activas como adoptante
                    y no podrás iniciar nuevas adopciones.
                  </p>
                  <label style={{ display: 'block', marginBottom: 6, fontSize: 14 }}>
                    <input type="checkbox" checked={confirmPref1} onChange={e => setConfirmPref1(e.target.checked)} style={{ marginRight: 6 }} />
                    Entiendo que mis reservas de adopción activas serán canceladas.
                  </label>
                  <label style={{ display: 'block', fontSize: 14 }}>
                    <input type="checkbox" checked={confirmPref2} onChange={e => setConfirmPref2(e.target.checked)} style={{ marginRight: 6 }} />
                    Confirmo que acepto estos términos.
                  </label>
                </div>
              )}

              {errorPref && <p style={{ color: 'red' }}>{errorPref}</p>}

              <button
                onClick={handleGuardarPreferencia}
                disabled={tipoCambio(nuevaPreferencia) !== null && (!confirmPref1 || !confirmPref2)}
              >
                Guardar
              </button>
              <button
                type="button"
                style={{ marginLeft: 8 }}
                onClick={() => { setEditandoPreferencia(false); setNuevaPreferencia(''); setConfirmPref1(false); setConfirmPref2(false); setErrorPref('') }}
              >
                Cancelar
              </button>
            </div>
          )}
        </div>
      )}

      {user?.activeProfile === 'RESCATISTA' && (
        <div style={{ marginTop: 32 }}>
          <h3>Donaciones</h3>

          {/* Configuracion de donaciones */}
          <form onSubmit={handleGuardarDonaciones}>
            <div style={{ marginBottom: 8 }}>
              <label>
                <input
                  type="checkbox"
                  checked={donConfig.aceptaDonaciones}
                  onChange={e => setDonConfig(prev => ({ ...prev, aceptaDonaciones: e.target.checked }))}
                  style={{ marginRight: 6 }}
                />
                Quiero aparecer en el listado de donaciones
              </label>
            </div>
            <div style={{ marginBottom: 8 }}>
              <label>Descripción (qué hacés con las donaciones)</label><br />
              <textarea
                value={donConfig.descripcionDonacion}
                onChange={e => setDonConfig(prev => ({ ...prev, descripcionDonacion: e.target.value }))}
                rows={3}
                style={{ width: '100%', maxWidth: 500, boxSizing: 'border-box', marginTop: 4 }}
                placeholder="Ej: Rescato perros callejeros en Córdoba y pago sus veterinarios..."
              />
            </div>
            {errorDon && <p style={{ color: 'red' }}>{errorDon}</p>}
            {exitoDon && <p>{exitoDon}</p>}
            <button type="submit" disabled={cargandoDon}>Guardar configuración</button>
          </form>

          {/* Historial */}
          {misDonaciones.length > 0 && (
            <div style={{ marginTop: 24 }}>
              <h4>Donaciones recibidas</h4>
              <table style={{ borderCollapse: 'collapse', width: '100%', maxWidth: 600 }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', padding: '4px 8px', borderBottom: '1px solid #ccc' }}>Donante</th>
                    <th style={{ textAlign: 'left', padding: '4px 8px', borderBottom: '1px solid #ccc' }}>Monto</th>
                    <th style={{ textAlign: 'left', padding: '4px 8px', borderBottom: '1px solid #ccc' }}>Estado</th>
                    <th style={{ textAlign: 'left', padding: '4px 8px', borderBottom: '1px solid #ccc' }}>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {misDonaciones.map(d => (
                    <tr key={d.id}>
                      <td style={{ padding: '4px 8px' }}>{d.donanteNombre}</td>
                      <td style={{ padding: '4px 8px' }}>${Number(d.monto).toLocaleString('es-AR')}</td>
                      <td style={{ padding: '4px 8px' }}>{d.estado}</td>
                      <td style={{ padding: '4px 8px' }}>{new Date(d.creadoEn).toLocaleDateString('es-AR')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default Profile
