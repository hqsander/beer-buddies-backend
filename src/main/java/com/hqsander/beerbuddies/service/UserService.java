package com.hqsander.beerbuddies.service;

import com.hqsander.beerbuddies.entity.UserEntity;
import com.hqsander.beerbuddies.exception.custom.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {

    UserEntity cadastrar(String nome, String sobrenome, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException;

    List<UserEntity> listarUsers();

    UserEntity findUserByUsername(String username);

    UserEntity findUserByEmail(String email);

    UserEntity criarUser(String nome, String sobrenome, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException;

    UserEntity atualizarUser(String usernameAtual, String nome, String sobrenome, String novoUsername, String novoEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException;

    void excluirUser(String username) throws IOException;

    void redefinirSenha(String email) throws MessagingException, EmailNotFoundException;

    UserEntity atualizarAvatar(String username, MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException;
}
