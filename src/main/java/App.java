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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class App {
    private static final String BASE_URL = "https://stepik.org/api/";
    private static final int THREADS_NUM = 10;

    private static List<Course> courses = new CopyOnWriteArrayList<>();

    private final StepikPageLoader stepikPageLoader;
    private final int N;

    private App(int N) {
        this.N = N;

        stepikPageLoader = getPageLoader();
    }

    private void exec() {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS_NUM);
        for (int i = 0; i < THREADS_NUM; i++) {
            pool.submit(new Task(stepikPageLoader, pool));
        }

        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            pool.shutdown();
            Thread.currentThread().interrupt();
        }

        courses = courses
                .stream()
                .sorted(Comparator.comparing(course -> -course.getLearnersCount()))
                .limit(N)
                .collect(Collectors.toList());
        for (Course course : courses) {
            System.out.println(course.getTitle() + '\t' + course.getLearnersCount());
        }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
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

    private static class Task implements Runnable {
        final static AtomicInteger page = new AtomicInteger();

        final ExecutorService pool;
        final StepikPageLoader stepikPageLoader;

        Task(StepikPageLoader stepikPageLoader, ExecutorService pool) {
            this.stepikPageLoader = stepikPageLoader;
            this.pool = pool;
        }

        @Override
        public void run() {
            stepikPageLoader.loadPage(page.incrementAndGet()).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<StepikResponsePage> call,
                                       Response<StepikResponsePage> response) {
                    final StepikResponsePage responsePage = response.body();
                    if (responsePage != null) {
                        courses.addAll(responsePage.getCourses());


                        if (responsePage.hasNextPage())
                            pool.submit(new Task(stepikPageLoader, pool));
                    }
                }

                @Override
                public void onFailure(Call<StepikResponsePage> call, Throwable t) {
                    System.err.println("Something wrong happened: " + t.toString());
                    System.exit(1);
                }
            });
        }
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
