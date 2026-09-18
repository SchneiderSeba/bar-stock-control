import {Link} from 'react-router-dom'
export default function ProductLink({id,name}:{id:number;name:string}){
 return <Link className="product-link" to={`/products/${id}`}>{name}</Link>
}
