import ProductLink from './ProductLink'
import {useEffect,useState,type FormEvent} from 'react'
import {api} from './api'
import type {SalesReport} from './types'

const periods={DAILY:'Diario',WEEKLY:'Semanal',MONTHLY:'Mensual'}
const statuses={READY:'Listo para aplicar',REJECTED:'Rechazado',APPLIED:'Aplicado al stock'}
const date=(value:string)=>new Date(value).toLocaleString('es')
const stockDecrease=(value:number)=>new Intl.NumberFormat('es-ES',{maximumFractionDigits:3}).format(value)

export default function SalesReports({reload}:{reload:()=>Promise<void>}){
 const [reports,setReports]=useState<SalesReport[]>([]),[selected,setSelected]=useState<SalesReport>(),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true),[error,setError]=useState(''),[period,setPeriod]=useState<'DAILY'|'WEEKLY'|'MONTHLY'>('DAILY')
 async function load(){setError('');try{setReports(await api.salesReports())}catch(e){setError(e instanceof Error?e.message:'No se pudo cargar el historial.')}finally{setLoading(false)}}
 useEffect(()=>{load()},[])
 async function upload(event:FormEvent<HTMLFormElement>){event.preventDefault();const body=new FormData(event.currentTarget);const file=body.get('file') as File;if(!file||file.size>2*1024*1024){setError('Selecciona un CSV de hasta 2 MB.');return}setBusy(true);setError('');try{const report=await api.uploadSalesReport(body);setSelected(report);setReports(current=>[report,...current.filter(r=>r.id!==report.id)])}catch(e){setError(e instanceof Error?e.message:'No se pudo cargar el reporte.')}finally{setBusy(false)}}
 async function apply(report:SalesReport){setBusy(true);setError('');try{const result=await api.applySalesReport(report.id);setSelected(result);setReports(current=>current.map(r=>r.id===result.id?result:r));await reload()}catch(e){setError(e instanceof Error?e.message:'No se pudo aplicar el reporte.')}finally{setBusy(false)}}
 const latest=reports[0]
 return <>
  <article className="card sales-upload">
   <div className="card-head"><h3>Cargar reporte de ventas</h3><span>CSV · hasta 2 MB</span></div>
   <p className="section-copy">Carga las ventas, revisa los ítems y aplica el descuento al stock. Primero se valida todo el reporte.</p>
   <form className="form-grid" onSubmit={upload}>
    <label>Período<select name="period" value={period} disabled={busy} onChange={e=>setPeriod(e.target.value as typeof period)}><option value="DAILY">Por día</option><option value="WEEKLY">Por semana (7 días)</option><option value="MONTHLY">Por mes</option></select></label>
    <label>{period==='MONTHLY'?'Fecha del mes':'Fecha de inicio'}<input name="startDate" type="date" required disabled={busy} defaultValue={new Date().toISOString().slice(0,10)}/></label>
    <label className="wide">Archivo CSV<input name="file" type="file" accept=".csv,text/csv" required disabled={busy}/></label>
    <p className="wide form-note">Columnas: <code>SKU</code>, <code>nombre del ítem</code>, <code>cantidad vendida</code> y <code>mililitros vendidos</code>. En productos con formato botella, la cantidad vendida se descuenta directamente como botellas. Para los demás formatos, los mililitros son el total de esa fila y no se multiplican por la cantidad. Los ingredientes de cócteles se descuentan por sus propios SKU. Se aceptan SKU internos o del proveedor; para códigos ambiguos usa el interno. UTF-8; coma, punto y coma o tabulación.</p>
    <details className="wide"><summary>Ejemplo de CSV</summary><pre>{'SKU,nombre del ítem,cantidad vendida,mililitros vendidos\nBEER-001,Guinness,10,5000\nSPIR-001,Jameson,3,150'}</pre><p>Guinness descuenta 5 L. Jameson descuenta 3 botellas. Se agrupan todas las filas del mismo ítem. Los períodos de reportes aplicados no pueden superponerse.</p></details>
    <div className="wide form-actions"><button className="primary" disabled={busy}>{busy?'Procesando…':'Subir y revisar reporte'}</button></div>
   </form>
  </article>
  {error&&<div className="error" role="alert">{error}</div>}
  {loading?<p className="loading">Cargando reportes…</p>:<>
   {latest&&<article className="card latest-report"><div className="card-head"><h3>Último reporte subido</h3><span>{statuses[latest.status]}</span></div><strong>{latest.fileName}</strong><p>{periods[latest.period]} · {latest.startDate} — {latest.endDate}</p><small>Subido: {date(latest.uploadedAt)} · {latest.rowCount} filas · {latest.productCount} productos</small><button className="ghost" onClick={()=>setSelected(latest)}>Ver reporte</button></article>}
   <div className="table-wrap"><table><thead><tr><th>Archivo</th><th>Período</th><th>Fecha de carga</th><th>Estado</th><th>Productos</th><th>Detalle</th></tr></thead><tbody>{reports.map(r=><tr key={r.id}><td>{r.fileName}</td><td>{periods[r.period]}<small>{r.startDate} — {r.endDate}</small></td><td>{date(r.uploadedAt)}</td><td><span className={`status ${r.status==='REJECTED'?'low':'good'}`}>{statuses[r.status]}</span></td><td>{r.productCount}</td><td><button className="ghost" onClick={()=>setSelected(r)}>Ver reporte</button></td></tr>)}{!reports.length&&<tr><td colSpan={6} className="empty">Todavía no se cargaron reportes de ventas.</td></tr>}</tbody></table></div>
  </>}
  {selected&&<div className="modal-backdrop"><section className="modal invoice-modal" role="dialog" aria-modal="true" aria-label="Reporte de ventas">
   <div className="modal-head"><h2>{selected.fileName}</h2><button disabled={busy} onClick={()=>setSelected(undefined)}>×</button></div>
   <p><strong>{statuses[selected.status]}</strong> · {selected.startDate} — {selected.endDate}</p>
   {selected.appliedAt&&<p>Aplicado: {date(selected.appliedAt)}</p>}
   {selected.errors.length>0&&<div className="error" role="alert"><strong>No se descontó stock.</strong><ul>{selected.errors.map((message,i)=><li key={i}>{message}</li>)}</ul></div>}
   {selected.status==='READY'&&<p className="form-note">Revisa las cantidades. Aplicar registra las ventas y descuenta todo el reporte en una sola operación.</p>}
   <div className="table-wrap"><table><thead><tr><th>Producto</th><th>SKU reportado</th><th>Cantidad vendida</th><th>Mililitros vendidos</th><th>Descuento de stock</th></tr></thead><tbody>{selected.lines.map((line,index)=><tr key={index}><td><ProductLink id={line.productId} name={line.productName}/></td><td>{line.sku}</td><td>{line.sold}</td><td>{line.soldMl} ml</td><td>{stockDecrease(line.stockDecrease)} {line.stockUnit}</td></tr>)}</tbody></table></div>
   <div className="form-actions"><a className="ghost" href={`/api/sales-reports/${selected.id}/file`}>Descargar CSV original</a><button className="secondary" disabled={busy} onClick={()=>setSelected(undefined)}>Cerrar</button>{selected.status==='READY'&&<button className="primary" disabled={busy} onClick={()=>apply(selected)}>{busy?'Aplicando…':'Aplicar ventas al stock'}</button>}</div>
  </section></div>}
 </>
}
