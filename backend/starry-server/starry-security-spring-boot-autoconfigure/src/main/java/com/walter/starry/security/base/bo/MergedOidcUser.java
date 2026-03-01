package com.walter.starry.security.base.bo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @Author: walter.tan
 * @DateTime: 2024-01-21 01:47:32
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class MergedOidcUser extends DefaultOidcUser {

    @Getter
    private UserDetails userDetails;

    public MergedOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, UserDetails userDetails) {
        this(authorities, idToken, IdTokenClaimNames.SUB, userDetails);
    }

    public MergedOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
                           String nameAttributeKey, UserDetails userDetails) {
        this(authorities, idToken, null, nameAttributeKey, userDetails);
    }

    public MergedOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
                           OidcUserInfo userInfo, UserDetails userDetails) {
        this(authorities, idToken, userInfo, IdTokenClaimNames.SUB, userDetails);
    }

    public MergedOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
                           OidcUserInfo userInfo, String nameAttributeKey, UserDetails userDetails) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.userDetails = userDetails;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SortedSet<GrantedAuthority> sortedAuthorities = new TreeSet<>(
                Comparator.comparing(GrantedAuthority::getAuthority));
        sortedAuthorities.addAll(CollectionUtils.union(super.getAuthorities(), userDetails.getAuthorities()));
        return sortedAuthorities;
    }
}
