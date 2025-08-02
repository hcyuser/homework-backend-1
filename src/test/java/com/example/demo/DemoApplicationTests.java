package com.example.demo;

import com.example.demo.entity.Notification;
import com.example.demo.model.NotificationRequest;
import com.example.demo.model.NotificationUpdateRequest;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private NotificationRepository notificationRepository;

	private String baseUrl;

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port + "/notifications";
		notificationRepository.deleteAll(); // Clean up before each test
	}

	@Test
	void contextLoads() {
		assertNotNull(restTemplate);
	}

	@Test
	void testCreateNotification() {
		NotificationRequest request = new NotificationRequest("email", "user@example.com", "Hello", "Welcome!");
		ResponseEntity<Notification> response = restTemplate.postForEntity(baseUrl, request, Notification.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("email", response.getBody().getType());
	}
	
	@Test
	void testCreateNotificationWrongType() {
		NotificationRequest request = new NotificationRequest("email2", "user@example.com", "Hello", "Welcome!");
		ResponseEntity<Notification> response = restTemplate.postForEntity(baseUrl, request, Notification.class);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void testGetById_whenExists() {
		Notification n = new Notification();
		n.setType("sms");
		n.setRecipient("123456789");
		n.setSubject("OTP");
		n.setContent("Your code is 1234");
		Notification saved = notificationRepository.save(n);

		ResponseEntity<Notification> response = restTemplate.getForEntity(baseUrl + "/" + saved.getId(), Notification.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("sms", response.getBody().getType());
	}
	
	@Test
	void testGetById_whenNotExists() {

		ResponseEntity<Notification> response = restTemplate.getForEntity(baseUrl + "/" + 0, Notification.class);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void testUpdateNotification() {
		Notification n = new Notification();
		n.setType("email");
		n.setRecipient("update@test.com");
		n.setSubject("Old Subject");
		n.setContent("Old Content");
		Notification saved = notificationRepository.save(n);

		NotificationUpdateRequest updateRequest = new NotificationUpdateRequest("New Subject", "New Content");
		HttpEntity<NotificationUpdateRequest> requestEntity = new HttpEntity<>(updateRequest);

		ResponseEntity<Notification> response = restTemplate.exchange(
				baseUrl + "/" + saved.getId(),
				HttpMethod.PUT,
				requestEntity,
				Notification.class
		);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("New Subject", response.getBody().getSubject());
	}
	
	@Test
	void testUpdateNotificationWithWrongId() {

		NotificationUpdateRequest updateRequest = new NotificationUpdateRequest("New Subject", "New Content");
		HttpEntity<NotificationUpdateRequest> requestEntity = new HttpEntity<>(updateRequest);

		ResponseEntity<Notification> response = restTemplate.exchange(
				baseUrl + "/" + 0,
				HttpMethod.PUT,
				requestEntity,
				Notification.class
		);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void testDeleteNotification() {
		Notification n = new Notification();
		n.setType("email");
		n.setRecipient("delete@test.com");
		n.setSubject("Delete Me");
		n.setContent("Should be gone soon");
		Notification saved = notificationRepository.save(n);

		ResponseEntity<Void> response = restTemplate.exchange(
				baseUrl + "/" + saved.getId(),
				HttpMethod.DELETE,
				null,
				Void.class
		);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		assertFalse(notificationRepository.findById(saved.getId()).isPresent());
	}
	
	@Test
	void testDeleteNotificationWithWrongId() {

		ResponseEntity<Void> response = restTemplate.exchange(
				baseUrl + "/" + 0,
				HttpMethod.DELETE,
				null,
				Void.class
		);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}
}
