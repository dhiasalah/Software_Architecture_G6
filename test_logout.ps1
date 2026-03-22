$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8080/api"
$timestamp = Get-Date -Format "HHmmss"
$testUser = "logout_test_$timestamp"

Write-Host "--- 1. Register User ---"
$regRes = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -ContentType "application/json" -Body "{`"username`":`"$testUser`",`"email`":`"logout_$timestamp@test.com`",`"password`":`"pass`",`"roleType`":`"USER`"}"
Write-Host "User registered: $testUser"

Write-Host "`n--- 2. Login User ---"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body "{`"username`":`"$testUser`",`"password`":`"pass`"}"
$token = $loginRes.token
Write-Host "Token received."

$headers = @{ Authorization = "Bearer $token" }

Write-Host "`n--- 3. Test Access BEFORE Logout (Should Succeed) ---"
try {
    $r = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get -Headers $headers
    Write-Host "✅ SUCCESS: Can read users"
} catch {
    Write-Host "❌ FAILED: $($_.Exception.Message)"
}

Write-Host "`n--- 4. Perform LOGOUT ---"
try {
    $logoutRes = Invoke-RestMethod -Uri "$baseUrl/auth/logout" -Method Post -Headers $headers
    Write-Host "✅ SUCCESS: $($logoutRes.message)"
} catch {
    Write-Host "❌ FAILED: $($_.Exception.Message)"
}

Write-Host "`n--- 5. Test Access AFTER Logout (Should Fail) ---"
try {
    $r2 = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get -Headers $headers
    Write-Host "❌ SUCCESS: This should have failed, token is still active!"
} catch {
    $status = $_.Exception.Response.StatusCode
    Write-Host "✅ BLOCKED as expected ($status): Token was revoked."
}
