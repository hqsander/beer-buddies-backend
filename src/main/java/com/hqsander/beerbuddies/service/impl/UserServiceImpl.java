package com.hqsander.beerbuddies.service.impl;

import com.hqsander.beerbuddies.entity.UserEntity;
import com.hqsander.beerbuddies.entity.UserPrincipal;
import com.hqsander.beerbuddies.enumeration.Role;
import com.hqsander.beerbuddies.exception.custom.*;
import com.hqsander.beerbuddies.repository.UserRepository;
import com.hqsander.beerbuddies.service.EmailService;
import com.hqsander.beerbuddies.service.TentativaLoginService;
import com.hqsander.beerbuddies.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.hqsander.beerbuddies.constant.ArquivoConstant.*;
import static com.hqsander.beerbuddies.constant.UserConstant.*;
import static com.hqsander.beerbuddies.enumeration.Role.ROLE_USER;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.MediaType.*;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TentativaLoginService tentativaLoginService;
    private final EmailService emailService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, TentativaLoginService tentativaLoginService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tentativaLoginService = tentativaLoginService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findUserByUsername(username);
        if (user == null) {
            LOGGER.error(USERNAME_NAO_ENCONTRADO + username);
            throw new UsernameNotFoundException(USERNAME_NAO_ENCONTRADO + username);
        } else {
            validarTentativaDeLogin(user);
            user.setDataAcesso(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info(USERNAME_ENCONTRADO + username);
            return userPrincipal;
        }
    }

    @Override
    public UserEntity cadastrar(String nome, String sobrenome, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException {
        validarAlteracaoUsuario(EMPTY, username, email);
        UserEntity user = new UserEntity();
        user.setUserId(generateUserId());
        String password = generatePassword();
        user.setNome(nome);
        user.setSobrenome(sobrenome);
        user.setUsername(username);
        user.setEmail(email);
        user.setDataCriacao(new Date());
        user.setPassword(encodePassword(password));
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setRole(ROLE_USER.name());
        user.setAuthorities(ROLE_USER.getAuthorities());
        user.setUrlAvatar(getUrlAvatarProvisorio(username));
        userRepository.save(user);
        LOGGER.info("Nova senha: " + password);
        // emailService.sendNewPasswordEmail(firstName, password, email);
        return user;
    }

    @Override
    public UserEntity criarUser(String nome, String sobrenome, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        validarAlteracaoUsuario(EMPTY, username, email);
        UserEntity user = new UserEntity();
        String password = generatePassword();
        user.setUserId(generateUserId());
        user.setNome(nome);
        user.setSobrenome(sobrenome);
        user.setDataCriacao(new Date());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodePassword(password));
        user.setEnabled(isActive);
        user.setAccountNonLocked(isNonLocked);
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getAuthorities());
        user.setUrlAvatar(getUrlAvatarProvisorio(username));
        userRepository.save(user);
        salvarImagemAvatar(user, avatar);
        LOGGER.info("Nova senha: " + password);
        return user;
    }

    @Override
    public UserEntity atualizarUser(String usernameAtual, String nome, String sobrenome, String novoUsername, String novoEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        UserEntity currentUser = validarAlteracaoUsuario(usernameAtual, novoUsername, novoEmail);
        currentUser.setNome(nome);
        currentUser.setSobrenome(sobrenome);
        currentUser.setUsername(novoUsername);
        currentUser.setEmail(novoEmail);
        currentUser.setEnabled(isActive);
        currentUser.setAccountNonLocked(isNonLocked);
        currentUser.setRole(getRoleEnumName(role).name());
        currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(currentUser);
        salvarImagemAvatar(currentUser, avatar);
        return currentUser;
    }

    @Override
    public void redefinirSenha(String email) throws EmailNotFoundException {
        UserEntity user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw new EmailNotFoundException(EMAIL_NAO_ENCONTRADO + email);
        }
        String password = generatePassword();
        user.setPassword(encodePassword(password));
        userRepository.save(user);
        LOGGER.info("Nova senha: " + password);
        // emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
    }

    @Override
    public UserEntity atualizarAvatar(String username, MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        UserEntity user = validarAlteracaoUsuario(username, null, null);
        salvarImagemAvatar(user, avatar);
        return user;
    }

    @Override
    public List<UserEntity> listarUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserEntity findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public UserEntity findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Override
    public void excluirUser(String username) throws IOException {
        UserEntity user = userRepository.findUserByUsername(username);
        Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        userRepository.deleteById(user.getId());
    }

    private void salvarImagemAvatar(UserEntity user, MultipartFile avatar) throws IOException, NotAnImageFileException {
        if (avatar != null) {
            if (!Arrays.asList(IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE, IMAGE_GIF_VALUE).contains(avatar.getContentType())) {
                throw new NotAnImageFileException(avatar.getOriginalFilename() + ARQUIVO_INVALIDO_IMAGEM);
            }
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();
            if (!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                LOGGER.info(DIRETORIO_CRIADO + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + PONTO + EXTENSAO_JPG));
            Files.copy(avatar.getInputStream(), userFolder.resolve(user.getUsername() + PONTO + EXTENSAO_JPG), REPLACE_EXISTING);
            user.setUrlAvatar(setUrlAvatar(user.getUsername()));
            userRepository.save(user);
            LOGGER.info(ARQUIVO_FOI_SALVO + avatar.getOriginalFilename());
        }
    }

    private String setUrlAvatar(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(PATH_IMAGEM_USUARIO + username + BARRA
                + username + PONTO + EXTENSAO_JPG).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    private String getUrlAvatarProvisorio(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(PATH_IMAGEM_USUARIO_DEFAULT + username).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(15);
    }

    private void validarTentativaDeLogin(UserEntity user) {
        if (user.isAccountNonLocked()) {
            user.setAccountNonLocked(!tentativaLoginService.excedeuTentativasDeLogin(user.getUsername()));
        } else {
            tentativaLoginService.removerUsuarioDoCache(user.getUsername());
        }
    }

    private UserEntity validarAlteracaoUsuario(String usernameAtual, String novoUsername, String novoEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {
        UserEntity userByNovoUsername = findUserByUsername(novoUsername);
        UserEntity userByNovoEmail = findUserByEmail(novoEmail);
        if (StringUtils.isNotBlank(usernameAtual)) {
            UserEntity userAtual = findUserByUsername(usernameAtual);
            if (userAtual == null) {
                throw new UserNotFoundException(USERNAME_NAO_ENCONTRADO + usernameAtual);
            }
            if (userByNovoUsername != null && !userAtual.getId().equals(userByNovoUsername.getId())) {
                throw new UsernameExistException(USERNAME_INDISPONIVEL);
            }
            if (userByNovoEmail != null && !userAtual.getId().equals(userByNovoEmail.getId())) {
                throw new EmailExistException(EMAIL_JA_CADASTRADO);
            }
            return userAtual;
        } else {
            if (userByNovoUsername != null) {
                throw new UsernameExistException(USERNAME_INDISPONIVEL);
            }
            if (userByNovoEmail != null) {
                throw new EmailExistException(EMAIL_JA_CADASTRADO);
            }
            return null;
        }
    }

}
