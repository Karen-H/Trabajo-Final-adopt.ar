import MapaAnimales from './MapaAnimales'

function LayoutConMapa({ filtros, animales, children }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '200px 1fr 1fr', gap: 16, alignItems: 'start' }}>
      <aside style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>{filtros}</aside>
      <main>{children}</main>
      <div style={{ position: 'sticky', top: 16, height: 'calc(100vh - 32px)' }}>
        <MapaAnimales animales={animales} />
      </div>
    </div>
  )
}

export default LayoutConMapa
