import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getProfile, updateProfile } from '../api/user'

function Profile() {
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState(null)
  const [editando, setEditando] = useState(false)
  const [form, setForm] = useState({ email: '', tel: '', organizacion: '' })
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
        })
      })
      .catch(() => navigate('/login'))
  }, [navigate])

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
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
      })
      setEditando(false)
      setExito('Perfil actualizado.')
    } catch (err) {
      const status = err.response?.status
      if (status === 409) {
        setError('Ya existe una cuenta con ese email.')
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
        <p><strong>Miembro desde:</strong> {new Date(perfil.createdAt).toLocaleDateString('es-AR')}</p>
      </div>

      {!editando ? (
        <div>
          <p><strong>Email:</strong> {perfil.email}</p>
          <p><strong>Teléfono:</strong> {perfil.tel}</p>
          <p><strong>Organización:</strong> {perfil.organizacion || '-'}</p>
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
