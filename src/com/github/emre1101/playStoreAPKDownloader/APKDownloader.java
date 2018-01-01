package com.github.emre1101.playStoreAPKDownloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.github.yeriomin.playstoreapi.AndroidAppDeliveryData;
import com.github.yeriomin.playstoreapi.AppDetails;
import com.github.yeriomin.playstoreapi.BuyResponse;
import com.github.yeriomin.playstoreapi.DeliveryResponse;
import com.github.yeriomin.playstoreapi.DetailsResponse;
import com.github.yeriomin.playstoreapi.GooglePlayAPI;
import com.github.yeriomin.playstoreapi.HttpCookie;
import com.github.yeriomin.playstoreapi.Offer;
import com.github.yeriomin.playstoreapi.PlayStoreApiBuilder;
import com.github.yeriomin.playstoreapi.PropertiesDeviceInfoProvider;

public class APKDownloader {

	private static final String EMAIL = "";
	private static final String PASSWORD = "";
	private static String GSFID = "";
	private static String TOKEN = "";

	private GooglePlayAPI api;

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Wrong number of args! Usage: playStoreAPKDownloader packageName outFilePath");
			return;
		}
		APKDownloader downloder = new APKDownloader();
		downloder.download(args[0], args[1]);
	}

	public void setUp() throws Exception {
		api = initApi();
	}

	public void download(String packageName, String outFile) throws Exception {

		api = login();

		DetailsResponse details = api.details(packageName);
		AppDetails appDetails = details.getDocV2().getDetails().getAppDetails();
		Offer offer = details.getDocV2().getOffer(0);

		int versionCode = appDetails.getVersionCode();
		long installationSize = appDetails.getInstallationSize();
		int offerType = offer.getOfferType();
		boolean checkoutRequired = offer.getCheckoutFlowRequired();

		if (checkoutRequired) {
			System.out.println("parali");
			return;
		}

		BuyResponse buyResponse = api.purchase(packageName, versionCode, offerType);
		DeliveryResponse response = api.delivery(packageName, versionCode, offerType);

		String url = response.getAppDeliveryData().getDownloadUrl();
		executeDownload(response.getAppDeliveryData(), outFile);
	}

	private void executeDownload(AndroidAppDeliveryData data, String outFile) throws IOException {
		StringBuilder cookies = new StringBuilder();
		for (HttpCookie cookie : data.getDownloadAuthCookieList()) {
			if (cookies.length() > 0)
				cookies.append("; ");
			cookies.append(cookie.getName());
			cookies.append("=");
			cookies.append(cookie.getValue());
		}

		HttpGet request = new HttpGet(data.getDownloadUrl());
		request.setHeader("Cookie", cookies.toString());
		request.setHeader("User-Agent", "APKDownloader/1" + " (Linux; U; Android " + "; " + " Build/" + ")");

		System.out.println("Downloading " + data.getDownloadUrl() + " to " + outFile);
		HttpResponse response = new DefaultHttpClient().execute(request);
		ReadableByteChannel inChannel = Channels.newChannel(response.getEntity().getContent());
		FileChannel outChannel = new FileOutputStream(outFile).getChannel();
		outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE);
		System.out.println("Downloaded " + outFile);

	}

	private GooglePlayAPI initApi() throws Exception {
		Properties properties = new Properties();
		try {
			properties.load(getClass().getClassLoader().getSystemResourceAsStream("device-honami.properties"));
		} catch (IOException e) {
			System.out.println("device-honami.properties not found");
			return null;
		}
		PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
		deviceInfoProvider.setProperties(properties);
		deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString());

		// google account info
		PlayStoreApiBuilder builder = new PlayStoreApiBuilder().setHttpClient(new OkHttpClientAdapter())
				.setDeviceInfoProvider(deviceInfoProvider).setEmail(EMAIL).setPassword(PASSWORD);
		GooglePlayAPI api = builder.build();
		TOKEN = api.getToken();
		GSFID = api.getGsfId();
		System.out.println("Token:" + TOKEN);
		System.out.println("GSFID:" + GSFID);

		return api;
	}

	private GooglePlayAPI login() throws Exception {
		Properties properties = new Properties();
		try {
			properties.load(getClass().getClassLoader().getSystemResourceAsStream("device-honami.properties"));
		} catch (IOException e) {
			System.out.println("device-honami.properties not found");
			return null;
		}
		PropertiesDeviceInfoProvider deviceInfoProvider = new PropertiesDeviceInfoProvider();
		deviceInfoProvider.setProperties(properties);
		deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString());

		PlayStoreApiBuilder builder = new PlayStoreApiBuilder().setHttpClient(new OkHttpClientAdapter())
				.setDeviceInfoProvider(deviceInfoProvider).setEmail(EMAIL).setPassword(PASSWORD).setToken(TOKEN)
				.setGsfId(GSFID);
		GooglePlayAPI api = builder.build();
		return api;
	}
}