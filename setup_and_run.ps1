# setup_and_run.ps1
# Script to compile and run CineBook Movie Booking System using local Java installation

$JavacPath = "C:\Users\Sruth\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\javac.exe"
$JavaPath = "C:\Users\Sruth\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe"
$Classpath = ".;lib/sqlite-jdbc-3.34.0.jar;lib/flatlaf-3.4.1.jar"

Write-Host "=============================================" -ForegroundColor Green
Write-Host "          CINEBOOK COMPILATION               " -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# Verify jars exist
if (-not (Test-Path "lib/sqlite-jdbc-3.34.0.jar") -or -not (Test-Path "lib/flatlaf-3.4.1.jar")) {
    Write-Host "[Error] Required jar dependencies are missing! Please check the download task." -ForegroundColor Red
    Exit 1
}

# Compile all java files
Write-Host "[Compiling] Compiling source files using javac..." -ForegroundColor Yellow
& $JavacPath -cp $Classpath Movie.java Ticket.java RegularTicket.java VIPTicket.java DatabaseHelper.java MovieBookingSystem.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "[Success] Compilation successful." -ForegroundColor Green
    Write-Host "[Running] Launching CineBook GUI Application..." -ForegroundColor Yellow
    & $JavaPath -cp $Classpath MovieBookingSystem
} else {
    Write-Host "[Error] Compilation failed!" -ForegroundColor Red
    Exit 1
}
