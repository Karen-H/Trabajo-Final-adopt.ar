import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../api/auth'

function Login() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', pass: '' })
  const [error, setError] = useState('')

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const res = await login(form.email, form.pass)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('nombre', res.data.nombre)
      localStorage.setItem('role', res.data.role)
      localStorage.setItem('activeProfile', res.data.activeProfile)
      navigate('/')
    } catch (err) {
      if (err.response?.status === 401) {
        setError('Email o contraseña incorrectos.')
      } else {
        setError('Ocurrió un error. Intentá de nuevo.')
      }
    }
  }

  return (
    <div>
      <h2>Iniciar sesión</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Email</label><br />
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
          />
        </div>
        <div>
          <label>Contraseña</label><br />
          <input
            type="password"
            name="pass"
            value={form.pass}
            onChange={handleChange}
            required
          />
        </div>
        {error && <p>{error}</p>}
        <button type="submit">Ingresar</button>
      </form>
      <p>¿No tenés cuenta? <Link to="/register">Registrate</Link></p>
    </div>
  )
}

export default Login
