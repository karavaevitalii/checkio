package model;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StepikPageLoader {
    @GET("courses")
    Call<StepikResponsePage> loadPage(@Query("page") int page);
}
