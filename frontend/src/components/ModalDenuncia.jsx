import { useState } from 'react'
import { denunciarPublicacion } from '../api/denuncia'

const RAZONES = [
  { value: 'INFORMACION_FALSA', label: 'Información falsa o engañosa' },
  { value: 'FOTOS_INAPROPIADAS', label: 'Fotos inapropiadas o que no corresponden' },
  { value: 'MALTRATO_ANIMAL', label: 'Sospecha de maltrato animal' },
  { value: 'PUBLICACION_DUPLICADA', label: 'Publicación duplicada' },
  { value: 'VENTA_DE_ANIMALES', label: 'Intento de venta de animales' },
  { value: 'DATOS_DE_CONTACTO_INVALIDOS', label: 'Datos de contacto inválidos' },
  { value: 'OTRO', label: 'Otro' },
]

function ModalDenuncia({ animalId, onClose, onSuccess }) {
  const [razon, setRazon] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!razon) { setError('Seleccioná una razón'); return }
    if (razon === 'OTRO' && !descripcion.trim()) { setError('Describí el problema cuando elegís "Otro"'); return }
    setError('')
    setEnviando(true)
    try {
      await denunciarPublicacion(animalId, { razon, descripcion: descripcion.trim() || null })
      onSuccess()
    } catch (e) {
      setError(e.response?.data || 'No se pudo enviar la denuncia')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
    }}>
      <div style={{ background: '#fff', color: '#222', padding: '1.5rem', borderRadius: 8, width: 400, maxWidth: '90vw' }}>
        <h3>Reportar publicación</h3>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label>Razón</label>
            <select
              value={razon}
              onChange={e => setRazon(e.target.value)}
              style={{ display: 'block', width: '100%', marginTop: 4 }}
            >
              <option value="">-- Seleccioná una razón --</option>
              {RAZONES.map(r => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>
          {razon && (
            <div style={{ marginBottom: '1rem' }}>
              <label>{razon === 'OTRO' ? 'Descripción *' : 'Información adicional (opcional)'}</label>
              <textarea
                value={descripcion}
                onChange={e => setDescripcion(e.target.value)}
                rows={3}
                style={{ display: 'block', width: '100%', marginTop: 4 }}
                placeholder={razon === 'OTRO' ? 'Describí el problema' : 'Podés agregar más detalles si querés'}
              />
            </div>
          )}
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose}>Cancelar</button>
            <button type="submit" disabled={enviando}>
              {enviando ? 'Enviando...' : 'Enviar denuncia'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default ModalDenuncia
