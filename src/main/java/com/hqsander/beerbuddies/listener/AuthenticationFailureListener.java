package com.hqsander.beerbuddies.listener;

import com.hqsander.beerbuddies.service.TentativaLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureListener {
    private TentativaLoginService tentativaLoginService;

    @Autowired
    public AuthenticationFailureListener(TentativaLoginService tentativaLoginService) {
        this.tentativaLoginService = tentativaLoginService;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) event.getAuthentication().getPrincipal();
            tentativaLoginService.adicionarTentativaNoCache(username);
        }

    }
}
