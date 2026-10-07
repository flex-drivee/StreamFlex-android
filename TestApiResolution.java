import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TestApiResolution {
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
        String searchJson = fetchAndDecrypt(searchUrl, finalKey, "GET", null);
        int idStart = searchJson.indexOf("\"id\":") + 5;
        int idEnd = searchJson.indexOf(",", idStart);
        String realId = searchJson.substring(idStart, idEnd).trim();
        
        String detailsUrl = baseUrl + "/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&clientType=1&lang=en-US&movieId=" + realId;
        String detailsJson = fetchAndDecrypt(detailsUrl, finalKey, "GET", null);
        
        int epsStart = detailsJson.indexOf("\"episodes\":[{\"id\":") + 18;
        int epsEnd = detailsJson.indexOf(",", epsStart);
        String epId = detailsJson.substring(epsStart, epsEnd).trim();

        String videoUrl = "https://api.hlowb.com/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US";
        String body = "{\"mode\":\"1\",\"appMarket\":\"GuanWang\",\"clientType\":\"1\",\"woolUser\":\"false\",\"apkSignKey\":\"ED0955EB04E67A1D9F3305B95454FED485261475\",\"androidVersion\":\"13\",\"movieId\":\"" + realId + "\",\"episodeId\":\"" + epId + "\",\"isNewUser\":\"true\",\"resolution\":\"3\"}";
        
        System.out.println("Testing resolution 3 (1080p)...");
        System.out.println(fetchAndDecrypt(videoUrl, finalKey, "POST", body));
    }
    
    public static String fetchAndDecrypt(String urlStr, byte[] finalKey, String method, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes("UTF-8"));
            conn.getOutputStream().close();
        }
        int code = conn.getResponseCode();
        if (code != 200) {
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder er = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) er.append(line);
            return "Error " + code + ": " + er.toString();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();
        
        String rawResponse = response.toString().trim();
        if (rawResponse.startsWith("\"") && rawResponse.endsWith("\"")) {
            rawResponse = rawResponse.substring(1, rawResponse.length() - 1);
        }
        if (rawResponse.startsWith("{")) return rawResponse;
        
        byte[] encryptedData = Base64.getDecoder().decode(rawResponse);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(finalKey, "AES"), new IvParameterSpec(finalKey));
        return new String(cipher.doFinal(encryptedData), "UTF-8");
    }
}
