package com.example.demo.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
    private final String REDIS_KEY = "recent_notifications";

    public NotificationService(NotificationRepository repository, RedisTemplate<String, Notification> redisTemplate, RocketMQTemplate rocketMQTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public Notification create(NotificationRequest request) {
        Notification n = new Notification();
        n.setType(request.getType());
        n.setRecipient(request.getRecipient());
        n.setSubject(request.getSubject());
        n.setContent(request.getContent());
        n.setCreatedAt(LocalDateTime.now());

        Notification saved = repository.save(n);

        // Send to RocketMQ
		rocketMQTemplate.convertAndSend("notification-topic", saved);

        // Save to Redis (list of recent)
        redisTemplate.opsForList().leftPush(REDIS_KEY, saved);
        redisTemplate.opsForList().trim(REDIS_KEY, 0, 9);

        return saved;
    }

    public Notification getById(Long id) {
        String redisKey = "notification:" + id;
        Notification cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) return cached;

        return repository.findById(id)
                .map(n -> {
                    redisTemplate.opsForValue().set(redisKey, n);
                    return n;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<Notification> getRecent() {
        List<Notification> recent = redisTemplate.opsForList().range(REDIS_KEY, 0, 9);
        return recent != null ? recent : List.of();
    }

    public Notification update(Long id, NotificationUpdateRequest req) {
        Notification existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        existing.setSubject(req.getSubject());
        existing.setContent(req.getContent());
        Notification updated = repository.save(existing);

        String redisKey = "notification:" + id;
        redisTemplate.delete(redisKey);

        return updated;
    }

    public void delete(Long id) {
    	if(!repository.existsById(id)) {
    		throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    	}

        repository.deleteById(id);
        redisTemplate.delete("notification:" + id);
    }
}
