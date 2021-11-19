package com.hqsander.beerbuddies.controller;

import com.hqsander.beerbuddies.entity.HttpResponse;
import com.hqsander.beerbuddies.entity.UserEntity;
import com.hqsander.beerbuddies.entity.UserPrincipal;
import com.hqsander.beerbuddies.exception.ExceptionHandling;
import com.hqsander.beerbuddies.exception.custom.*;
import com.hqsander.beerbuddies.service.UserService;
import com.hqsander.beerbuddies.utility.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static com.hqsander.beerbuddies.constant.ArquivoConstant.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping("/user")
public class UserController extends ExceptionHandling {
    public static final String CABECALHO_JWT_TOKEN = "Jwt-Token";
    public static final String EMAIL_ENVIADO = "Nova senha enviada para o email: ";
    public static final String USER_EXCLUIDO = "User excluído com sucesso";

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserController(AuthenticationManager authenticationManager, UserService userService, JWTTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<UserEntity> login(@RequestBody UserEntity user) {
        authenticate(user.getUsername(), user.getPassword());
        UserEntity loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, OK);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<UserEntity> cadastrar(@RequestBody UserEntity user) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException {
        UserEntity newUser = userService.cadastrar(user.getNome(), user.getSobrenome(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/criar")
    public ResponseEntity<UserEntity> criarNovoUser(@RequestParam("nome") String nome,
                                                    @RequestParam("sobrenome") String sobrenome,
                                                    @RequestParam("username") String username,
                                                    @RequestParam("email") String email,
                                                    @RequestParam("role") String role,
                                                    @RequestParam("isEnabled") String isEnabled,
                                                    @RequestParam("isAccountNonLocked") String isAccountNonLocked,
                                                    @RequestParam(value = "avatar", required = false) MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        UserEntity user = userService.criarUser(nome, sobrenome, username, email, role, Boolean.parseBoolean(isAccountNonLocked), Boolean.parseBoolean(isEnabled), avatar);
        return new ResponseEntity<>(user, OK);
    }

    @PostMapping("/atualizar")
    public ResponseEntity<UserEntity> atualizar(@RequestParam("currentUsername") String currentUsername,
                                                @RequestParam("nome") String nome,
                                                @RequestParam("sobrenome") String sobrenome,
                                                @RequestParam("username") String username,
                                                @RequestParam("email") String email,
                                                @RequestParam("role") String role,
                                                @RequestParam("isEnabled") String isEnabled,
                                                @RequestParam("isAccountNonLocked") String isAccountNonLocked,
                                                @RequestParam(value = "avatar", required = false) MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        UserEntity user = userService.atualizarUser(currentUsername, nome, sobrenome, username, email, role, Boolean.parseBoolean(isAccountNonLocked), Boolean.parseBoolean(isEnabled), avatar);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/buscar/{username}")
    public ResponseEntity<UserEntity> buscarUser(@PathVariable("username") String username) {
        UserEntity user = userService.findUserByUsername(username);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/listar")
    public ResponseEntity<List<UserEntity>> listarUsers() {
        List<UserEntity> users = userService.listarUsers();
        return new ResponseEntity<>(users, OK);
    }

    @GetMapping("/redefinirSenha/{email}")
    public ResponseEntity<HttpResponse> redefinirSenha(@PathVariable("email") String email) throws MessagingException, EmailNotFoundException {
        userService.redefinirSenha(email);
        return response(OK, EMAIL_ENVIADO + email);
    }

    @DeleteMapping("/excluir/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> excluirUser(@PathVariable("username") String username) throws IOException {
        userService.excluirUser(username);
        return response(OK, USER_EXCLUIDO);
    }

    @PostMapping("/atualizarAvatar")
    public ResponseEntity<UserEntity> atualizarAvatar(@RequestParam("username") String username, @RequestParam(value = "avatar") MultipartFile avatar) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        UserEntity user = userService.atualizarAvatar(username, avatar);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping(path = "/avatar/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] buscarAvatar(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + username + BARRA + fileName));
    }

    @GetMapping(path = "/avatarProvisorio/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] obterAvatarProvisorio(@PathVariable("username") String username) throws IOException {
        URL url = new URL(IMAGEM_TEMPORARIA_BASE_URL + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while ((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(new Date(), httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(),
                message), httpStatus);
    }

    private HttpHeaders getJwtHeader(UserPrincipal user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CABECALHO_JWT_TOKEN, jwtTokenProvider.generateJwtToken(user));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}