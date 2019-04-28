package com.gpch.login.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import java.util.concurrent.TimeUnit;

import com.gpch.login.model.User;
import com.gpch.login.service.UserService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;


@Controller
public class LoginController {
	
	private final Counter user_register_count, user_login_count;
	private final Timer user_register_time, user_login_time;
	Logger logger = LoggerFactory.getLogger(LoginController.class);
	LoginController(MeterRegistry meterRegistry) {
		user_register_count = meterRegistry.counter("user.register.count");
		user_login_count = meterRegistry.counter("user.login.count");
		user_register_time = meterRegistry.timer("user.register.time","unit", "Microseconds");
		user_login_time = meterRegistry.timer("user.login.time","unit", "Microseconds");
	    }
	
	@Autowired
	private UserService userService;
	
	@Value("${adminContent}")
	private String adminContent;
	
	@Value("${generatePdf}")
	private String generatePdf;
	
	@Value("${userContent}")
	private String userContent;

	@RequestMapping(value = { "/", "/login" }, method = RequestMethod.GET)
	public ModelAndView login() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("login");
		return modelAndView;
	}

	@RequestMapping(value = "/registration", method = RequestMethod.GET)
	public ModelAndView registration() {
		ModelAndView modelAndView = new ModelAndView();
		User user = new User();
		modelAndView.addObject("user", user);
		modelAndView.setViewName("registration");
		return modelAndView;
	}

	@RequestMapping(value = "/registration", method = RequestMethod.POST)
	public ModelAndView createNewUser(@Valid User user, BindingResult bindingResult) {
		long startTime = System.nanoTime();
		user_register_count.increment();
		ModelAndView modelAndView = new ModelAndView();
		User userExists = userService.findUserByEmail(user.getEmail());
		if (userExists != null) {
			bindingResult.rejectValue("email", "error.user",
					"There is already a user registered with the email provided");
		logger.info("user email="+ user.getEmail()+ " status=duplicate email");
		}
		if (bindingResult.hasErrors()) {
			modelAndView.setViewName("registration");
		} else {
			userService.saveUser(user);
			logger.info("user email="+ user.getEmail()+ " status=success");
			modelAndView.addObject("successMessage", "User has been registered successfully");
			modelAndView.addObject("user", new User());
			modelAndView.setViewName("registration");
		}
		user_register_time.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
		return modelAndView;
	}

	@RequestMapping(value = "/admin/home", method = RequestMethod.GET)
	public ModelAndView adminHome() throws IOException {
		ModelAndView modelAndView = new ModelAndView();
		try {
			long startTime = System.nanoTime();
			user_login_count.increment();
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			User user = userService.findUserByEmail(auth.getName());
			String content = userService.sentContentRequest(adminContent);
			modelAndView.addObject("userName",
					"Welcome " + user.getName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
			modelAndView.addObject("content", content);
			modelAndView.addObject("adminMessage", "Content Available Only for Users with Admin Role");
			modelAndView.setViewName("admin/home");
			logger.info("user id=" + user.getId()+ " role=admin status=logined");
			user_login_time.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
		}catch(ConnectException e) {
			logger.error("request = " + adminContent + ", status=failed, error=" + e.getMessage());
		}
		
		return modelAndView;
	}
	@RequestMapping(value = "/user/home", method = RequestMethod.GET)
	public ModelAndView userHome() throws IOException {
		ModelAndView modelAndView = new ModelAndView();
		try {
			long startTime = System.nanoTime();
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			User user = userService.findUserByEmail(auth.getName());
			String content = userService.sentContentRequest(userContent);
			modelAndView.addObject("userName",
					"Welcome " + user.getName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
			modelAndView.addObject("content", content);
			modelAndView.setViewName("user/home");
			logger.info("user id=" + user.getId()+ " role=user status=logined");
			user_login_count.increment();
			user_login_time.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
		}catch(ConnectException e) {
			logger.error("request = " + userContent + ", status=failed, error=" + e.getMessage());
		}
		
		return modelAndView;
	}
	
	@RequestMapping(value = "/generate/pdf", method = RequestMethod.GET)
	@ResponseBody
	public org.springframework.http.HttpEntity<byte[]> generatePDF() throws IOException, InterruptedException, ExecutionException {
		byte[] document = null; 
		HttpHeaders header = new HttpHeaders();
		try {
			JSONObject json = new JSONObject();
			json.put("users", userService.findAllUsers());
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost request = new HttpPost(generatePdf);
			StringEntity stringEntity = new StringEntity(json.toString());
			request.setEntity(stringEntity);
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			File file = new File("test.pdf");
			if (entity != null) {
		        try (FileOutputStream outstream = new FileOutputStream(file)) {
		            entity.writeTo(outstream);
		        }
		    }
			document = FileCopyUtils.copyToByteArray(file);
			header.setContentType(new MediaType("application", "pdf"));
			header.set("Content-Disposition", "attachment; filename=" + file.getName());
			header.setContentLength(document.length);
		}catch (ConnectException e) {
			logger.error("request = " + generatePdf + ", status=failed, error=" + e.getMessage());
		}
		catch (Exception e) {
			logger.error("pdf generation, status=failed, error=" + e.getMessage());
		}
		
		return new org.springframework.http.HttpEntity<byte[]>(document, header);
	}
}
