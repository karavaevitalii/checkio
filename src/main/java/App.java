import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Course;
import model.StepikPageLoader;
import model.StepikResponsePage;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class App {
    private static final String BASE_URL = "https://stepik.org/api/";
    private static final int THREADS_NUM = 16;

    private final List<Course> courses = new CopyOnWriteArrayList<>();

    private final StepikPageLoader stepikPageLoader;
    private final int N;

    private App(int N) {
        this.N = N;
        stepikPageLoader = createPageLoader();
    }

    private void exec() {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS_NUM);
        for (int i = 1; i <= THREADS_NUM; i++)
            pool.submit(new Loader(i));

        pool.shutdown();
        try {
            if (!pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                pool.shutdownNow();
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        courses.stream()
                .sorted((c1, c2) -> c2.getLearnersCount() - c1.getLearnersCount())
                .limit(N)
                .forEach(c -> System.out.printf("%-50s%d%n", c.getTitle(), c.getLearnersCount()));
    }

    private StepikPageLoader createPageLoader() {
        final Gson gson = new GsonBuilder().setLenient().create();
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(StepikPageLoader.class);
    }

    private class Loader implements Runnable {
        int page;

        Loader(int page) {
            this.page = page;
        }

        @Override
        public void run() {
            boolean hasNext;
            do {
                try {
                    final StepikResponsePage response = stepikPageLoader
                            .loadPage(page)
                            .execute()
                            .body();
                    if (response != null) {
                        courses.addAll(response.getCourses());
                        hasNext = response.hasNextPage();
                    } else
                        hasNext = false;
                } catch (IOException e) {
                    System.err.println("Some problem occurred while talking to the server: " +
                            e.getMessage());
                    hasNext = true;
                }

                page += THREADS_NUM;
            } while (hasNext);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ./gradlew run --project-prop N=<#N>");
            return;
        }

        try {
            final int N = Integer.parseInt(args[0]);
            if (N <= 0)
                System.err.println("N should be positive");
            else
                new App(N).exec();
        } catch (NumberFormatException e) {
            System.err.println("N should be Integer");
        }
    }
}
