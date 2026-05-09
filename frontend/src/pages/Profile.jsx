import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getProfile, updateProfile } from '../api/user'
import { getProvincias, getMunicipios } from '../api/georef'

function Profile() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(null)
  const [editando, setEditando] = useState(false)
  const [form, setForm] = useState({ email: '', tel: '', organizacion: '', provincia: '', ciudad: '' })
  const [provincias, setProvincias] = useState([])
  const [municipios, setMunicipios] = useState([])
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')

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

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default Profile
