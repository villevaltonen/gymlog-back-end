package gymlog.security

import gymlog.security.jwt.JWTAuthenticationFilter
import gymlog.security.jwt.JWTLoginFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.security.SecureRandom
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
class WebSecurityConfig: WebSecurityConfigurerAdapter() {

    @Autowired
    @Qualifier("gymlogdatasource")
    private val gymlogDataSource: DataSource? = null

    @Value("encoding.random")
    private val secureRandom: String? = null

    @Value("jwt.secret")
    private val jwtSecret: String? = null

    // needed for encrypted passwords
    @Bean
    fun passwordEncoder() : PasswordEncoder {
        return BCryptPasswordEncoder(10, SecureRandom((secureRandom!!).toByteArray()))
    }

    override fun configure(http: HttpSecurity) {
        // disable caching
        http.headers().cacheControl()

        http.csrf().disable() // disable csrf for requests
                .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers(HttpMethod.POST, "/login").permitAll()
                .antMatchers(HttpMethod.POST, "/register").permitAll()
                .anyRequest().authenticated()
                .and()
                // filtering login requests
                .addFilterBefore(JWTLoginFilter(url = "/login", authenticationManager = authenticationManager(), jwtSecret = jwtSecret!!), UsernamePasswordAuthenticationFilter::class.java)
                // filter other requests to check JWT header
                .addFilterBefore(JWTAuthenticationFilter(jwtSecret), UsernamePasswordAuthenticationFilter::class.java)
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.jdbcAuthentication()
                .dataSource(gymlogDataSource)
                .passwordEncoder(passwordEncoder())
                .usersByUsernameQuery("select username, password, enabled from gymlog_db.users where username = ?")
                .authoritiesByUsernameQuery("select username, authority from gymlog_db.authorities where username = ?")
    }

    companion object {
        fun passwordEncoder(secureRandom: String) : PasswordEncoder {
            return BCryptPasswordEncoder(10, SecureRandom((secureRandom).toByteArray()))
        }
    }
}