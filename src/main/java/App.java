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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    private static final String BASE_URL = "https://stepik.org/api/";
    private static final int THREADS_NUM = 16;
    private final ExecutorService loaders = Executors.newFixedThreadPool(THREADS_NUM);
    private final Phaser phaser = new Phaser();
    private final AtomicInteger page = new AtomicInteger();

    private final List<Course> courses = new CopyOnWriteArrayList<>();

    private final StepikPageLoader stepikPageLoader;
    private final int N;

    private App(int N) {
        this.N = N;
        stepikPageLoader = createPageLoader();
    }

    private void exec() {
        for (int i = 0; i < THREADS_NUM; i++)
            loaders.submit(new Loader());
        phaser.arriveAndAwaitAdvance();
        loaders.shutdown();

        courses.stream()
                .sorted((c1, c2) -> c2.getLearnersCount() - c1.getLearnersCount())
                .limit(N)
                .forEach(c -> System.out.printf("%-75s%d%n", c.getTitle(), c.getLearnersCount()));
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
        @Override
        public void run() {
            phaser.register();
            try {
                StepikResponsePage response = stepikPageLoader
                        .loadPage(page.incrementAndGet())
                        .execute()
                        .body();
                if (response != null) {
                    courses.addAll(response.getCourses());
                    if (response.hasNextPage())
                        loaders.submit(new Loader());
                }
            } catch (IOException e) {
                System.err.println("Something wrong happened while talking to server: " +
                        e.getMessage());
            } finally {
                phaser.arrive();
            }
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
