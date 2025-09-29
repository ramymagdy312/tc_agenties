package de.aerticket.tc_agenties.controller;

import de.aerticket.tc_agenties.model.AuthenticationResponse;
import de.aerticket.tc_agenties.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/aerwebservice/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

	private final AuthenticationService authenticationService;
	private final static String ERRORHTML = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			    <meta charset="UTF-8">
			    <title>Error - Web Services</title>
			    <style>
			        body {
			            background-color: #f8f9fa;
			            font-family: Arial, sans-serif;
			            display: flex;
			            height: 100vh;
			            justify-content: center;
			            align-items: center;
			            margin: 0;
			        }
			        .error-container {
			            background: #fff;
			            padding: 30px 40px;
			            border-radius: 12px;
			            box-shadow: 0 4px 10px rgba(0,0,0,0.15);
			            text-align: center;
			        }
			        .error-title {
			            font-size: 22px;
			            font-weight: bold;
			            color: #dc3545;
			            margin-bottom: 10px;
			        }
			        .error-message {
			            font-size: 16px;
			            color: #333;
			        }
			    </style>
			</head>
			<body>
			    <div class="error-container">
			        <div class="error-title">⚠ Error in Web Services</div>
			        <div class="error-message">Please try again later or contact support.</div>
			    </div>
			</body>
			</html>
			""";

	@GetMapping("/authenticate")
	public ResponseEntity<String> authenticate(@RequestParam("jwt") String jwtToken,
			@RequestParam("lang") String language, @RequestParam("type") String type) {

		log.info("Received authentication request with language: {} and type: {}", language, type);

		try {
			AuthenticationResponse response = authenticationService.authenticateUser(jwtToken, language, type);

			if (response.isSuccess()) {
				log.info("Authentication successful for user: {} {}", response.getAgentFirstName(),
						response.getAgentLastName());
				return ResponseEntity.status(302).header("Location", response.getMicrositeUrl()).build();
			} else {
				log.warn("Authentication failed: {}", response.getMessage());
				return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(ERRORHTML);
			}

		} catch (Exception e) {
			log.error("Error during authentication: {}", e.getMessage());
			AuthenticationResponse errorResponse = AuthenticationResponse.builder().success(false)
					.message("Internal server error: " + e.getMessage()).build();
			return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(ERRORHTML);
		}
	}

	@GetMapping("/authenticatetest")
	public ResponseEntity<AuthenticationResponse> authenticateTest(@RequestParam("jwt") String jwtToken,
			@RequestParam("lang") String language, @RequestParam("type") String type) {
		log.info("Received authentication request with language: {} and type: {}", language, type);
		try {
			AuthenticationResponse response = authenticationService.authenticateUser(jwtToken, language, type);
			if (response.isSuccess()) {
				log.info("Authentication successful for user: {} {}", response.getAgentFirstName(),
						response.getAgentLastName());
				return ResponseEntity.ok(response);
			} else {
				log.warn("Authentication failed: {}", response.getMessage());
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			log.error("Error during authentication: {}", e.getMessage());
			AuthenticationResponse errorResponse = AuthenticationResponse.builder().success(false)
					.message("Internal server error: " + e.getMessage()).build();
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}
}