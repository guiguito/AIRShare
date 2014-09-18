package com.ggt.airshare.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;

/**
 * Contact utils
 * 
 * @author gduche
 */
public class ContactsUtils {

	public static AddressBookParsedResult getContactDetailsFromUri(
			Context context, Uri contactUri) {
		try {
			byte[] vcard;
			String vcardString;
			InputStream stream;
			stream = context.getContentResolver().openInputStream(contactUri);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[2048];
			int bytesRead;
			while ((bytesRead = stream.read(buffer)) > 0) {
				baos.write(buffer, 0, bytesRead);
			}
			vcard = baos.toByteArray();
			vcardString = new String(vcard, 0, vcard.length, "UTF-8");
			Result result = new Result(vcardString, vcard, null,
					BarcodeFormat.QR_CODE);
			ParsedResult parsedResult = ResultParser.parseResult(result);
			return (AddressBookParsedResult) parsedResult;
		} catch (Exception e) {
			return null;
		}
	}

}
