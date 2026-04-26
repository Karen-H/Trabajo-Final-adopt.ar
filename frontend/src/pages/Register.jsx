import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../api/auth'

function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    nombre: '',
    apellido: '',
    dni: '',
    email: '',
    tel: '',
    pass: '',
    organizacion: '',
  })
  const [error, setError] = useState('')

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const data = { ...form, dni: Number(form.dni) }
      if (!data.organizacion) delete data.organizacion
      const res = await register(data)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('nombre', res.data.nombre)
      navigate('/')
    } catch (err) {
      const status = err.response?.status
      if (status === 409) {
        setError('Ya existe una cuenta con ese email o DNI.')
      } else if (status === 400) {

        const violations = err.response?.data?.errors || err.response?.data
        if (Array.isArray(violations) && violations.length > 0) {
          setError(violations[0].defaultMessage || violations[0])
        } else if (typeof violations === 'string') {
          setError(violations)
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

  return (
    <div>
      <h2>Registrarse</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Nombre</label><br />
          <input name="nombre" value={form.nombre} onChange={handleChange} required />
        </div>
        <div>
          <label>Apellido</label><br />
          <input name="apellido" value={form.apellido} onChange={handleChange} required />
        </div>
        <div>
          <label>DNI</label><br />
          <input type="number" name="dni" value={form.dni} onChange={handleChange} required />
        </div>
        <div>
          <label>Email</label><br />
          <input type="email" name="email" value={form.email} onChange={handleChange} required />
        </div>
        <div>
          <label>Teléfono</label><br />
          <input name="tel" value={form.tel} onChange={handleChange} required />
        </div>
        <div>
          <label>Contraseña</label><br />
          <input type="password" name="pass" value={form.pass} onChange={handleChange} required minLength={6} />
        </div>
        <div>
          <label>Organización (opcional)</label><br />
          <input name="organizacion" value={form.organizacion} onChange={handleChange} />
        </div>
        {error && <p>{error}</p>}
        <button type="submit">Registrarse</button>
      </form>
      <p>¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link></p>
    </div>
  )
}

export default Register
