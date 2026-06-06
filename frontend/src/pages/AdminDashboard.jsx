import { useEffect, useState } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LabelList
} from 'recharts'
import { getDashboardStats } from '../api/admin'

const ESPECIE_LABEL = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

// rellena los últimos 12 meses con 0 si no hay datos
function rellenarMeses(datos) {
  const hoy = new Date()
  const resultado = []
  for (let i = 11; i >= 0; i--) {
    const d = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    const encontrado = datos.find(x => x.mes === key)
    resultado.push({ mes: key, cantidad: encontrado ? encontrado.cantidad : 0 })
  }
  return resultado
}

function StatCard({ label, value }) {
  return (
    <div style={{ border: '1px solid #ccc', borderRadius: 6, padding: '1rem 1.5rem', minWidth: 140, textAlign: 'center' }}>
      <div style={{ fontSize: '2rem', fontWeight: 'bold' }}>{value}</div>
      <div style={{ color: '#555', marginTop: 4 }}>{label}</div>
    </div>
  )
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getDashboardStats()
      .then(res => setStats(res.data))
      .catch(() => setError('No se pudieron cargar las estadísticas.'))
  }, [])

  if (error) return <p style={{ color: 'red' }}>{error}</p>
  if (!stats) return <p>Cargando estadísticas...</p>

  const adoptadosPorMes = rellenarMeses(stats.adoptadosPorMes)
  const usuariosPorMes = rellenarMeses(stats.usuariosPorMes)
  const especieData = stats.animalPorEspecie.map(e => ({
    ...e,
    nombre: ESPECIE_LABEL[e.especie] ?? e.especie,
  }))

  const tasaAdopcion = stats.totalHistoricoAdopcion > 0
    ? (stats.totalAdoptados / stats.totalHistoricoAdopcion) * 100 : 0
  const tasaPerdidos = stats.totalHistoricoPerdidos > 0
    ? (stats.resueltosPerdidos / stats.totalHistoricoPerdidos) * 100 : 0
  const tasaEncontrados = stats.totalHistoricoEncontrados > 0
    ? (stats.resueltosEncontrados / stats.totalHistoricoEncontrados) * 100 : 0

  const adopcionData = [{
    name: 'Adopciones',
    total: stats.totalHistoricoAdopcion,
    adoptados: stats.totalAdoptados,
    activos: stats.enAdopcionActivos,
    otros: Math.max(0, stats.totalHistoricoAdopcion - stats.totalAdoptados - stats.enAdopcionActivos),
  }]
  const perdidosData = [{
    name: 'Perdidos',
    total: stats.totalHistoricoPerdidos,
    resueltos: stats.resueltosPerdidos,
    activos: stats.perdidosActivos,
    otros: Math.max(0, stats.totalHistoricoPerdidos - stats.resueltosPerdidos - stats.perdidosActivos),
  }]
  const encontradosData = [{
    name: 'Encontrados',
    total: stats.totalHistoricoEncontrados,
    resueltos: stats.resueltosEncontrados,
    activos: stats.encontradosActivos,
    otros: Math.max(0, stats.totalHistoricoEncontrados - stats.resueltosEncontrados - stats.encontradosActivos),
  }]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>

      {/* publicaciones activas */}
      <section>
        <h3>Publicaciones activas</h3>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <StatCard label="En adopción" value={stats.enAdopcionActivos} />
          <StatCard label="Perdidos" value={stats.perdidosActivos} />
          <StatCard label="Encontrados" value={stats.encontradosActivos} />
        </div>
      </section>

      {/* adopciones */}
      <section>
        <h3>Adopciones</h3>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: '1rem' }}>
          <StatCard label="Total adoptados (histórico)" value={stats.totalAdoptados} />
          <StatCard label="En tránsito (activos)" value={stats.transitoActivos} />
          <StatCard label="Permanente (activos)" value={stats.permanenteActivos} />
        </div>
        <h4 style={{ margin: '0.5rem 0' }}>Adoptados por mes (último año)</h4>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={adoptadosPorMes} margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="mes" tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="cantidad" name="Adoptados" fill="#4a90d9" />
          </BarChart>
        </ResponsiveContainer>
      </section>

      {/* usuarios */}
      <section>
        <h3>Usuarios</h3>
        <div style={{ marginBottom: '1rem' }}>
          <StatCard label="Total registrados" value={stats.totalUsuarios} />
        </div>
        <h4 style={{ margin: '0.5rem 0' }}>Nuevos usuarios por mes (último año)</h4>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={usuariosPorMes} margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="mes" tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="cantidad" name="Nuevos usuarios" fill="#5cb85c" />
          </BarChart>
        </ResponsiveContainer>
      </section>

      {/* por especie */}
      <section>
        <h3>Animales por especie (solo adopciones)</h3>
        <p style={{ color: '#666', marginTop: 0, fontSize: '0.9rem' }}>
          Gris: total histórico · Azul: publicados actualmente (superpuestos)
        </p>
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={especieData} margin={{ top: 4, right: 16, left: 0, bottom: 4 }} barGap={-40}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="nombre" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Legend />
            {/* gris primero → queda por detrás; azul encima */}
            <Bar dataKey="totalHistorico" name="Total histórico" fill="#ccc" barSize={40}>
              <LabelList dataKey="totalHistorico" position="top" style={{ fontSize: 12, fill: '#555' }} />
            </Bar>
            <Bar dataKey="publicadosActuales" name="Publicados ahora" fill="#4a90d9" barSize={40}>
              <LabelList dataKey="publicadosActuales" position="insideTop" style={{ fontSize: 12, fill: '#fff' }} />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </section>

      {/* success rate */}
      <section>
        <h3>Tasa de éxito</h3>
        <p style={{ color: '#666', marginTop: 0, fontSize: '0.9rem' }}>
          Gris: total histórico · las barras de la derecha muestran el desglose (deben sumar igual al total)
        </p>

        {/* Adopciones */}
        <div style={{ marginBottom: '2rem' }}>
          <h4 style={{ marginTop: 0, marginBottom: '0.5rem' }}>Adopciones</h4>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: '1rem' }}>
            <StatCard label="Total publicaciones (histórico)" value={stats.totalHistoricoAdopcion} />
            <StatCard label="Adoptados" value={stats.totalAdoptados} />
            <StatCard label="Tasa de adopción" value={`${tasaAdopcion.toFixed(1)}%`} />
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={adopcionData} barGap={0} margin={{ top: 16, right: 16, left: 0, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" hide />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="total" name="Total histórico" fill="#bbb" barSize={65} />
              <Bar dataKey="adoptados" stackId="s" name="Adoptados" fill="#27ae60" barSize={65} />
              <Bar dataKey="activos" stackId="s" name="En adopción" fill="#4a90d9" barSize={65} />
              <Bar dataKey="otros" stackId="s" name="Otros" fill="#f39c12" barSize={65} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Perdidos */}
        <div style={{ marginBottom: '2rem' }}>
          <h4 style={{ marginTop: 0, marginBottom: '0.5rem' }}>Perdidos</h4>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: '1rem' }}>
            <StatCard label="Total perdidos (histórico)" value={stats.totalHistoricoPerdidos} />
            <StatCard label="Resueltos" value={stats.resueltosPerdidos} />
            <StatCard label="Tasa de resolución" value={`${tasaPerdidos.toFixed(1)}%`} />
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={perdidosData} barGap={0} margin={{ top: 16, right: 16, left: 0, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" hide />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="total" name="Total histórico" fill="#bbb" barSize={65} />
              <Bar dataKey="resueltos" stackId="s" name="Resueltos" fill="#27ae60" barSize={65} />
              <Bar dataKey="activos" stackId="s" name="Activos" fill="#4a90d9" barSize={65} />
              <Bar dataKey="otros" stackId="s" name="Otros" fill="#f39c12" barSize={65} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Encontrados */}
        <div style={{ marginBottom: '2rem' }}>
          <h4 style={{ marginTop: 0, marginBottom: '0.5rem' }}>Encontrados</h4>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: '1rem' }}>
            <StatCard label="Total encontrados (histórico)" value={stats.totalHistoricoEncontrados} />
            <StatCard label="Resueltos" value={stats.resueltosEncontrados} />
            <StatCard label="Tasa de resolución" value={`${tasaEncontrados.toFixed(1)}%`} />
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={encontradosData} barGap={0} margin={{ top: 16, right: 16, left: 0, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" hide />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="total" name="Total histórico" fill="#bbb" barSize={65} />
              <Bar dataKey="resueltos" stackId="s" name="Resueltos" fill="#27ae60" barSize={65} />
              <Bar dataKey="activos" stackId="s" name="Activos" fill="#4a90d9" barSize={65} />
              <Bar dataKey="otros" stackId="s" name="Otros" fill="#f39c12" barSize={65} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Eliminados */}
        <div>
          <h4 style={{ marginTop: 0, marginBottom: '0.5rem' }}>Eliminados</h4>
          <StatCard label="Total publicaciones eliminadas" value={stats.totalEliminados} />
        </div>
      </section>

    </div>
  )
}
