package org.amv.access.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class UserValidator implements Validator {

    @Autowired
    private UserRepository userRepository;

    private Pageable firstPageSingleItemRequest = new PageRequest(0, 1);

    @Override
    public boolean supports(Class<?> aClass) {
        return UserEntity.class.equals(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        UserEntity user = (UserEntity) o;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "NotEmpty");
        if (user.getName().length() < 3 || user.getName().length() > 32) {
            errors.rejectValue("username", "Size.userForm.username");
        }
        if (userRepository.findByName(user.getName(), firstPageSingleItemRequest).hasContent()) {
            errors.rejectValue("username", "Duplicate.userForm.username");
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "NotEmpty");
        if (user.getPassword().length() < 3) {
            errors.rejectValue("password", "Size.userForm.password");
        }
    }
}
