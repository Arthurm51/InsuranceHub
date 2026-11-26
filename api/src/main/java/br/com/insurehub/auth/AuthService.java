package br.com.insurehub.auth;

import br.com.insurehub.audit.AuditAction;
import br.com.insurehub.audit.AuditService;
import br.com.insurehub.auth.AuthDtos.LoginRequest;
import br.com.insurehub.auth.AuthDtos.RegisterRequest;
import br.com.insurehub.auth.AuthDtos.TokenResponse;
import br.com.insurehub.common.ApiException;
import br.com.insurehub.config.AppProperties;
import br.com.insurehub.security.TokenService;
import br.com.insurehub.user.Role;
import br.com.insurehub.user.User;
import br.com.insurehub.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final int MIN_AGE = 18;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditService audit;
    private final AppProperties props;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
                       AuditService audit, AppProperties props, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public User register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (Period.between(req.birthDate(), LocalDate.now(clock)).getYears() < MIN_AGE) {
            throw ApiException.unprocessable("UNDERAGE_CUSTOMER", "Cliente deve ter ao menos 18 anos");
        }
        if (users.existsByCpf(req.cpf())) {
            throw ApiException.conflict("CPF_ALREADY_REGISTERED", "CPF já cadastrado");
        }
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_REGISTERED", "E-mail já cadastrado");
        }
        User user = users.save(new User(req.name().trim(), req.cpf(), email,
                passwordEncoder.encode(req.password()), req.birthDate(), Role.CUSTOMER, clock.instant()));
        audit.record(user.getId(), AuditAction.USER_REGISTERED, "USER", user.getId(), null);
        return user;
    }

    /**
     * noRollbackFor: a tentativa falha (contador/bloqueio + auditoria) precisa ser persistida
     * mesmo quando o login termina em erro.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(LoginRequest req) {
        Instant now = clock.instant();
        Optional<User> found = users.findByEmail(req.email().trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) {
            audit.record(null, AuditAction.LOGIN_FAILED, "USER", null, "e-mail não cadastrado");
            throw invalidCredentials();
        }
        User user = found.get();
        if (user.isLocked(now)) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                    "Conta bloqueada temporariamente por excesso de tentativas");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            user.registerFailedLogin(props.security().maxFailedLogins(),
                    now.plus(props.security().lockMinutes(), ChronoUnit.MINUTES));
            audit.record(user.getId(), AuditAction.LOGIN_FAILED, "USER", user.getId(), "senha inválida");
            if (user.isLocked(now)) {
                audit.record(user.getId(), AuditAction.ACCOUNT_LOCKED, "USER", user.getId(),
                        "bloqueado até " + user.getLockedUntil());
                throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                        "Conta bloqueada temporariamente por excesso de tentativas");
            }
            throw invalidCredentials();
        }
        user.registerSuccessfulLogin();
        audit.record(user.getId(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(), null);
        TokenService.IssuedToken token = tokenService.issue(user);
        return new TokenResponse(token.value(), "Bearer", token.expiresAt(), user.getRole(), user.getName());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha inválidos");
    }
}
