package com.inventalert.identityService.config;

import com.inventalert.identityService.dto.response.LoginResponse;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<Role, String> roleToString = ctx -> ctx.getSource().name();

        modelMapper.typeMap(User.class, LoginResponse.class)
                .addMapping(User::getId, LoginResponse::setUserId)
                .addMappings(m -> m.using(roleToString).map(User::getRole, LoginResponse::setRole));

        return modelMapper;
    }
}
