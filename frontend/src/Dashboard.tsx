import {useState,type ReactNode} from 'react'
import {Bar,BarChart,CartesianGrid,Cell,ResponsiveContainer,Tooltip,XAxis,YAxis} from 'recharts'
import ProductLink from './ProductLink'
import {pricedQuantity,stockUnit} from './inventory'
import type {DashboardData,DashboardPeriod,MonthMetrics,Product} from './types'

const euro=new Intl.NumberFormat('en-IE',{style:'currency',currency:'EUR'})
const compactEuro=new Intl.NumberFormat('es-ES',{style:'currency',currency:'EUR',notation:'compact',maximumFractionDigits:1})
const periodNames:Record<DashboardPeriod,string>={DAY:'Día',WEEK:'Semana',MONTH:'Mes'}

function localDate(value:string){return new Date(`${value}T00:00:00`)}
function periodLabel(metrics:MonthMetrics,period:DashboardPeriod){
 const start=localDate(metrics.startDate),end=localDate(metrics.endDate)
 if(period==='DAY')return new Intl.DateTimeFormat('es-ES',{day:'numeric',month:'short'}).format(start)
 if(period==='MONTH')return new Intl.DateTimeFormat('es-ES',{month:'long',year:'numeric'}).format(start)
 const short=new Intl.DateTimeFormat('es-ES',{day:'numeric',month:'short'})
 return `${short.format(start)} – ${short.format(end)}`
}
function change(current:number,previous:number){
 if(previous===0)return current===0?{label:'Sin actividad',value:0}:{label:'Nuevo período con actividad',value:100}
 const value=(current-previous)/Math.abs(previous)*100
 return {label:`${value>=0?'+':''}${value.toFixed(1)}%`,value}
}
function ComparisonChart({title,description,metric,previous,current,period,color}:{title:string;description:string;metric:'sales'|'purchases';previous:MonthMetrics;current:MonthMetrics;period:DashboardPeriod;color:string}){
 const delta=change(current[metric],previous[metric])
 const data=[{name:periodLabel(previous,period),value:previous[metric],period:'Anterior'},{name:periodLabel(current,period),value:current[metric],period:'Actual'}]
 return <article className="period-chart-card">
  <div className="period-chart-head"><div><span>{title}</span><h3>{euro.format(current[metric])}</h3><small>{description}</small></div><strong className={delta.value>=0?'positive':'negative'}>{delta.label}</strong></div>
  <div className="period-chart-canvas">
   <ResponsiveContainer width="100%" height="100%">
    <BarChart data={data} margin={{top:12,right:8,left:0,bottom:4}} accessibilityLayer>
     <CartesianGrid stroke="#e4e9e1" strokeDasharray="4 4" vertical={false}/>
     <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill:'#667269',fontSize:11}}/>
     <YAxis axisLine={false} tickLine={false} width={58} tick={{fill:'#839087',fontSize:10}} tickFormatter={value=>compactEuro.format(Number(value))}/>
     <Tooltip cursor={{fill:'#edf2e9'}} formatter={value=>[euro.format(Number(value)),title]} labelFormatter={label=>String(label)}/>
     <Bar dataKey="value" name={title} radius={[9,9,3,3]} maxBarSize={86}>{data.map((entry,index)=><Cell key={entry.period} fill={index===0?'#c4cec2':color}/>)}</Bar>
    </BarChart>
   </ResponsiveContainer>
  </div>
  <div className="period-chart-values">{data.map((item,index)=><div key={item.period}><span><i style={{background:index===0?'#c4cec2':color}}/>{item.period}</span><b>{euro.format(item.value)}</b></div>)}</div>
 </article>
}

function PeriodCharts({data}:{data:DashboardData}){
 const [period,setPeriod]=useState<DashboardPeriod>('MONTH')
 const {previous,current}=data.comparisons[period]
 return <section className="comparison-overview">
  <div className="comparison-head"><div><p className="eyebrow">RENDIMIENTO COMPARATIVO</p><h2>Ventas y compras</h2><p>{period==='DAY'?'Hoy frente a ayer':period==='WEEK'?'Semana actual frente a la semana anterior':'Mes actual frente al mes pasado'}, usando reportes aplicados y facturas recibidas.</p></div><div className="period-selector" role="group" aria-label="Período del dashboard">{(['DAY','WEEK','MONTH'] as DashboardPeriod[]).map(value=><button type="button" key={value} className={period===value?'active':''} aria-pressed={period===value} onClick={()=>setPeriod(value)}>{periodNames[value]}</button>)}</div></div>
  <div className="comparison-context"><div><span>Período anterior</span><strong>{periodLabel(previous,period)}</strong></div><div><span>Período actual</span><strong>{periodLabel(current,period)}</strong></div><div><span>Cobertura actual</span><strong>{current.appliedReportCount} reporte{current.appliedReportCount===1?'':'s'}</strong></div></div>
  <div className="period-chart-grid"><ComparisonChart title="Ventas" description="Unidades vendidas × precio registrado" metric="sales" previous={previous} current={current} period={period} color="#4f7cff"/><ComparisonChart title="Compras" description="Importe total de facturas de stock" metric="purchases" previous={previous} current={current} period={period} color="#e68a52"/></div>
  <p className="comparison-method">Los reportes que abarcan varios días se distribuyen proporcionalmente para calcular las vistas diaria, semanal y mensual.</p>
 </section>
}

export default function Dashboard({data,products}:{data:DashboardData;products:Product[]}){
 const low=products.filter(p=>p.stock<=p.minimumStock)
 const margin=data.potentialRevenue?data.potentialProfit/data.potentialRevenue*100:0
 return <><PeriodCharts data={data}/><section className="metrics"><Metric label="Valor del stock" value={euro.format(data.stockValue)} note="al último costo de compra"/><Metric label="Ingresos potenciales" value={euro.format(data.potentialRevenue)} note="del stock actual"/><Metric label="Beneficio potencial" value={euro.format(data.potentialProfit)} note={`${margin.toFixed(1)}% de margen bruto`} accent/><Metric label="Stock bajo" value={String(data.lowStockCount)} note={`${data.productCount} productos en total`} warn={data.lowStockCount>0}/></section><section className="grid-two"><Card title="Necesitan atención" action={`${low.length} ítems`}><div className="attention-list">{low.length?low.map(p=><div key={p.id}><span className="product-icon">{p.category.slice(0,1)}</span><div><strong><ProductLink id={p.id} name={p.name}/></strong><small>SKU {p.sku} · mínimo {p.minimumStock} {stockUnit(p)}</small></div><b>{p.stock} {stockUnit(p)}</b></div>):<p className="empty">El stock está en niveles adecuados.</p>}</div></Card><Card title="Composición del inventario" action="Por valor"><div className="mix">{products.map((p,i)=>{const max=Math.max(0,...products.map(x=>pricedQuantity(x)*x.costPrice));const val=pricedQuantity(p)*p.costPrice;return <div key={p.id}><p><span><ProductLink id={p.id} name={p.name}/><small className="dashboard-stock">{p.stock} {stockUnit(p)} disponibles</small></span><b>{euro.format(val)}</b></p><i><u style={{width:`${max?val/max*100:0}%`,background:['#d9ff62','#9bd7c0','#f2aa7e','#86a8ff'][i%4]}}/></i></div>})}</div></Card></section></>
}
function Metric({label,value,note,accent,warn}:{label:string;value:string;note:string;accent?:boolean;warn?:boolean}){return <article className={`metric ${accent?'accent':''} ${warn?'warn':''}`}><p>{label}</p><strong>{value}</strong><small>{note}</small></article>}
function Card({title,action,children}:{title:string;action:string;children:ReactNode}){return <article className="card"><div className="card-head"><h3>{title}</h3><span>{action}</span></div>{children}</article>}
