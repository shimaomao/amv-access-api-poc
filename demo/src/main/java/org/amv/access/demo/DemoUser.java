package org.amv.access.demo;

import org.amv.access.model.UserEntity;

public class DemoUser {
    private String name;
    private String password;
    private String encryptedPassword;
    private UserEntity origin;

    DemoUser(String name, String password, String encryptedPassword, UserEntity origin) {
        this.name = name;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.origin = origin;
    }

    public static DemoUserBuilder builder() {
        return new DemoUserBuilder();
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public String getEncryptedPassword() {
        return this.encryptedPassword;
    }

    public UserEntity getOrigin() {
        return this.origin;
    }

    public static class DemoUserBuilder {
        private String name;
        private String password;
        private String encryptedPassword;
        private UserEntity origin;

        DemoUserBuilder() {
        }

        public DemoUserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public String name() {
            return name;
        }

        public DemoUserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public String password() {
            return password;
        }

        public DemoUserBuilder encryptedPassword(String encryptedPassword) {
            this.encryptedPassword = encryptedPassword;
            return this;
        }

        public String encryptedPassword() {
            return encryptedPassword;
        }

        public DemoUserBuilder origin(UserEntity origin) {
            this.origin = origin;
            return this;
        }

        public DemoUser build() {
            return new DemoUser(name, password, encryptedPassword, origin);
        }
    }
}
