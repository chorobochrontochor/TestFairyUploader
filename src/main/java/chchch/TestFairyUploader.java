package chchch;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestFairyUploader
{
    private static String TestFairyUploadAPI = "https://upload.testfairy.com/api/upload/";

    private static String getAppName()
    {
        return TestFairyUploader.class.getPackage().getImplementationTitle();
    }
    private static String getAppVersion()
    {
        return TestFairyUploader.class.getPackage().getImplementationVersion();
    }
    private static boolean hasArg(String[] args, String flag)
    {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }
    private static String getArgValue(String[] args, String flag, boolean optional, String defaultValue) throws Exception
    {
        boolean nextIsValue = false;
        for (String arg : args) {
            if (nextIsValue) {
                return arg;
            }
            if (arg.equals(flag)) {
                nextIsValue = true;
            }
        }
        if (optional) return defaultValue;

        if (nextIsValue) {
            throw new Exception("Value for mandatory argument \"" + flag + "\" is missing!");
        } else {
            throw new Exception("Mandatory argument \"" + flag + "\" is missing!");
        }
    }
    public static void main(String[] args)
    {
        try
        {
            System.out.println(getAppName() + ": v" + getAppVersion());
            if (hasArg(args, "-version")) {
                System.exit(0);
            }

            System.out.print("Processing command line arguments...");
            String apiKey = getArgValue(args, "-apiKey", false, null);
            int httpTimeout = Integer.parseInt(getArgValue(args, "-httpTimeout", true, "120"));
            int retryCount = Integer.parseInt(getArgValue(args, "-retryCount", true, "0"));
            String appFilePath = getArgValue(args, "-appFilePath", false, null);
            String symbolsFilePath = getArgValue(args, "-symbolsFilePath", true, null);
            String testersGroups = getArgValue(args, "-testersGroups", true, null);
            boolean notifyTesters = hasArg(args, "-notifyTesters");
            boolean autoUpdate = hasArg(args, "-autoUpdate");
            String releaseNotesFilePath = getArgValue(args, "-releaseNotesFilePath", true, null);
            String tags = getArgValue(args, "-tags", true, null);
            System.out.println("Done.");

            System.out.println();
            System.out.println("Settings: ");
            System.out.println(" - apiKey: *****");
            System.out.println(" - httpTimeout: " + httpTimeout);
            System.out.println(" - appFilePath: " + appFilePath);
            System.out.println(" - symbolsFilePath: " + symbolsFilePath);
            System.out.println(" - testersGroups: " + testersGroups);
            System.out.println(" - notifyTesters: " + notifyTesters);
            System.out.println(" - autoUpdate: " + autoUpdate);
            System.out.println(" - releaseNotesFilePath: " + releaseNotesFilePath);
            System.out.println(" - tags: " + tags);
            System.out.println();

            System.out.println("Done.");

            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            requestBuilder.setConnectTimeout(httpTimeout * 1000);
            requestBuilder.setConnectionRequestTimeout(httpTimeout * 1000);
            requestBuilder.setSocketTimeout(httpTimeout * 1000);

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(1, true));

            httpClientBuilder.setRetryHandler(new HttpRequestRetryHandler()
            {
                @Override
                public boolean retryRequest(IOException exception, int executionCount, HttpContext context)
                {
                    return executionCount <= retryCount && exception instanceof SocketException;
                }
            });
            httpClientBuilder.setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy()
            {
                @Override
                public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context)
                {
                    return executionCount <= retryCount && response.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
                }
                @Override
                public long getRetryInterval()
                {
                    return 30000;
                }
            });
            httpClientBuilder.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
            httpClientBuilder.setDefaultRequestConfig(requestBuilder.build());
            HttpClient httpClient = httpClientBuilder.build();

            HttpPost httpPost = new HttpPost(TestFairyUploadAPI);
            httpPost.setConfig(requestBuilder.build());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("api_key", apiKey, ContentType.TEXT_PLAIN);
            if (testersGroups != null) {
                builder.addTextBody("testers_groups", testersGroups, ContentType.TEXT_PLAIN);
            }
            if (notifyTesters) {
                builder.addTextBody("notify", "on", ContentType.TEXT_PLAIN);
            }
            if (autoUpdate) {
                builder.addTextBody("auto_update", "on", ContentType.TEXT_PLAIN);
            }
            if (releaseNotesFilePath != null) {
                String releaseNotes = new String(Files.readAllBytes(Paths.get(releaseNotesFilePath)), StandardCharsets.UTF_8).replaceAll("[^\\x00-\\x7e]", "");
                builder.addTextBody("release_notes", releaseNotes, ContentType.TEXT_PLAIN);
            }
            if (tags != null) {
                builder.addTextBody("tags", tags, ContentType.TEXT_PLAIN);
            }

            File appFile = new File(appFilePath);
            builder.addBinaryBody(
                "file",
                new FileInputStream(appFile),
                ContentType.APPLICATION_OCTET_STREAM,
                appFile.getName()
            );

            if (symbolsFilePath != null) {
                File symbolsFile = new File(appFilePath);
                builder.addBinaryBody(
                    "symbols_file",
                    new FileInputStream(symbolsFile),
                    ContentType.APPLICATION_OCTET_STREAM,
                    symbolsFile.getName()
                );
            }

            httpPost.setEntity(builder.build());
            HttpResponse response = httpClient.execute(httpPost);
            String stringResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONObject jsonObject = new JSONObject(stringResponse);

            System.out.println("Response:");
            System.out.println("raw: " + stringResponse);
            System.out.println();
            String status = (String) jsonObject.get("status");
            System.out.println("status: " + status);

            if (status != null && status.equals("ok")) {
                String app_name = (String) jsonObject.get("app_name");
                String app_version = (String) jsonObject.get("app_version");
                String app_url = (String) jsonObject.get("app_url");
                String landing_page_url = (String) jsonObject.get("landing_page_url");

                System.out.println("app_name: " + app_name);
                System.out.println("app_version: " + app_version);
                System.out.println("app_url: " + app_url);
                System.out.println("landing_page_url: " + landing_page_url);
                System.exit(0);
            } else {
                String message = (String) jsonObject.get("message");
                System.out.println("message: " + message);
                int code = Integer.parseInt(jsonObject.get("code").toString());
                System.out.println("code: " + code);
                System.exit(code > 0 ? code : 2);
            }
        }
        catch(Exception exception)
        {
            System.out.println("Failed.");
            System.err.println("Error: " + exception.getMessage());
            System.exit(2);
        }
    }
}