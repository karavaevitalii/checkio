import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Course;
import model.StepikPageLoader;
import model.StepikResponsePage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    private static final String BASE_URL = "https://stepik.org/api/";

    private final List<Course> courses = new ArrayList<>();

    private final StepikPageLoader stepikPageLoader;
    private final int N;

    private App(int N) {
        this.N = N;

        stepikPageLoader = getPageLoader();
    }

    private void exec() {
        final StepikResponsePage[] responsePage = new StepikResponsePage[1];
        final boolean[] hasNext = {true};
        int page = 1;
        do {
            stepikPageLoader.loadPage(page).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<StepikResponsePage> call,
                                       Response<StepikResponsePage> response) {
                    responsePage[0] = response.body();
                    if (responsePage[0] == null) {
                        hasNext[0] = false;
                        return;
                    }

                    if (!responsePage[0].hasNextPage())
                        hasNext[0] = false;

                    synchronized (courses) {
                        courses.addAll(responsePage[0].getCourses());
                    }
                }

                @Override
                public void onFailure(Call<StepikResponsePage> call, Throwable t) {
                    System.err.println("Something went wrong: " + t.toString());
                    System.exit(1);
                }
            });

            ++page;
        } while (hasNext[0]);

        final List<Course> popular = courses
                .stream()
                .sorted(Comparator.comparing(course -> -course.getLearnersCount()))
                .limit(N)
                .collect(Collectors.toList());
        for (Course course : popular) {
            System.out.println(course.getTitle() + '\t' + course.getLearnersCount());
        }
    }

    private StepikPageLoader getPageLoader() {
        final Gson gson = new GsonBuilder().setLenient().create();
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(StepikPageLoader.class);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ./gradlew run --project-prop N=<#N>");
            return;
        }

        try {
            final int N = Integer.parseInt(args[0]);
            if (N <= 0) {
                System.err.println("N should be positive");
                return;
            }

            new App(N).exec();
        } catch (NumberFormatException e) {
            System.err.println("N should be Integer");
        }
    }
}
