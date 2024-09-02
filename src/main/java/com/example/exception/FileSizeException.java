package com.example.exception;

public class FileSizeException extends RuntimeException {
	public FileSizeException(String message) {
		super(message);
	}
}
