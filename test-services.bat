@echo off
echo ============================================
echo   TEST COMPLET - Tous les services Docker
echo   (PostgreSQL + RabbitMQ + MailHog + Backend + Nginx)
echo ============================================
echo.

REM =============================================
REM ETAPE 1 : Verifier Docker
REM =============================================
echo [1/6] Verification de Docker...
docker --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERREUR : Docker n'est pas installe ou pas dans le PATH
    pause
    exit /b 1
)
echo OK - Docker est installe

echo.
echo [2/6] Construction et demarrage de TOUS les containers...
echo (Cela peut prendre quelques minutes la premiere fois)
docker-compose up -d --build
if %ERRORLEVEL% neq 0 (
    echo ERREUR : docker-compose a echoue
    pause
    exit /b 1
)

echo.
echo Attente de 30 secondes pour que tous les services demarrent...
timeout /t 30 /nobreak >nul

REM =============================================
REM ETAPE 2 : Verifier que les containers tournent
REM =============================================
echo.
echo [3/6] Verification des containers...
echo.

echo --- Containers actifs ---
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.

REM =============================================
REM ETAPE 3 : Verifier les ports
REM =============================================
echo [4/6] Test des ports...
echo.

echo Test PostgreSQL (port 5432)...
powershell -Command "try { $c = New-Object System.Net.Sockets.TcpClient('localhost', 5432); $c.Close(); Write-Host '  OK - Port 5432 ouvert' } catch { Write-Host '  ERREUR - Port 5432 ferme' }"

echo Test RabbitMQ Management (port 15672)...
curl -s -o nul -w "  HTTP Status: %%{http_code}" http://localhost:15672
echo.

echo Test MailHog Web UI (port 8025)...
curl -s -o nul -w "  HTTP Status: %%{http_code}" http://localhost:8025
echo.

echo Test RabbitMQ AMQP (port 5672)...
powershell -Command "try { $c = New-Object System.Net.Sockets.TcpClient('localhost', 5672); $c.Close(); Write-Host '  OK - Port 5672 ouvert' } catch { Write-Host '  ERREUR - Port 5672 ferme' }"

echo Test MailHog SMTP (port 1025)...
powershell -Command "try { $c = New-Object System.Net.Sockets.TcpClient('localhost', 1025); $c.Close(); Write-Host '  OK - Port 1025 ouvert' } catch { Write-Host '  ERREUR - Port 1025 ferme' }"

echo Test Backend Spring Boot (port 8080)...
curl -s -o nul -w "  HTTP Status: %%{http_code}" http://localhost:8080/api/auth/validate
echo.

echo Test Nginx (port 80)...
curl -s -o nul -w "  HTTP Status: %%{http_code}" http://localhost/api/auth/validate
echo.

REM =============================================
REM ETAPE 4 : Verifier l'API RabbitMQ
REM =============================================
echo.
echo [5/6] Test API RabbitMQ (management)...
curl -s -u guest:guest http://localhost:15672/api/overview 2>nul | findstr "cluster_name" >nul
if %ERRORLEVEL% equ 0 (
    echo   OK - RabbitMQ Management API repond
) else (
    echo   ATTENTION - RabbitMQ Management API ne repond pas encore
)

REM =============================================
REM ETAPE 5 : Test fonctionnel rapide
REM =============================================
echo.
echo [6/6] Test fonctionnel - Inscription...
echo.
curl -s -X POST http://localhost/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"pass123\"}"
echo.

echo.
echo ============================================
echo   RESULTATS
echo ============================================
echo.
echo Tous les services tournent dans Docker !
echo.
echo   PostgreSQL : localhost:5432  (base: project_spring, user: postgres, pass: dhia)
echo   RabbitMQ   : http://localhost:15672  (guest / guest)
echo   MailHog    : http://localhost:8025
echo   Backend    : http://localhost:8080
echo   Swagger    : http://localhost:8080/swagger-ui.html
echo   Nginx      : http://localhost  (port 80 - point d'entree)
echo.
echo Testez avec Swagger ou curl :
echo.
echo   POST http://localhost/api/auth/register
echo   Body: {"username":"testuser","email":"test@example.com","password":"pass123"}
echo.
echo Puis verifiez :
echo   1. L'e-mail dans MailHog  : http://localhost:8025
echo   2. Le lien dans l'e-mail  : copiez-collez dans le navigateur
echo   3. Les queues RabbitMQ    : http://localhost:15672 (onglet Queues)
echo.
echo Commandes utiles :
echo   Voir les logs     : docker-compose logs -f
echo   Logs backend      : docker-compose logs -f backend
echo   Tout arreter      : docker-compose down
echo   Tout reconstruire : docker-compose up -d --build
echo.
pause
