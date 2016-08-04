package org.pac4j.jwt.profile;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.jwt.JwtConstants;
import org.pac4j.jwt.config.DirectEncryptionConfiguration;
import org.pac4j.jwt.config.EncryptionConfiguration;
import org.pac4j.jwt.config.MacSignatureConfiguration;
import org.pac4j.jwt.config.SignatureConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * Generates a JWT token from a user profile.
 *
 * @author Jerome Leleu
 * @since 1.8.0
 */
public class JwtGenerator<U extends CommonProfile> {

    public static final String INTERNAL_ROLES = "$int_roles";
    public static final String INTERNAL_PERMISSIONS = "$int_perms";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private SignatureConfiguration signatureConfiguration;

    private EncryptionConfiguration encryptionConfiguration;

    public JwtGenerator() {}

    public JwtGenerator(final SignatureConfiguration signatureConfiguration) {
        this.signatureConfiguration = signatureConfiguration;
    }

    public JwtGenerator(final SignatureConfiguration signatureConfiguration, final EncryptionConfiguration encryptionConfiguration) {
        this.signatureConfiguration = signatureConfiguration;
        this.encryptionConfiguration = encryptionConfiguration;
    }

    @Deprecated
    public JwtGenerator(final String secret) {
        this(secret, true);
    }

    @Deprecated
    public JwtGenerator(final String secret, final boolean encrypted) {
        if (secret != null) {
            this.signatureConfiguration = new MacSignatureConfiguration(secret);
        }
        if (encrypted) {
            if (secret != null) {
                this.encryptionConfiguration = new DirectEncryptionConfiguration(secret);
            }
            logger.warn("Using the same key for signature and encryption may lead to security vulnerabilities. Consider using different keys");
        }
    }

    @Deprecated
    public JwtGenerator(final String signingSecret, final String encryptionSecret) {
        if (signingSecret != null) {
            this.signatureConfiguration = new MacSignatureConfiguration(signingSecret);
        }
        if (encryptionSecret != null) {
            this.encryptionConfiguration = new DirectEncryptionConfiguration(encryptionSecret);
        }
    }
    
    /**
     * Generates a JWT from a user profile.
     *
     * @param profile the given user profile
     * @return the created JWT
     */
    public String generate(final U profile) {
        verifyProfile(profile);

        final JWTClaimsSet claims = buildJwtClaimsSet(profile);

        // no signature configuration -> plain JWT
        if (signatureConfiguration == null) {
            return new PlainJWT(claims).serialize();

        } else {

            final SignedJWT signedJWT = signatureConfiguration.sign(claims);

            if (encryptionConfiguration != null) {

                return encryptionConfiguration.encrypt(signedJWT);
            } else {
                return signedJWT.serialize();
            }

        }
    }

    protected void verifyProfile(final U profile) {
        CommonHelper.assertNotNull("profile", profile);
        CommonHelper.assertNull("profile.sub", profile.getAttribute(JwtConstants.SUBJECT));
        CommonHelper.assertNull("profile.iat", profile.getAttribute(JwtConstants.ISSUE_TIME));
        CommonHelper.assertNull(INTERNAL_ROLES, profile.getAttribute(INTERNAL_ROLES));
        CommonHelper.assertNull(INTERNAL_PERMISSIONS, profile.getAttribute(INTERNAL_PERMISSIONS));
    }

    protected JWTClaimsSet buildJwtClaimsSet(final U profile) {
        // Build claims
        final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(profile.getTypedId())
                .issueTime(new Date());

        // add attributes
        final Map<String, Object> attributes = profile.getAttributes();
        for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        builder.claim(INTERNAL_ROLES, profile.getRoles());
        builder.claim(INTERNAL_PERMISSIONS, profile.getPermissions());

        // claims
        return builder.build();
    }

    public SignatureConfiguration getSignatureConfiguration() {
        return signatureConfiguration;
    }

    public void setSignatureConfiguration(final SignatureConfiguration signatureConfiguration) {
        this.signatureConfiguration = signatureConfiguration;
    }

    public EncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
    }

    public void setEncryptionConfiguration(final EncryptionConfiguration encryptionConfiguration) {
        this.encryptionConfiguration = encryptionConfiguration;
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(), "signatureConfiguration", signatureConfiguration, "encryptionConfiguration", encryptionConfiguration);
    }
}
