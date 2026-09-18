export type Supplier={id:number;name:string;contactName?:string;email?:string;phone?:string;taxId?:string}
export type User={id:number;name:string;email:string;role:'ADMIN'|'USER'}
export type Product={id:number;sku:string;imageVersion?:string;name:string;category:string;unit:string;kegSizeLitres?:number;stock:number;minimumStock:number;costPrice:number;sellingPrice:number;supplier?:Supplier;supplierSkus:{supplier:Supplier;sku:string}[];active:boolean}
export type InvoiceItem={id:number;product:Product;supplierSku?:string;quantity:number;unitCost:number;lineTotal:number}
export type Invoice={id:number;invoiceNumber:string;supplier:Supplier;invoiceDate:string;status:'PENDING'|'PAID'|'OVERDUE';notes?:string;total:number;items:InvoiceItem[]}
export type DashboardData={stockValue:number;potentialRevenue:number;potentialProfit:number;purchases:number;productCount:number;lowStockCount:number}
