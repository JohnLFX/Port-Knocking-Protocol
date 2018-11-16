package cnt4004.protocol;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAIO {

    public static PublicKey decodePublicKey(KeyFactory keyFactory, String encoded) throws InvalidKeySpecException {
        return keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048); //TODO Configurable?
        return kpg.generateKeyPair();
    }

    public static void save(Path file, KeyPair keyPair) throws Exception {

        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(file))) {

            writer.println(Base64.getEncoder().encodeToString(
                    new X509EncodedKeySpec(keyPair.getPublic().getEncoded()).getEncoded())
            );

            writer.println(Base64.getEncoder().encodeToString(
                    new X509EncodedKeySpec(keyPair.getPrivate().getEncoded()).getEncoded())
            );

        }

    }

    public static KeyPair load(Path file) throws Exception {

        try (BufferedReader br = Files.newBufferedReader(file)) {

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(br.readLine()));
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(br.readLine()));
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);

        }

    }

}
