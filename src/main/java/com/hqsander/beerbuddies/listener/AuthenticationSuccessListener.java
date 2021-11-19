package com.hqsander.beerbuddies.listener;

import com.hqsander.beerbuddies.entity.UserPrincipal;
import com.hqsander.beerbuddies.service.TentativaLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener {
    private TentativaLoginService tentativaLoginService;

    @Autowired
    public AuthenticationSuccessListener(TentativaLoginService tentativaLoginService) {
        this.tentativaLoginService = tentativaLoginService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            UserPrincipal user = (UserPrincipal) event.getAuthentication().getPrincipal();
            tentativaLoginService.removerUsuarioDoCache(user.getUsername());
        }
    }
}
