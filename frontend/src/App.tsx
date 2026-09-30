import {useEffect,useState} from 'react'
import {Navigate,NavLink,Route,Routes,useLocation} from 'react-router-dom'
import {api} from './api'
import {AuthGate,Account,Users} from './Auth'
import Dashboard from './Dashboard'
import Stock from './Stock'
import Products from './Products'
import Invoices from './Invoices'
import Suppliers from './Suppliers'
import SalesReports from './SalesReports'
import ProductDetails from './ProductDetails'
import type {DashboardData,Invoice,Product,Supplier,User} from './types'
import {oppositeTheme,resolveTheme,THEME_STORAGE_KEY,type Theme} from './theme'

const icons={dashboard:'◫',stock:'▦',products:'▧',suppliers:'♙',invoices:'▤','sales-reports':'▥'}

export default function App(){
 const [theme,setTheme]=useState<Theme>(()=>resolveTheme(localStorage.getItem(THEME_STORAGE_KEY),window.matchMedia('(prefers-color-scheme: dark)').matches))
 useEffect(()=>{document.documentElement.dataset.theme=theme;document.documentElement.style.colorScheme=theme;localStorage.setItem(THEME_STORAGE_KEY,theme)},[theme])
 return <AuthGate>{(user,onLogout)=><Inventory key={user.id} user={user} onLogout={onLogout} theme={theme} onToggleTheme={()=>setTheme(oppositeTheme(theme))}/>}</AuthGate>
}
function Inventory({user,onLogout,theme,onToggleTheme}:{user:User;onLogout:()=>void;theme:Theme;onToggleTheme:()=>void}){
 const location=useLocation()
 const [products,setProducts]=useState<Product[]>([]),[suppliers,setSuppliers]=useState<Supplier[]>([]),[invoices,setInvoices]=useState<Invoice[]>([]),[dashboard,setDashboard]=useState<DashboardData>(),[error,setError]=useState(''),[loading,setLoading]=useState(true)
 const reload=async()=>{setLoading(true);setError('');try{const [p,s,i,d]=await Promise.all([api.products(),api.suppliers(),api.invoices(),api.dashboard()]);setProducts(p);setSuppliers(s);setInvoices(i);setDashboard(d)}catch(e){setError(e instanceof Error?e.message:'Unable to load data')}finally{setLoading(false)}}
 useEffect(()=>{reload()},[])
 const title=location.pathname.startsWith('/products/')?'Detalle del producto':location.pathname==='/sales-reports'?'Reportes de ventas':location.pathname==='/products'?'Productos':location.pathname==='/users'?'Usuarios':location.pathname==='/stock'?'Stock control':location.pathname==='/suppliers'?'Suppliers':location.pathname==='/invoices'?'Supplier invoices':'Dashboard'
 return <div className="app-shell"><aside><div className="brand"><span className="brand-mark">B</span><div><strong>BarStock</strong><small>Inventory studio</small></div></div><nav>{Object.entries(user.role==='ADMIN'?{...icons,users:'♙'}:icons).map(([key,icon])=><NavLink key={key} to={key==='dashboard'?'/':`/${key}`} end={key==='dashboard'}><span>{icon}</span>{{dashboard:'Dashboard',stock:'Stock',products:'Productos',suppliers:'Proveedores',invoices:'Facturas','sales-reports':'Reportes de ventas',users:'Usuarios'}[key as 'dashboard'|'stock'|'products'|'suppliers'|'invoices'|'sales-reports'|'users']}</NavLink>)}</nav><div className="sidebar-foot"><div className="pulse"/><div><strong>System online</strong><small>Spring API connected</small></div></div></aside><main><header><div><p>OPERATIONS / {title.toUpperCase()}</p><h1>{title}</h1></div><div className="header-actions"><button className="theme-toggle" type="button" onClick={onToggleTheme} aria-label={theme==='dark'?'Activar modo claro':'Activar modo oscuro'} aria-pressed={theme==='dark'}><span aria-hidden="true">{theme==='dark'?'☀':'☾'}</span><span>{theme==='dark'?'Modo claro':'Modo oscuro'}</span></button><Account user={user} onLogout={onLogout}/></div></header>{error&&<div className="error">{error}</div>}{loading?<div className="loading">Refreshing the bar…</div>:!dashboard?<button className="primary" onClick={reload}>Reintentar</button>:<Routes>{user.role==='ADMIN'&&<Route path="/users" element={<Users/>}/>}<Route path="/" element={<Dashboard data={dashboard} products={products}/>}/><Route path="/products/:id" element={<ProductDetails products={products} suppliers={suppliers} invoices={invoices} reload={reload}/>}/><Route path="/products" element={<Products products={products} suppliers={suppliers} reload={reload}/>}/><Route path="/stock" element={<Stock products={products} reload={reload}/>}/><Route path="/suppliers" element={<Suppliers suppliers={suppliers} reload={reload}/>}/><Route path="/sales-reports" element={<SalesReports reload={reload}/>}/><Route path="/invoices" element={<Invoices invoices={invoices} products={products} suppliers={suppliers} reload={reload}/>}/><Route path="*" element={<Navigate to="/" replace/>}/></Routes>}</main></div>
}
