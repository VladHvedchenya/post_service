# Created-post notification integration

## Feature branches

All branches are local until explicitly pushed.

- feature/post-creation-api: API and validation only, based on dev.
- feature/producer-creation-for-notification-service: Redis event publisher and tests only, based on dev.
- feature/post-created-notification: dependent integration branch combining the two branches above and adding after-commit publication.
- notification_service / feature/notification-handler-event: consumer, message builder and email delivery only. No post CRUD changes belong in that repository.

Merge the two independent post_service features into dev first. The integration
PR can then target dev without duplicating their changes. No existing branch
needs force-push, reset, or history rewriting. The repositories are Git submodules;
updating their pointers in the parent repository is a separate change.

## Runtime

POST /api/v1/posts saves a draft and publishes an internal Spring PostCreatedEvent
inside the transaction. AFTER_COMMIT forwards that immutable event to Redis channel
post.created only when the database transaction has committed. On rollback, there
is no Redis publication. The internal event is not itself a Redis message.

The callback runs synchronously on the request thread under the default Spring
event configuration; AFTER_COMMIT does not create a background task.

Redis Pub/Sub delivery is best-effort. A process crash after commit, unavailable
Redis, or an offline subscriber can lose the notification. A publication failure
is logged without undoing the saved post or reporting a failed create to the
client: retrying creation could create a duplicate post. No automatic retry,
outbox, deduplication, or delivery guarantee is claimed.

## Manual verification

Start infrastructure, user_service, notification_service (with MAIL_USERNAME and
MAIL_PASSWORD), then post_service from the integration branch. Run only one
notification_service instance to avoid duplicate delivery through Pub/Sub fan-out.
The chosen user's email must be your own and their preference must be EMAIL.

```powershell
$body = @{ content = 'Created through API'; projectId = $null } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/v1/posts' -Headers @{ 'x-user-id' = '1' } -ContentType 'application/json; charset=utf-8' -Body $body
```

Expect HTTP 201 with a generated id, a row in post, and an email containing that
same id. This checks creation, not publication: published remains false.
Do not send credentials in requests, commit them, or include them in screenshots.

Automated tests use disposable PostgreSQL and Redis containers and mocked SMTP.
They do not send email to a real recipient or use the shared development database.
