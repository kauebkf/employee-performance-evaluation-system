$reviews = Get-Content test-reviews.json -Raw | ConvertFrom-Json
foreach ($review in $reviews) {
    $json = $review | ConvertTo-Json -Compress
    $json | docker exec -i employee-performance-evaluation-system-kafka-1 kafka-console-producer --bootstrap-server kafka:9092 --topic performance-reviews
    Write-Host "Sent review for employee: $($review.employeeId)"
    Start-Sleep -Seconds 1
}
