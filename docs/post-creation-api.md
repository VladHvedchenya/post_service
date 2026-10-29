# Post creation API

This feature adds creation only, not updates, deletion, or notification delivery.

## Request

Start infrastructure and post_service, then run in PowerShell:

```powershell
$body = @{ content = 'My first post'; projectId = $null } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/v1/posts' -Headers @{ 'x-user-id' = '1' } -ContentType 'application/json; charset=utf-8' -Body $body
```

The endpoint returns HTTP 201 and a post DTO. The author is taken from the required
x-user-id header, never from JSON. This is the project's educational identity
mechanism, not production authentication.

Content must not be blank and must contain at most 4096 characters. A project id
is optional and must be positive when present. Missing or invalid author headers
and invalid request bodies return HTTP 400.

A new post is a draft: published=false, deleted=false, with no publication schedule.
Creation and publication are separate actions. Existing scheduled publishing is
unchanged. The API does not verify user or project existence through HTTP; foreign
service validation is outside this minimal creation feature.

## Branch isolation

feature/post-creation-api starts from dev and contains no notification event,
Redis publisher changes, or notification listener. Notifications are connected in
a separate dependent integration branch after both features are available.
