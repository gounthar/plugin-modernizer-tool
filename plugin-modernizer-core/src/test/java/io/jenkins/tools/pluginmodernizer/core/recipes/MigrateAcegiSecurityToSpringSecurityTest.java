package io.jenkins.tools.pluginmodernizer.core.recipes;

import static io.jenkins.tools.pluginmodernizer.core.recipes.DeclarativeRecipesTest.collectRewriteTestDependencies;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link MigrateAcegiSecurityToSpringSecurity}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class MigrateAcegiSecurityToSpringSecurityTest implements RewriteTest {

    @Test
    void migrateAcegiToSpringSecurity() {
        rewriteRun(
                spec -> {
                    var parser = JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true);
                    collectRewriteTestDependencies().forEach(parser::addClasspathEntry);
                    spec.recipe(new MigrateAcegiSecurityToSpringSecurity()).parser(parser);
                },
                java(
                        """
                         import org.acegisecurity.Authentication;
                         import org.acegisecurity.GrantedAuthority;
                         import org.acegisecurity.GrantedAuthorityImpl;
                         import org.acegisecurity.providers.AbstractAuthenticationToken;
                         import org.acegisecurity.context.SecurityContextHolder;
                         import org.acegisecurity.AuthenticationException;
                         import org.acegisecurity.AuthenticationManager;
                         import org.acegisecurity.BadCredentialsException;
                         import org.acegisecurity.userdetails.UserDetails;
                         import org.acegisecurity.userdetails.UserDetailsService;
                         import org.acegisecurity.userdetails.UsernameNotFoundException;
                         import jenkins.model.Jenkins;
                         import jenkins.security.SecurityListener;

                         public class Foo implements UserDetails {
                             @Override
                             public GrantedAuthority[] getAuthorities() {
                                 return new GrantedAuthority[] {
                                     new GrantedAuthorityImpl("ROLE_USER")
                                 };
                             }

                             @Override
                             public String getPassword() {
                                 return "password123";
                             }

                             @Override
                             public String getUsername() {
                                 return "testUser";
                             }

                             @Override
                             public boolean isAccountNonExpired() {
                                 return true;
                             }

                             @Override
                             public boolean isAccountNonLocked() {
                                 return true;
                             }

                             @Override
                             public boolean isCredentialsNonExpired() {
                                 return true;
                             }

                             @Override
                             public boolean isEnabled() {
                                 return true;
                             }
                             public UserDetails loadUserByUsername(String username) {
                                 return new Foo();
                             }
                             public void foo() {
                                 Authentication auth = Jenkins.getAuthentication();
                                 Foo userDetails = new Foo();
                                 SecurityListener.fireAuthenticated(userDetails);
                             }
                         }
                         """,
                        """
                        import org.springframework.security.core.Authentication;
                        import org.springframework.security.core.GrantedAuthority;
                        import org.springframework.security.core.authority.SimpleGrantedAuthority;
                        import org.springframework.security.authentication.AbstractAuthenticationToken;
                        import org.springframework.security.core.context.SecurityContextHolder;
                        import org.springframework.security.core.AuthenticationException;
                        import org.springframework.security.core.userdetails.UserDetails;
                        import org.springframework.security.core.userdetails.UserDetailsService;
                        import org.springframework.security.core.userdetails.UsernameNotFoundException;
                        import jenkins.model.Jenkins;
                        import jenkins.security.SecurityListener;
                        import org.springframework.security.authentication.AuthenticationManager;
                        import org.springframework.security.authentication.BadCredentialsException;

                        public class Foo implements UserDetails {
                            @Override
                            public GrantedAuthority[] getAuthorities() {
                                return new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_USER")
                                };
                            }

                            @Override
                            public String getPassword() {
                                return "password123";
                            }

                            @Override
                            public String getUsername() {
                                return "testUser";
                            }

                            @Override
                            public boolean isAccountNonExpired() {
                                return true;
                            }

                            @Override
                            public boolean isAccountNonLocked() {
                                return true;
                            }

                            @Override
                            public boolean isCredentialsNonExpired() {
                                return true;
                            }

                            @Override
                            public boolean isEnabled() {
                                return true;
                            }
                            public UserDetails loadUserByUsername(String username) {
                                return new Foo();
                            }
                            public void foo() {
                                Authentication auth = Jenkins.getAuthentication2();
                                Foo userDetails = new Foo();
                                SecurityListener.fireAuthenticated2(userDetails);
                            }
                        }
                        """));
    }
}
