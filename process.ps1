Get-ChildItem -Path ".\verilog-gen" -Recurse -Include *.v |
ForEach-Object {
    (Get-Content $_.FullName) -replace '_(aw|ar|w|r|b)_(bits_)?', '_$1' | Set-Content $_.FullName
}