import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TestApiTracks {
    public static void main(String[] args) throws Exception {
        String baseUrl = "https://api.hlowb.com";
        byte[] keySuffix = "T!BgJB".getBytes("US-ASCII");

        URL url = new URL(baseUrl + "/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String secResp = in.readLine();
        in.close();

        int b64Start = secResp.indexOf("\"data\":\"") + 8;
        int b64End = secResp.indexOf("\"", b64Start);
        String apiKeyB64 = secResp.substring(b64Start, b64End);
        byte[] apiKeyBytes = Base64.getDecoder().decode(apiKeyB64);
        byte[] keyMaterial = new byte[apiKeyBytes.length + keySuffix.length];
        System.arraycopy(apiKeyBytes, 0, keyMaterial, 0, apiKeyBytes.length);
        System.arraycopy(keySuffix, 0, keyMaterial, apiKeyBytes.length, keySuffix.length);

        byte[] finalKey = new byte[16];
        System.arraycopy(keyMaterial, 0, finalKey, 0, Math.min(keyMaterial.length, 16));

        String searchUrl = baseUrl + "/film-api/v1.1.0/movie/searchByKeyword?channel=IndiaA&clientType=1&clientType=1&keyword=Batman";
        String searchJson = fetchAndDecrypt(searchUrl, finalKey);
        int idStart = searchJson.indexOf("\"id\":") + 5;
        int idEnd = searchJson.indexOf(",", idStart);
        String realId = searchJson.substring(idStart, idEnd).trim();
        
        String detailsUrl = baseUrl + "/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&clientType=1&lang=en-US&movieId=" + realId;
        String detailsJson = fetchAndDecrypt(detailsUrl, finalKey);
        
        int tStart = detailsJson.indexOf("\"availableTracks\":");
        if (tStart != -1) {
            System.out.println("AVAILABLE TRACKS: " + detailsJson.substring(tStart, Math.min(tStart + 2000, detailsJson.length())));
        } else {
            System.out.println("No availableTracks found in movie API.");
        }
    }
    
    public static String fetchAndDecrypt(String urlStr, byte[] finalKey) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();
        
        String rawResponse = response.toString().trim();
        if (rawResponse.startsWith("\"") && rawResponse.endsWith("\"")) {
            rawResponse = rawResponse.substring(1, rawResponse.length() - 1);
        }
        byte[] encryptedData = Base64.getDecoder().decode(rawResponse);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(finalKey, "AES"), new IvParameterSpec(finalKey));
        return new String(cipher.doFinal(encryptedData), "UTF-8");
    }
}
