package com.mycompany.backend.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;


/*@Log4j2
@RestController
public class ErrorHandlerController implements ErrorController {
  @RequestMapping("/error")
  public ResponseEntity<String> error(HttpServletRequest request, HttpServletResponse response) {
    int status = response.getStatus();
    log.info(status);
    if(status == 404) {
      //return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create("/")).body("");
      try { request.getRequestDispatcher("/").forward(request, response); } catch(Exception e) {}
      return null;
    } else {
      return ResponseEntity.status(status).body("");
    }
  }
}*/

@Log4j2
@Controller
public class ErrorHandlerController implements ErrorController {
  @RequestMapping("/error")
  public String error(HttpServletRequest request, HttpServletResponse response) {
    int status = response.getStatus();
    log.info(status);
    if(status == 404) {
      return "redirect:/";
      //return "forward:/";
    } else {
      return null;
    }
  }
}