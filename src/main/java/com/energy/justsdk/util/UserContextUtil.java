package com.energy.justsdk.util;

import com.energy.justsdk.model.UserContext;
import com.energy.justsdk.model.UserEnrollment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Bryan
 * @date 2019-12-18
 */
@Slf4j
public class UserContextUtil {

    /**
     * 序列化用户上下文
     *
     * @param context 用户上下文
     */
    public static void writeUserContext(UserContext context) throws Exception {
        String directoryPath = String.format("users/%s", context.getAffiliation());
        String filePath = String.format("%s/%s.ser", directoryPath, context.getName());
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        FileOutputStream file = new FileOutputStream(filePath);
        ObjectOutputStream out = new ObjectOutputStream(file);

        try {
            out.writeObject(context);
        } finally {
            out.close();
            file.close();
        }
    }

    /**
     * 反序列化用户上下文
     *
     * @param affiliation 用户affiliation
     * @param username 用户名
     */
    public static UserContext readUserContext(String affiliation, String username) throws Exception {
        String filePath = String.format("users/%s/%s.ser", affiliation, username);
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream fileStream = new FileInputStream(filePath);
            ObjectInputStream in = new ObjectInputStream(fileStream);

            try {
                UserContext context = (UserContext) in.readObject();
                return context;
            } finally {
                in.close();
                fileStream.close();
            }
        }
        return null;
    }

    /**
     * 通过用户私钥和证书文件，创建用户 Enrollment
     *
     * @param keyFile 私钥文件名称
     * @param certFile 证书文件
     */
    public static UserEnrollment getEnrollment(File keyFile, File certFile) throws Exception {
        try (InputStream isKey = new FileInputStream(keyFile);
            BufferedReader brKey = new BufferedReader(new InputStreamReader(isKey))) {

            StringBuilder keyBuilder = new StringBuilder();
            for (String line = brKey.readLine(); line != null; line = brKey.readLine()) {
                if (!line.contains("PRIVATE")) {
                    keyBuilder.append(line);
                }
            }

            byte[] encoded = DatatypeConverter.parseBase64Binary(keyBuilder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            PrivateKey key = KeyFactory.getInstance("EC").generatePrivate(keySpec);
            String certificate = new String(Files.readAllBytes(certFile.toPath()));
            return new UserEnrollment(key, certificate);
        }
    }

    public static void cleanUp() {
        String directoryPath = "users";
        File directory = new File(directoryPath);
        deleteDirectory(directory);
    }

    private static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    boolean success = deleteDirectory(child);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        log.info("Deleting - {}", dir.getName());
        return dir.delete();
    }
}
