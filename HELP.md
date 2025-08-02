- RocketMQ Setting
Please setup brokerIP1 with local IP not `localhost` in `broker.conf` before starting the docker

- Create Notification
```
curl --location 'localhost:8080/notifications' \
--header 'Content-Type: application/json' \
--data-raw '{
  "type": "email",
  "recipient": "user@example.com",
  "subject": "Welcome!",
  "content": "Thanks for signing up!"
}'
```

- Update Notification
```
curl --location --request PUT 'localhost:8080/notifications/1' \
--header 'Content-Type: application/json' \
--data '{
  "subject": "Updated subject line",
  "content": "Updated content of the notification"
}'
```

- Get Recent Notification
```
curl --location 'localhost:8080/notifications/recent'
```

- Get Specific Notification
```
curl --location 'localhost:8080/notifications/3'
```

- Delete Specific Notification
```
curl --location --request DELETE 'localhost:8080/notifications/1'
```