import type {DashboardData,Invoice,Product,Supplier,User} from './types'
async function request<T>(path:string,options:RequestInit={}):Promise<T>{
 const headers=new Headers(options.headers)
 if(options.body && !(options.body instanceof FormData)) headers.set('Content-Type','application/json')
 if(options.method && !['GET','HEAD'].includes(options.method)){
  const response=await fetch('/api/auth/csrf',{credentials:'same-origin'})
  if(!response.ok) throw new Error('No se pudo preparar el formulario. Intenta de nuevo.')
  const csrf=await response.json();headers.set(csrf.headerName,csrf.token)
 }
 const response=await fetch(`/api${path}`,{...options,headers,credentials:'same-origin'})
 if(!response.ok){
  if(response.status===401 && !path.startsWith('/auth/')) window.dispatchEvent(new Event('session-expired'))
  const error=await response.json().catch(()=>({}))
  throw new Error(error.message || (response.status===400?'Revisa los datos del formulario.':response.status===413?'La imagen debe pesar como máximo 2 MB.':'No se pudo completar la operación.'))
 }
 return response.json()
}
const post=(body:unknown):RequestInit=>({method:'POST',body:JSON.stringify(body)})
export const auth={me:()=>request<User>('/auth/me'),login:(email:string,password:string)=>request<User>('/auth/login',post({email,password})),register:(name:string,email:string,password:string)=>request<User>('/auth/register',post({name,email,password})),logout:()=>request('/auth/logout',{method:'POST'}),password:(currentPassword:string,newPassword:string)=>request('/auth/password',post({currentPassword,newPassword})),users:()=>request<User[]>('/admin/users')}
export const api={dashboard:()=>request<DashboardData>('/dashboard'),products:()=>request<Product[]>('/products'),suppliers:()=>request<Supplier[]>('/suppliers'),invoices:()=>request<Invoice[]>('/invoices'),createProduct:(body:unknown)=>request<Product>('/products',post(body)),updateProduct:(id:number,body:unknown)=>request<Product>(`/products/${id}`,{method:'PUT',body:JSON.stringify(body)}),uploadImage:(id:number,file:File)=>{const body=new FormData();body.append('file',file);return request<Product>(`/products/${id}/image`,{method:'POST',body})},deleteImage:(id:number)=>request<Product>(`/products/${id}/image`,{method:'DELETE'}),createSupplier:(body:unknown)=>request<Supplier>('/suppliers',post(body)),createInvoice:(body:unknown)=>request<Invoice>('/invoices',post(body)),adjustStock:(id:number,body:unknown)=>request<Product>(`/products/${id}/adjust`,post(body))}
export const productImage=(product:Product)=>`/api/products/${product.id}/image?v=${product.imageVersion}`
