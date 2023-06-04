package ru.RSOI.Gateway.Error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class EUnauthorized extends RuntimeException {
    public String message;

    public EUnauthorized(String message)
    {
        this.message = message;
    }
}
