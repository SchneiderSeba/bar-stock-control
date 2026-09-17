export type Supplier={id:number;name:string;contactName?:string;email?:string;phone?:string;taxId?:string}
export type Product={id:number;sku:string;name:string;category:string;unit:string;stock:number;minimumStock:number;costPrice:number;sellingPrice:number;supplier?:Supplier;active:boolean}
export type InvoiceItem={id:number;product:Product;quantity:number;unitCost:number;lineTotal:number}
export type Invoice={id:number;invoiceNumber:string;supplier:Supplier;invoiceDate:string;status:'PENDING'|'PAID'|'OVERDUE';notes?:string;total:number;items:InvoiceItem[]}
export type DashboardData={stockValue:number;potentialRevenue:number;potentialProfit:number;purchases:number;productCount:number;lowStockCount:number}

