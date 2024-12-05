package com.example.clocklike_portal.health;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/health")
public class HealthController {

    @Value("${server.ssl.key-store-password}")
    private String keystorePassword;
    @Value("${server.ssl.key-store}")
    private String keystorePath;

    @GetMapping("/ssl")
    public String checkSslStatus() {
        String pathWithoutPrefix = keystorePath.replace("file:", "");
        System.out.println(pathWithoutPrefix);
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream keystoreFile = new FileInputStream(pathWithoutPrefix);
            keyStore.load(keystoreFile, keystorePassword.toCharArray());

            String alias = "1";
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            if (certificate == null) {
                return "Certyfikat nie został znaleziony w keystore.";
            }

            Date notBefore = certificate.getNotBefore();
            Date notAfter = certificate.getNotAfter();
            Date currentDate = new Date();

            if (currentDate.after(notBefore) && currentDate.before(notAfter)) {
                return String.format("Certyfikat jest aktywny. Ważny od: %s do: %s",
                        notBefore.toString(), notAfter.toString());
            } else {
                return String.format("Certyfikat wygasł lub nie jest jeszcze aktywny. Ważny od: %s do: %s",
                        notBefore.toString(), notAfter.toString());
            }

        } catch (Exception e) {
            return "Błąd podczas sprawdzania certyfikatu: " + e.getMessage();
        }
    }
}
