package com.milesight.beaveriot.authentication.config;

import com.milesight.beaveriot.authentication.util.OAuth2EndpointUtils;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    IUserFacade userFacade;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDTO userDTO = userFacade.getEnableUserByEmail(username);
        if (userDTO == null) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, "user not found.", null);
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        return new User(username, userDTO.getEncodePassword(), authorities);
    }

}
