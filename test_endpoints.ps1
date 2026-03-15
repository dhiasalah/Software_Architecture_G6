$ErrorActionPreference = "Continue" # So it doesn't stop on 400 Bad Request
$baseUrl = "http://localhost:8080/api"

$timestamp = Get-Date -Format "HHmmss"
$adminUser = "admin_$timestamp"
$regularUser = "user_$timestamp"

Write-Host "--- 1. Register ADMIN ---"
$adminReg = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -ContentType "application/json" -Body "{`"username`":`"$adminUser`",`"email`":`"admin_$timestamp@test.com`",`"password`":`"pass`",`"roleType`":`"ADMIN`"}"
Write-Host "Admin registered: $adminUser"

Write-Host "`n--- 2. Register USER ---"
$userReg = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -ContentType "application/json" -Body "{`"username`":`"$regularUser`",`"email`":`"user_$timestamp@test.com`",`"password`":`"pass`",`"roleType`":`"USER`"}"
Write-Host "User registered: $regularUser"

Write-Host "`n--- 3. Login ADMIN ---"
$adminLogin = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body "{`"username`":`"$adminUser`",`"password`":`"pass`"}"
$adminToken = $adminLogin.token
Write-Host "Admin token received."

Write-Host "`n--- 4. Login USER ---"
$userLogin = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body "{`"username`":`"$regularUser`",`"password`":`"pass`"}"
$userToken = $userLogin.token
Write-Host "User token received."

Write-Host "`n--- ADMIN TESTS (Should all SUCCEED) ---"
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

# Read All
$users = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get -Headers $adminHeaders
Write-Host "[GET] /users      : SUCCESS (Has: USER_READ)"

# Create
$newUser = Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body "{
    `"username`": `"testuser_temp_$timestamp`",
    `"role`": { `"name`": `"USER`" },
    `"credentials`": { `"email`": `"temp_$timestamp@test.com`", `"password`": `"pass`" }
}"
$newId = $newUser.id
Write-Host "[POST] /users     : SUCCESS (Has: USER_CREATE)"

# Update
$updatedUser = Invoke-RestMethod -Uri "$baseUrl/users/$newId" -Method Put -Headers $adminHeaders -ContentType "application/json" -Body "{
    `"username`": `"testuser_temp_updated_$timestamp`",
    `"role`": { `"name`": `"USER`" },
    `"credentials`": { `"email`": `"temp2_$timestamp@test.com`" }
}"
Write-Host "[PUT] /users/{id} : SUCCESS (Has: USER_UPDATE)"

# Delete
Invoke-RestMethod -Uri "$baseUrl/users/$newId" -Method Delete -Headers $adminHeaders
Write-Host "[DEL] /users/{id} : SUCCESS (Has: USER_DELETE)"


Write-Host "`n--- USER TESTS (Mixed outcomes based on perm) ---"
$userHeaders = @{ Authorization = "Bearer $userToken" }

# Read All (Should Succeed - USER_READ permission)
try {
    $readResult = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get -Headers $userHeaders
    Write-Host "[GET] /users      : SUCCESS (Has: USER_READ)"
} catch {
    Write-Host "[GET] /users      : FAILED"
}

# Create (Should Fail - missing USER_CREATE)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -Headers $userHeaders -ContentType "application/json" -Body "{
        `"username`": `"should_fail_$timestamp`",
        `"role`": { `"name`": `"USER`" },
        `"credentials`": { `"email`": `"fail_$timestamp@test.com`", `"password`": `"pass`" }
    }"
    Write-Host "[POST] /users     : SUCCESS (This is a bug, should fail!)"
} catch {
    Write-Host "[POST] /users     : BLOCKED (Missing: USER_CREATE)"
}

# Delete (Should Fail - missing USER_DELETE)
try {
    $regId = $userReg.id
    $null = Invoke-RestMethod -Uri "$baseUrl/users/$regId" -Method Delete -Headers $userHeaders
    Write-Host "[DEL] /users/{id} : SUCCESS (This is a bug, should fail!)"
} catch {
    Write-Host "[DEL] /users/{id} : BLOCKED (Missing: USER_DELETE)"
}
