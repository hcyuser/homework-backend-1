package com.example.demo.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.rocketmq.spring.core.RocketMQTemplate;

import com.example.demo.model.NotificationRequest;
import com.example.demo.model.NotificationUpdateRequest;
import com.example.demo.entity.Notification;
import com.example.demo.repository.NotificationRepository;

@Service
public class NotificationService {

	private final NotificationRepository repository;
	private final RedisTemplate<String, Notification> redisTemplate;
	private final RocketMQTemplate rocketMQTemplate;
	private final String REDIS_RECENT_NOTIFICATION_LIST = "recent_notifications";
	private final String ROCKET_MQ_TOPIC = "notification-topic";
	private final List<String> notificationType  = Arrays.asList("email", "sms");

	public NotificationService(NotificationRepository repository, RedisTemplate<String, Notification> redisTemplate,
			RocketMQTemplate rocketMQTemplate) {
		this.repository = repository;
		this.redisTemplate = redisTemplate;
		this.rocketMQTemplate = rocketMQTemplate;
	}

	public Notification create(NotificationRequest request) {
		Notification n = new Notification();

		if(notificationType.contains(request.getType())) {
			n.setType(request.getType());
		}else{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		n.setRecipient(request.getRecipient());
		n.setSubject(request.getSubject());
		n.setContent(request.getContent());
		n.setCreatedAt(LocalDateTime.now());

		// Save to MySQL
		Notification saved = repository.save(n);

		// Send to RocketMQ
		rocketMQTemplate.convertAndSend(ROCKET_MQ_TOPIC, saved);

		// Save to Redis (list of recent)
		redisTemplate.opsForList().leftPush(REDIS_RECENT_NOTIFICATION_LIST, saved);
		redisTemplate.opsForList().trim(REDIS_RECENT_NOTIFICATION_LIST, 0, 9);
		// Save to Redis Key Value
		redisTemplate.opsForValue().set(saved.getId().toString(), saved);

		return saved;
	}

	public Notification getById(Long id) {
		Notification cached = redisTemplate.opsForValue().get(id.toString());
		if (cached != null)
			return cached;

		return repository.findById(id).map(n -> {
			redisTemplate.opsForValue().set(id.toString(), n);
			return n;
		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	public List<Notification> getRecent() {
		List<Notification> recent = redisTemplate.opsForList().range(REDIS_RECENT_NOTIFICATION_LIST, 0, 9);
		return recent != null ? recent : List.of();
	}

	public Notification update(Long id, NotificationUpdateRequest req) {
		Notification existing = repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		// Save to MySQL
		existing.setSubject(req.getSubject());
		existing.setContent(req.getContent());
		Notification updated = repository.save(existing);

		Notification cached = redisTemplate.opsForValue().get(id.toString());
		if (cached != null) {
			redisTemplate.opsForValue().set(id.toString(), updated);
		}

		// Update the recent_notifications list in Redis
		List<Notification> recentNotifications = redisTemplate.opsForList().range(REDIS_RECENT_NOTIFICATION_LIST, 0,
				-1);

		if (recentNotifications != null) {
			for (int i = 0; i < recentNotifications.size(); i++) {
				Notification n = recentNotifications.get(i);
				if (n.getId().equals(id)) {
					// Update the item at index i
					redisTemplate.opsForList().set(REDIS_RECENT_NOTIFICATION_LIST, i, updated);
					break;
				}
			}
		}

		return updated;
	}

	public void delete(Long id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		// Delete from MySQL
		repository.deleteById(id);

		// Delete from Redis
		Notification cached = redisTemplate.opsForValue().get(id.toString());
		if (cached != null)
			redisTemplate.delete(id.toString());

		// Update the specific item in recent_notifications list in Redis
		redisTemplate.opsForList().remove(REDIS_RECENT_NOTIFICATION_LIST, 0, cached);

	}
}
