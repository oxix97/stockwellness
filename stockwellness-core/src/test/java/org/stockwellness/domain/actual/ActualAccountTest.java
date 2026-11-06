package org.stockwellness.domain.actual;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;
import static org.assertj.core.api.Assertions.*;

class ActualAccountTest {
    @Test void separate_krw_account_starts_at_zero_and_noop_rename_increments_only_account() {
        var account = ActualAccount.create(7, "  장부  ", "\u0085\u00a0", "KRW");
        assertThat(account.getName()).isEqualTo("장부");
        assertThat(account.getBrokerLabel()).isNull();
        assertThat(account.getAccountVersion()).isZero();
        account.rename("장부");
        assertThat(account.getAccountVersion()).isEqualTo(1);
        assertThat(account.getLedgerVersion()).isZero();
        assertThat(account.getMemberId()).isEqualTo(7);
    }
    @Test void unicode50_points_is_valid_but51_invalid_and_internal_whitespace_is_preserved() {
        var account = ActualAccount.create(7, "😀".repeat(50), "a\u00a0b", "KRW");
        assertThat(account.getName().length()).isEqualTo(100);
        assertThat(account.getBrokerLabel()).isEqualTo("a\u00a0b");
        assertThatThrownBy(() -> account.rename("😀".repeat(51))).isInstanceOf(GlobalException.class);
        assertThat(ActualAccountRules.text("\u200bname\ufeff", 50, false)).isEqualTo("\u200bname\ufeff");
    }
    @Test void invalid_currency_blank_name_and_owner_are_rejected() {
        assertThatThrownBy(() -> ActualAccount.create(7,"name",null,"USD")).isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> ActualAccount.create(7,"\u3000",null,"KRW")).isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> ActualAccount.create(0,"name",null,"KRW")).isInstanceOf(GlobalException.class);
    }
    @Test void safe_version_exhaustion_keeps_state() {
        var account = ActualAccount.create(7,"original",null,"KRW");
        ReflectionTestUtils.setField(account,"accountVersion",ActualAccountRules.MAX_SAFE_INTEGER);
        assertThatThrownBy(() -> account.rename("new")).isInstanceOfSatisfying(GlobalException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACTUAL_NUMERIC_RANGE_EXCEEDED));
        assertThat(account.getName()).isEqualTo("original");
        assertThat(account.getAccountVersion()).isEqualTo(ActualAccountRules.MAX_SAFE_INTEGER);
    }
    @Test void all_fixed_whitespace_points_trim_with_exact_boundary_and_header_grammar() {
        int count=0;
        int[] cps = {9,10,11,12,13,32,0x85,0xa0,0x1680,0x2000,0x2001,0x2002,0x2003,0x2004,
                0x2005,0x2006,0x2007,0x2008,0x2009,0x200a,0x2028,0x2029,0x202f,0x205f,0x3000};
        for (int cp:cps) {
            String ws=new String(Character.toChars(cp));
            assertThat(ActualAccountRules.text(ws+"x"+ws,50,false)).isEqualTo("x");
            count++;
        }
        assertThat(count).isEqualTo(25);
        assertThat(ActualAccountRules.ifMatch("\"9007199254740991\"")).isEqualTo(ActualAccountRules.MAX_SAFE_INTEGER);
        assertThatThrownBy(() -> ActualAccountRules.ifMatch("\"9007199254740992\"")).isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> ActualAccountRules.key("short")).isInstanceOf(GlobalException.class);
        ActualAccountRules.key("a".repeat(64));
        assertThatThrownBy(() -> ActualAccountRules.key("a".repeat(65))).isInstanceOf(GlobalException.class);
    }
}
