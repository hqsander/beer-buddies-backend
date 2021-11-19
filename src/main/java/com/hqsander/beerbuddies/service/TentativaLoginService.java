package com.hqsander.beerbuddies.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MINUTES;

@Service
public class TentativaLoginService {
    private static final int NUMERO_TENTATIVAS_PERMITIDAS = 5;
    private static final int INCREMENTO_TENTATIVA = 1;
    private LoadingCache<String, Integer> cacheTentativaLogin;

    public TentativaLoginService() {
        super();
        cacheTentativaLogin = CacheBuilder.newBuilder().expireAfterWrite(15, MINUTES)
                .maximumSize(100).build(new CacheLoader<String, Integer>() {
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void removerUsuarioDoCache(String username) {
        cacheTentativaLogin.invalidate(username);
    }

    public void adicionarTentativaNoCache(String username) {
        int tentativas = 0;
        try {
            tentativas = INCREMENTO_TENTATIVA + cacheTentativaLogin.get(username);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        cacheTentativaLogin.put(username, tentativas);
    }

    public boolean excedeuTentativasDeLogin(String username) {
        try {
            return cacheTentativaLogin.get(username) >= NUMERO_TENTATIVAS_PERMITIDAS;
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
