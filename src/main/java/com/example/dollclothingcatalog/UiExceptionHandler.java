package com.example.dollclothingcatalog;

import org.springframework.http.HttpStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class UiExceptionHandler {
	@ExceptionHandler({ResponseStatusException.class, EmptyResultDataAccessException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String notFound(Exception exception, Model model) {
		model.addAttribute("pageTitle", "ページが見つかりません");
		model.addAttribute("message", "指定された情報は見つかりませんでした。");
		return "error";
	}
}
