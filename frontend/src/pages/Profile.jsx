import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getProfile, updateProfile } from '../api/user'
import { getProvincias, getMunicipios } from '../api/georef'
import { getDisponibilidadPropia, agregarDisponibilidad, eliminarDisponibilidad } from '../api/tienda'

function Profile() {
  const navigate = useNavigate()
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
        setError('Ya existe una cuenta con ese email o teléfono.')
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
          <p><strong>Perfil activo:</strong> {perfil.activeProfile}</p>
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

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default Profile
