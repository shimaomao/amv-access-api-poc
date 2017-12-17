package org.amv.access.client.android;

import io.reactivex.Observable;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import org.amv.access.client.android.model.ErrorResponseDto;

import java.io.Reader;

final class ResponseHelper {

    public static <T> Observable<T> parse(Response response, Class<T> clazz) {
        try {
            final ResponseBody responseBody = response.body();
            Reader reader = responseBody.charStream();

            try {
                if (!response.isSuccessful()) {
                    ErrorResponseDto errorResponseDto = Clients
                            .defaultObjectMapper
                            .fromJson(reader, ErrorResponseDto.class);

                    return Observable.error(new AccessApiException(errorResponseDto, null));
                }

                T parsedBody = Clients
                        .defaultObjectMapper
                        .fromJson(reader, clazz);

                return Observable.just(parsedBody);
            } finally {
                Util.closeQuietly(reader);
            }
        } catch (Exception e) {
            return Observable.error(e);
        } finally {
            Util.closeQuietly(response);
        }
    }
}
