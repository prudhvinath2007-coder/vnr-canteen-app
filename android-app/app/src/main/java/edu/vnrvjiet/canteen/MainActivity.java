package edu.vnrvjiet.canteen;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "VnrCanteen";
    private static final int UPI_PAYMENT_REQUEST_CODE = 2001;

    public static final String PACKAGE_PHONEPE = "com.phonepe.app";
    public static final String PACKAGE_PAYTM = "net.one97.paytm";
    public static final String PACKAGE_GPAY = "com.google.android.apps.nbu.paisa.user";
    public static final String PACKAGE_BHIM = "in.org.npci.upiapp";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Default canteen web app URL (Use localhost/10.0.2.2 or production domain)
    public static final String CANTEEN_URL = "http://10.0.2.2:8080/student";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " VnrCanteenAndroidApp/1.0.0");

        // Enable Cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Swipe to Refresh
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        // Native JavaScript Bridge for direct 1-tap UPI app launching
        webView.addJavascriptInterface(new UpiNativeBridge(), "AndroidUpi");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Intercept UPI Payment links
                if (url.startsWith("upi://pay")) {
                    handleUpiUrl(url, null);
                    return true;
                }

                // Handle intent:// schemes
                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        startActivityForResult(intent, UPI_PAYMENT_REQUEST_CODE);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse intent URL: " + e.getMessage());
                    }
                }

                // Direct app schemes
                if (url.startsWith("phonepe://") || url.startsWith("paytmmp://") || url.startsWith("tez://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivityForResult(intent, UPI_PAYMENT_REQUEST_CODE);
                        return true;
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Requested payment app not found", Toast.LENGTH_SHORT).show();
                    }
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "Network error: Unable to connect to Canteen Server", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load VNR VJIET Canteen App
        webView.loadUrl(CANTEEN_URL);
    }

    /**
     * JavaScript Interface Bridge exposed to the web application
     */
    public class UpiNativeBridge {
        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }

        @JavascriptInterface
        public void payWithPhonePe(String upiUri) {
            runOnUiThread(() -> handleUpiUrl(upiUri, PACKAGE_PHONEPE));
        }

        @JavascriptInterface
        public void payWithPaytm(String upiUri) {
            runOnUiThread(() -> handleUpiUrl(upiUri, PACKAGE_PAYTM));
        }

        @JavascriptInterface
        public void payWithGPay(String upiUri) {
            runOnUiThread(() -> handleUpiUrl(upiUri, PACKAGE_GPAY));
        }

        @JavascriptInterface
        public void payWithAnyUpi(String upiUri) {
            runOnUiThread(() -> handleUpiUrl(upiUri, null));
        }
    }

    /**
     * Dispatches native Android Intent to PhonePe, Paytm, Google Pay, or generic chooser
     */
    private void handleUpiUrl(String upiUri, @Nullable String specificPackage) {
        try {
            Intent upiIntent = new Intent(Intent.ACTION_VIEW);
            upiIntent.setData(Uri.parse(upiUri));

            if (specificPackage != null && isAppInstalled(specificPackage)) {
                upiIntent.setPackage(specificPackage);
                startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST_CODE);
            } else {
                Intent chooser = Intent.createChooser(upiIntent, "Pay via UPI (PhonePe, Paytm, Google Pay)");
                startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No UPI payment apps found on this phone", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initiating UPI payment", e);
            Toast.makeText(this, "Could not open UPI app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Intercepts transaction response returned by UPI app
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            String response = null;
            if (data != null) {
                response = data.getStringExtra("response");
                if (response == null && data.getData() != null) {
                    response = data.getData().toString();
                }
            }

            Log.d(TAG, "UPI Payment Response: " + response);

            Map<String, String> params = parseUpiResponse(response);
            String status = params.get("status");
            String txnId = params.get("txnid");
            if (txnId == null) txnId = params.get("approvalrefno");
            if (txnId == null) txnId = "UPI" + System.currentTimeMillis();

            if ("success".equalsIgnoreCase(status) || "submitted".equalsIgnoreCase(status) || resultCode == RESULT_OK) {
                Toast.makeText(this, "UPI Payment Successful! Ref: " + txnId, Toast.LENGTH_LONG).show();
                final String finalTxnId = txnId;
                // Auto-fill transaction reference into the web form and submit
                webView.post(() -> webView.evaluateJavascript(
                        "if (typeof window.onUpiPaymentSuccess === 'function') { window.onUpiPaymentSuccess('" + finalTxnId + "'); }",
                        null
                ));
            } else {
                Toast.makeText(this, "Payment was not completed. Please retry or scan QR code.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Map<String, String> parseUpiResponse(String response) {
        Map<String, String> map = new HashMap<>();
        if (response == null || response.trim().isEmpty()) {
            return map;
        }

        try {
            String[] pairs = response.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length >= 2) {
                    map.put(kv[0].trim().toLowerCase(), URLDecoder.decode(kv[1].trim(), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed parsing UPI response", e);
        }
        return map;
    }

    // Hardware Back Button Navigation
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
