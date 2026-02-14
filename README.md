# Side-project

scp -P 55555 deploy.ps1 chan@192.168.55.170:C:/Users/chan/Desktop/project/
scp -P 55555 build/libs/stockwellness-0.0.1-SNAPSHOT.jar chan@192.168.55.170:C:/Users/chan/Desktop/project/

ssh -p 55555 chan@192.168.55.170:C:/Users/chan/Desktop/project/deploy.ps1


ssh chan@192.168.55.170 "powershell Stop-Process -Name java -Force -ErrorAction SilentlyContinue"

Get-Process java | Select-Object Id, WorkingSet64, StartTime