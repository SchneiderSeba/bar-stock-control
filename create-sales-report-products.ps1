[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",

    [Parameter(Mandatory = $true)]
    [string]$Email
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")
$SecurePassword = Read-Host "Contraseña para $Email" -AsSecureString
$Password = [System.Net.NetworkCredential]::new("", $SecurePassword).Password
$Session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Get-CsrfHeaders {
    $Csrf = Invoke-RestMethod `
        -Method Get `
        -Uri "$BaseUrl/api/auth/csrf" `
        -WebSession $Session

    $Headers = @{}
    $Headers[$Csrf.headerName] = $Csrf.token
    return $Headers
}

try {
    $Headers = Get-CsrfHeaders
    $LoginBody = @{
        email    = $Email
        password = $Password
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/auth/login" `
        -WebSession $Session `
        -Headers $Headers `
        -ContentType "application/json" `
        -Body $LoginBody | Out-Null

    # Refresh the token after the authenticated session has been established.
    $Headers = Get-CsrfHeaders
    $ExistingProducts = @(
        Invoke-RestMethod `
            -Method Get `
            -Uri "$BaseUrl/api/products" `
            -WebSession $Session
    )
    $ExistingSkus = @($ExistingProducts | ForEach-Object { $_.sku })

    # Product creation does not load stock. Initial stock remains at zero and
    # must be entered later through a supplier invoice.
    $Products = @(
        @{
            sku            = "SPIR-002"
            name           = "Baileys"
            category       = "Spirits"
            unit           = "L"
            kegSizeLitres  = $null
            volumeMl       = $null
            minimumStock   = 0
            sellingPrice   = 0
            active         = $true
            supplierSkus   = @()
        },
        @{
            sku            = "BEER-003"
            name           = "Corona"
            category       = "Beer"
            unit           = "bottle"
            kegSizeLitres  = $null
            volumeMl       = 330
            minimumStock   = 0
            sellingPrice   = 0
            active         = $true
            supplierSkus   = @()
        },
        @{
            sku            = "SPIR-003"
            name           = "Jack Daniel's"
            category       = "Spirits"
            unit           = "bottle"
            kegSizeLitres  = $null
            volumeMl       = 700
            minimumStock   = 0
            sellingPrice   = 0
            active         = $true
            supplierSkus   = @()
        },
        @{
            sku            = "BEER-004"
            name           = "Budweiser"
            category       = "Beer"
            unit           = "bottle"
            kegSizeLitres  = $null
            volumeMl       = 330
            minimumStock   = 0
            sellingPrice   = 0
            active         = $true
            supplierSkus   = @()
        }
    )

    foreach ($Product in $Products) {
        if ($ExistingSkus -contains $Product.sku) {
            Write-Host "Omitido: $($Product.sku) ya existe." -ForegroundColor Yellow
            continue
        }

        try {
            $Created = Invoke-RestMethod `
                -Method Post `
                -Uri "$BaseUrl/api/products" `
                -WebSession $Session `
                -Headers $Headers `
                -ContentType "application/json" `
                -Body ($Product | ConvertTo-Json -Depth 5)

            Write-Host "Creado: $($Created.sku) - $($Created.name)" -ForegroundColor Green
            $ExistingSkus += $Created.sku
        }
        catch {
            $Message = $_.ErrorDetails.Message
            if ([string]::IsNullOrWhiteSpace($Message)) {
                $Message = $_.Exception.Message
            }
            Write-Host "Error creando $($Product.sku): $Message" -ForegroundColor Red
        }
    }
}
finally {
    $Password = $null
    $SecurePassword.Dispose()
}
