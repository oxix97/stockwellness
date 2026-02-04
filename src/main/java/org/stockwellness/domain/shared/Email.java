package org.stockwellness.domain.shared;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.stockwellness.domain.member.exception.InvalidEmailException;

import java.util.regex.Pattern;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class Email {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private String address;

    public Email(String address) {
        if (!EMAIL_PATTERN.matcher(address).matches()) {
            throw new InvalidEmailException("이메일 형식이 바르지 않습니다: " + address);
        }
        this.address = address;
    }
}