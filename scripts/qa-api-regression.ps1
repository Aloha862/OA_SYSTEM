param(
  [string]$BaseUrl = "http://127.0.0.1:8081/api"
)

$ErrorActionPreference = "Stop"

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$results = New-Object System.Collections.Generic.List[object]
$leftovers = New-Object System.Collections.Generic.List[object]

function Add-Result {
  param(
    [string]$Name,
    [bool]$Ok,
    [hashtable]$Extra = @{}
  )
  $entry = [ordered]@{ name = $Name; ok = $Ok }
  foreach ($key in $Extra.Keys) {
    $entry[$key] = $Extra[$key]
  }
  $results.Add([pscustomobject]$entry) | Out-Null
}

function Invoke-OaApi {
  param(
    [string]$Method,
    [string]$Path,
    [string]$Token = "",
    [object]$Body = $null,
    [switch]$Blob
  )

  $headers = @{}
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }

  $uri = "$BaseUrl$Path"
  try {
    if ($null -eq $Body) {
      $response = Invoke-WebRequest -Uri $uri -Method $Method -Headers $headers -UseBasicParsing
    } else {
      $json = $Body | ConvertTo-Json -Depth 12 -Compress
      $response = Invoke-WebRequest -Uri $uri -Method $Method -Headers $headers -ContentType "application/json; charset=utf-8" -Body $json -UseBasicParsing
    }
  } catch {
    $response = $_.Exception.Response
    if ($null -eq $response) {
      return [pscustomobject]@{ status = 0; code = 0; message = $_.Exception.Message; data = $null; raw = $null }
    }
    $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
    $text = $reader.ReadToEnd()
    $reader.Close()
    try {
      $payload = $text | ConvertFrom-Json
    } catch {
      $payload = [pscustomobject]@{ message = $text }
    }
    return [pscustomobject]@{ status = [int]$response.StatusCode; code = $payload.code; message = $payload.message; data = $payload.data; raw = $payload }
  }

  if ($Blob) {
    return [pscustomobject]@{ status = [int]$response.StatusCode; code = [int]$response.StatusCode; message = ""; data = $response.RawContentLength; raw = $response }
  }

  try {
    $payload = $response.Content | ConvertFrom-Json
  } catch {
    $payload = [pscustomobject]@{ code = [int]$response.StatusCode; message = ""; data = $response.Content }
  }
  return [pscustomobject]@{ status = [int]$response.StatusCode; code = $payload.code; message = $payload.message; data = $payload.data; raw = $payload }
}

function Invoke-OaUpload {
  param(
    [string]$Path,
    [string]$Token,
    [string]$FilePath,
    [string]$BusinessType,
    [string]$BusinessId
  )

  Add-Type -AssemblyName System.Net.Http
  $client = New-Object System.Net.Http.HttpClient
  $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $Token)
  $multipart = New-Object System.Net.Http.MultipartFormDataContent
  $stream = $null
  try {
    $stream = [System.IO.File]::OpenRead($FilePath)
    $fileContent = New-Object System.Net.Http.StreamContent($stream)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
    $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
    $multipart.Add((New-Object System.Net.Http.StringContent($BusinessType)), "businessType")
    $multipart.Add((New-Object System.Net.Http.StringContent($BusinessId)), "businessId")
    $response = $client.PostAsync("$BaseUrl$Path", $multipart).Result
    $text = $response.Content.ReadAsStringAsync().Result
    try {
      $payload = $text | ConvertFrom-Json
    } catch {
      $payload = [pscustomobject]@{ code = [int]$response.StatusCode; message = $text; data = $null }
    }
    return [pscustomobject]@{ status = [int]$response.StatusCode; code = $payload.code; message = $payload.message; data = $payload.data; raw = $payload }
  } catch {
    return [pscustomobject]@{ status = 0; code = 0; message = $_.Exception.Message; data = $null; raw = $null }
  } finally {
    if ($stream) { $stream.Dispose() }
    $multipart.Dispose()
    $client.Dispose()
  }
}

function Format-Code {
  param([object]$Response)
  return "$($Response.status)/$($Response.code) $($Response.message)"
}

$tempFile = Join-Path $env:TEMP "oa-qa-upload-$stamp.txt"
Set-Content -LiteralPath $tempFile -Value "QA upload $stamp" -Encoding UTF8

try {
  $adminLogin = Invoke-OaApi -Method Post -Path "/auth/login" -Body @{ username = "admin"; password = "123456" }
  $employeeLogin = Invoke-OaApi -Method Post -Path "/auth/login" -Body @{ username = "employee"; password = "123456" }
  $lisiLogin = Invoke-OaApi -Method Post -Path "/auth/login" -Body @{ username = "lisi"; password = "123456" }
  Add-Result "admin login" ($adminLogin.code -eq 200) @{ result = Format-Code $adminLogin }
  Add-Result "employee login" ($employeeLogin.code -eq 200) @{ result = Format-Code $employeeLogin }
  Add-Result "lisi login" ($lisiLogin.code -eq 200) @{ result = Format-Code $lisiLogin }

  $admin = $adminLogin.data.token
  $employee = $employeeLogin.data.token
  $lisi = $lisiLogin.data.token
  if (-not $admin -or -not $employee -or -not $lisi) {
    throw "login failed"
  }

  $employeePage = Invoke-OaApi -Method Get -Path "/users/page?current=1&size=10&keyword=employee" -Token $admin
  $employeeId = ($employeePage.data.records | Where-Object { $_.username -eq "employee" } | Select-Object -First 1).id
  $lisiPage = Invoke-OaApi -Method Get -Path "/users/page?current=1&size=10&keyword=lisi" -Token $admin
  $lisiId = ($lisiPage.data.records | Where-Object { $_.username -eq "lisi" } | Select-Object -First 1).id
  Add-Result "lookup employee/lisi ids" ([bool]($employeeId -and $lisiId)) @{ employeeId = $employeeId; lisiId = $lisiId }

  $deptName = "QA_TEMP_DEPT_$stamp"
  $deptCreate = Invoke-OaApi -Method Post -Path "/departments" -Token $admin -Body @{ name = $deptName; sortOrder = 998; status = 1 }
  Add-Result "department create" ($deptCreate.code -eq 200) @{ result = Format-Code $deptCreate }
  $deptPage = Invoke-OaApi -Method Get -Path "/departments/page?current=1&size=10&keyword=$deptName" -Token $admin
  $deptId = ($deptPage.data.records | Select-Object -First 1).id
  Add-Result "department keyword lookup" ([bool]$deptId) @{ total = $deptPage.data.total; deptId = $deptId }
  if ($deptId) {
    $deptUpdate = Invoke-OaApi -Method Put -Path "/departments/$deptId" -Token $admin -Body @{ name = "$deptName Updated"; sortOrder = 997; status = 1 }
    Add-Result "department update" ($deptUpdate.code -eq 200) @{ result = Format-Code $deptUpdate }
    $deptLeader = Invoke-OaApi -Method Put -Path "/departments/$deptId/leader" -Token $admin -Body @{ userId = $employeeId }
    Add-Result "department leader body userId" ($deptLeader.code -eq 200) @{ result = Format-Code $deptLeader }
    $deptApprover = Invoke-OaApi -Method Put -Path "/departments/$deptId/approver" -Token $admin -Body @{ userId = $lisiId }
    Add-Result "department approver body userId" ($deptApprover.code -eq 200) @{ result = Format-Code $deptApprover }
    $deptDelete = Invoke-OaApi -Method Delete -Path "/departments/$deptId" -Token $admin
    Add-Result "department delete" ($deptDelete.code -eq 200) @{ result = Format-Code $deptDelete }
    if ($deptDelete.code -ne 200) { $leftovers.Add([pscustomobject]@{ type = "department"; id = $deptId; name = $deptName }) | Out-Null }
  }

  $typeCode = "qa_type_$stamp"
  $typeName = "QA_TEMP_DICT_$stamp"
  $dtCreate = Invoke-OaApi -Method Post -Path "/dict-types" -Token $admin -Body @{ typeCode = $typeCode; typeName = $typeName; remark = "QA temp"; status = 1 }
  Add-Result "dict type create" ($dtCreate.code -eq 200) @{ result = Format-Code $dtCreate }
  $dtPage = Invoke-OaApi -Method Get -Path "/dict-types/page?current=1&size=10&keyword=$typeCode" -Token $admin
  $dictTypeId = ($dtPage.data.records | Select-Object -First 1).id
  Add-Result "dict type keyword lookup" ([bool]$dictTypeId) @{ total = $dtPage.data.total; dictTypeId = $dictTypeId }
  $ddCreate = Invoke-OaApi -Method Post -Path "/dict-data" -Token $admin -Body @{ typeCode = $typeCode; dictLabel = "QA Option A"; dictValue = "qa_a"; sortOrder = 1; status = 1; remark = "QA temp" }
  Add-Result "dict data create" ($ddCreate.code -eq 200) @{ result = Format-Code $ddCreate }
  $ddPage = Invoke-OaApi -Method Get -Path "/dict-data/page?current=1&size=10&keyword=QA%20Option%20A" -Token $admin
  $dictDataId = ($ddPage.data.records | Where-Object { $_.typeCode -eq $typeCode } | Select-Object -First 1).id
  Add-Result "dict data keyword lookup" ([bool]$dictDataId) @{ total = $ddPage.data.total; dictDataId = $dictDataId }
  $byType = Invoke-OaApi -Method Get -Path "/dict-data/type/$typeCode" -Token $admin
  Add-Result "dict data by type" (($byType.code -eq 200) -and ($byType.data.Count -ge 1)) @{ count = $byType.data.Count; result = Format-Code $byType }
  if ($dictDataId) {
    $ddUpdate = Invoke-OaApi -Method Put -Path "/dict-data/$dictDataId" -Token $admin -Body @{ typeCode = $typeCode; dictLabel = "QA Option A Updated"; dictValue = "qa_a"; sortOrder = 2; status = 1; remark = "QA temp updated" }
    Add-Result "dict data update" ($ddUpdate.code -eq 200) @{ result = Format-Code $ddUpdate }
    $ddDelete = Invoke-OaApi -Method Delete -Path "/dict-data/$dictDataId" -Token $admin
    Add-Result "dict data delete" ($ddDelete.code -eq 200) @{ result = Format-Code $ddDelete }
    if ($ddDelete.code -ne 200) { $leftovers.Add([pscustomobject]@{ type = "dict-data"; id = $dictDataId; typeCode = $typeCode }) | Out-Null }
  }
  if ($dictTypeId) {
    $dtUpdate = Invoke-OaApi -Method Put -Path "/dict-types/$dictTypeId" -Token $admin -Body @{ typeCode = $typeCode; typeName = "$typeName Updated"; remark = "QA temp updated"; status = 1 }
    Add-Result "dict type update" ($dtUpdate.code -eq 200) @{ result = Format-Code $dtUpdate }
    $refresh = Invoke-OaApi -Method Post -Path "/dict-data/cache/refresh" -Token $admin
    Add-Result "dict cache refresh" ($refresh.code -eq 200) @{ result = Format-Code $refresh }
    $dtDelete = Invoke-OaApi -Method Delete -Path "/dict-types/$dictTypeId" -Token $admin
    Add-Result "dict type delete" ($dtDelete.code -eq 200) @{ result = Format-Code $dtDelete }
    if ($dtDelete.code -ne 200) { $leftovers.Add([pscustomobject]@{ type = "dict-type"; id = $dictTypeId; typeCode = $typeCode }) | Out-Null }
  }

  $newsTitle = "QA_TEMP_NEWS_$stamp"
  $newsCreate = Invoke-OaApi -Method Post -Path "/news" -Token $admin -Body @{
    title = $newsTitle
    summary = "QA temp news summary"
    content = "<p>QA temp news body.</p>"
    category = "Company"
    coverImage = "/files/2026/06/seed-cover-20.jpg"
    status = "DRAFT"
    isTop = 0
    aiGenerated = 0
  }
  $newsId = $newsCreate.data.id
  Add-Result "news create" (($newsCreate.code -eq 200) -and [bool]$newsId) @{ result = Format-Code $newsCreate; newsId = $newsId }
  if ($newsId) {
    $newsUpdate = Invoke-OaApi -Method Put -Path "/news/$newsId" -Token $admin -Body @{
      title = "$newsTitle Updated"
      summary = "QA updated summary"
      content = "<p>QA updated body.</p>"
      category = "Company"
      coverImage = "/files/2026/06/seed-cover-20.jpg"
      status = "DRAFT"
      isTop = 0
      aiGenerated = 0
    }
    Add-Result "news update" ($newsUpdate.code -eq 200) @{ result = Format-Code $newsUpdate }
    $publish = Invoke-OaApi -Method Post -Path "/news/$newsId/publish" -Token $admin
    Add-Result "news publish" ($publish.code -eq 200) @{ result = Format-Code $publish }
    $detail = Invoke-OaApi -Method Get -Path "/news/$newsId" -Token $employee
    Add-Result "news detail employee view" (($detail.code -eq 200) -and [bool]$detail.data.coverImage) @{ result = Format-Code $detail; cover = $detail.data.coverImage }
    $view = Invoke-OaApi -Method Post -Path "/news/$newsId/view" -Token $employee
    Add-Result "news view count endpoint" ($view.code -eq 200) @{ result = Format-Code $view }
    $comment = Invoke-OaApi -Method Post -Path "/news/$newsId/comments" -Token $employee -Body @{ content = "QA comment $stamp"; parentId = 0 }
    $commentId = $comment.data.id
    Add-Result "news comment create" (($comment.code -eq 200) -and [bool]$commentId) @{ result = Format-Code $comment; commentId = $commentId }
    $comments = Invoke-OaApi -Method Get -Path "/news/$newsId/comments" -Token $employee
    Add-Result "news comments list" (($comments.code -eq 200) -and ($comments.data.Count -ge 1)) @{ count = $comments.data.Count; result = Format-Code $comments }
    $like = Invoke-OaApi -Method Post -Path "/news/$newsId/like" -Token $employee
    Add-Result "news like" ($like.code -eq 200) @{ result = Format-Code $like }
    $unlike = Invoke-OaApi -Method Delete -Path "/news/$newsId/like" -Token $employee
    Add-Result "news unlike" ($unlike.code -eq 200) @{ result = Format-Code $unlike }
    $favorite = Invoke-OaApi -Method Post -Path "/news/$newsId/favorite" -Token $employee
    Add-Result "news favorite" ($favorite.code -eq 200) @{ result = Format-Code $favorite }
    $myFavorites = Invoke-OaApi -Method Get -Path "/news/favorites/my" -Token $employee
    $hasFavorite = [bool]($myFavorites.data | Where-Object { $_.id -eq $newsId } | Select-Object -First 1)
    Add-Result "news my favorites list" (($myFavorites.code -eq 200) -and $hasFavorite) @{ result = Format-Code $myFavorites; count = $myFavorites.data.Count }
    $unfavorite = Invoke-OaApi -Method Delete -Path "/news/$newsId/favorite" -Token $employee
    Add-Result "news unfavorite" ($unfavorite.code -eq 200) @{ result = Format-Code $unfavorite }
    if ($commentId) {
      $delComment = Invoke-OaApi -Method Delete -Path "/news/comments/$commentId" -Token $employee
      Add-Result "news comment delete own" ($delComment.code -eq 200) @{ result = Format-Code $delComment }
    }
    $offline = Invoke-OaApi -Method Post -Path "/news/$newsId/offline" -Token $admin
    Add-Result "news offline" ($offline.code -eq 200) @{ result = Format-Code $offline }
    $newsDelete = Invoke-OaApi -Method Delete -Path "/news/$newsId" -Token $admin
    Add-Result "news delete" ($newsDelete.code -eq 200) @{ result = Format-Code $newsDelete }
    if ($newsDelete.code -ne 200) { $leftovers.Add([pscustomobject]@{ type = "news"; id = $newsId; title = $newsTitle }) | Out-Null }
  }

  $noticeTitle = "QA_TEMP_NOTICE_$stamp"
  $sysNotice = Invoke-OaApi -Method Post -Path "/notifications/system" -Token $admin -Body @{ receiverIds = @($employeeId); title = $noticeTitle; content = "QA notification regression; mail and WebSocket delivery are verified separately."; type = "SYSTEM" }
  Add-Result "notification system send all" ($sysNotice.code -eq 200) @{ result = Format-Code $sysNotice }
  Start-Sleep -Milliseconds 800
  $noticePage = Invoke-OaApi -Method Get -Path "/notifications/page?current=1&size=10&keyword=$noticeTitle" -Token $employee
  $noticeId = ($noticePage.data.records | Select-Object -First 1).id
  Add-Result "notification keyword page" (($noticePage.code -eq 200) -and [bool]$noticeId) @{ total = $noticePage.data.total; noticeId = $noticeId; result = Format-Code $noticePage }
  if ($noticeId) {
    $read = Invoke-OaApi -Method Put -Path "/notifications/$noticeId/read" -Token $employee
    Add-Result "notification read" ($read.code -eq 200) @{ result = Format-Code $read }
    $delOther = Invoke-OaApi -Method Delete -Path "/notifications/$noticeId" -Token $lisi
    Add-Result "notification other user cannot delete" ($delOther.code -eq 403) @{ result = Format-Code $delOther }
    $delOwn = Invoke-OaApi -Method Delete -Path "/notifications/$noticeId" -Token $employee
    Add-Result "notification owner delete" ($delOwn.code -eq 200) @{ result = Format-Code $delOwn }
    if ($delOwn.code -ne 200) { $leftovers.Add([pscustomobject]@{ type = "notification"; id = $noticeId; title = $noticeTitle }) | Out-Null }
  }

  $fileUpload = Invoke-OaUpload -Path "/files/upload" -Token $employee -FilePath $tempFile -BusinessType "QA" -BusinessId "$stamp"
  $fileId = $fileUpload.data.id
  Add-Result "file upload" (($fileUpload.code -eq 200) -and [bool]$fileId) @{ result = Format-Code $fileUpload; fileId = $fileId }
  if ($fileId) {
    $fileDetail = Invoke-OaApi -Method Get -Path "/files/$fileId" -Token $employee
    Add-Result "file detail own" (($fileDetail.code -eq 200) -and ($fileDetail.data.id -eq $fileId)) @{ result = Format-Code $fileDetail }
    $fileBusiness = Invoke-OaApi -Method Get -Path "/files/business?businessType=QA&businessId=$stamp" -Token $employee
    $hasFile = [bool]($fileBusiness.data | Where-Object { $_.id -eq $fileId } | Select-Object -First 1)
    Add-Result "file business lookup" (($fileBusiness.code -eq 200) -and $hasFile) @{ count = $fileBusiness.data.Count; result = Format-Code $fileBusiness }
    $fileDownload = Invoke-OaApi -Method Get -Path "/files/download/$fileId" -Token $employee -Blob
    Add-Result "file download own" (($fileDownload.status -eq 200) -and ($fileDownload.data -gt 0)) @{ status = $fileDownload.status; size = $fileDownload.data }
    $fileDeleteOther = Invoke-OaApi -Method Delete -Path "/files/$fileId" -Token $lisi
    Add-Result "file other user cannot delete" ($fileDeleteOther.code -eq 403) @{ result = Format-Code $fileDeleteOther }
    $fileDeleteOwn = Invoke-OaApi -Method Delete -Path "/files/$fileId" -Token $employee
    Add-Result "file owner delete" ($fileDeleteOwn.code -eq 200) @{ result = Format-Code $fileDeleteOwn }
    if ($fileDeleteOwn.code -ne 200) { $leftovers.Add([pscustomobject]@{ type = "file"; id = $fileId }) | Out-Null }
  }
} catch {
  Add-Result "suite exception" $false @{ error = $_.Exception.Message }
} finally {
  if (Test-Path -LiteralPath $tempFile) {
    Remove-Item -LiteralPath $tempFile -Force
  }
}

[pscustomobject]@{
  stamp = $stamp
  passed = ($results | Where-Object { $_.ok }).Count
  failed = @($results | Where-Object { -not $_.ok })
  results = $results
  leftovers = $leftovers
} | ConvertTo-Json -Depth 12
