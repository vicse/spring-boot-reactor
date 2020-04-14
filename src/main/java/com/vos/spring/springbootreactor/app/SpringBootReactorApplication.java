package com.vos.spring.springbootreactor.app;

import com.vos.spring.springbootreactor.app.models.Comments;
import com.vos.spring.springbootreactor.app.models.User;
import com.vos.spring.springbootreactor.app.models.UserComment;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(SpringBootReactorApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(SpringBootReactorApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {

    contraPressure();

  }

  public void contraPressure() {

    Flux.range(1, 10)
    .log()
    //.limitRate(5)
    .subscribe(new Subscriber<Integer>() {

      private Subscription s;
      private Integer limit = 2;
      private Integer consumed = 0;

      @Override
      public void onSubscribe(Subscription subscription) {
        this.s = subscription;
        s.request(limit);

      }

      @Override
      public void onNext(Integer integer) {
        logger.info(integer.toString());
        consumed++;
        if (consumed.equals(limit)) {
          consumed = 0;
          s.request(limit);
        }

      }

      @Override
      public void onError(Throwable throwable) {

      }

      @Override
      public void onComplete() {

      }
    });

  }

  public void exampleIntervalSinceCreate() {

    Flux.create(emitter -> {
      Timer timer = new Timer();
      timer.schedule(new TimerTask() {

        private Integer counter = 0;

        @Override
        public void run() {
          emitter.next(++counter);
          if (counter == 10) {
            timer.cancel();
            emitter.complete();
          }

          if (counter == 5 ) {
            timer.cancel();
            emitter.error(new InterruptedException("Error, se ha detenido el flux en 5!"));
          }
        }
      }, 1000, 1000);
    })
    .subscribe(next -> logger.info(next.toString()),
            error -> logger.error(error.getMessage()),
            () -> logger.info("Hemos Terminado"));

  }

  public void exampleIntervalInfinity() throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);

    Flux.interval(Duration.ofSeconds(1))
            .doOnTerminate(latch::countDown)
            .flatMap(i -> {
              if (i >= 5) {
                return Flux.error(new InterruptedException("Only, since 5!!!"));
              }
              return Flux.just(i);
            })
            .map(i -> "Hello" + i)
            .retry(2)
            .subscribe(logger::info, e -> logger.error(e.getMessage()));


    latch.await();

  }

  public void exampleDelayElements() throws InterruptedException {

    Flux<Integer> range = Flux.range(1, 12)
            .delayElements(Duration.ofSeconds(1))
            .doOnNext(i -> logger.info(i.toString()));

    //BlockLast is only example because is not recommend block
    range.blockLast();

    // Subscribe()
    //Thread.sleep(13000);

  }

  public void exampleInterval() {

    Flux<Integer> range = Flux.range(1, 12);
    Flux<Long> delay = Flux.interval(Duration.ofSeconds(1));

    range.zipWith(delay, (ra, de) -> ra)
            .doOnNext(i -> logger.info(i.toString()))
            .blockLast();

  }

  public void exampleUserCommentZipWithRange() {

    Flux<Integer> ranges = Flux.range(0, 4);

    Flux.just(1, 2, 3, 4)
            .map(i -> (i*4))
            .zipWith(ranges, (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
            .subscribe(logger::info);

  }

  public void exampleUserCommentZipWithForm2() {

    Mono<User> userMono = Mono.fromCallable(() -> new User("Vicse", "Ore"));

    Mono<Comments> commentsMono = Mono.fromCallable(() -> {
      Comments comments = new Comments();
      comments.addComment("Hola, Como estas");
      comments.addComment("Heyy, holaaa");
      comments.addComment("Que tal pproo");
      return comments;
    });

    Mono<UserComment> userCommentMono = userMono
            .zipWith(commentsMono)
            .map(tuple -> {
              User u = tuple.getT1();
              Comments c = tuple.getT2();
              return new UserComment(u, c);
            });

    userCommentMono .subscribe(uc -> logger.info(uc.toString()));

  }

  public void exampleUserCommentZipWith() {

    Mono<User> userMono = Mono.fromCallable(() -> new User("Vicse", "Ore"));

    Mono<Comments> commentsMono = Mono.fromCallable(() -> {
      Comments comments = new Comments();
      comments.addComment("Hola, Como estas");
      comments.addComment("Heyy, holaaa");
      comments.addComment("Que tal pproo");
      return comments;
    });

    Mono<UserComment> userCommentMono = userMono
            .zipWith(commentsMono, UserComment::new);

    userCommentMono .subscribe(uc -> logger.info(uc.toString()));

  }

  public void exampleUserCommentFlatMap() {

    Mono<User> userMono = Mono.fromCallable(() -> new User("Vicse", "Ore"));

    Mono<Comments> commentsMono = Mono.fromCallable(() -> {
      Comments comments = new Comments();
      comments.addComment("Hola, Como estas");
      comments.addComment("Heyy, holaaa");
      comments.addComment("Que tal pproo");
      return comments;
    });

    Mono<UserComment> userCommentMono = userMono.flatMap(u -> commentsMono.map(c -> new UserComment(u, c)));
    userCommentMono.subscribe(uc -> logger.info(uc.toString()));

  }

  public void exampleCollectList() throws Exception {

    List<User> usersList = new ArrayList<>();
    usersList.add(new User("Vicse", "Ore"));
    usersList.add(new User("Maria", "Estrada"));
    usersList.add(new User("Carlos", "Soto"));
    usersList.add(new User("Cristian", "Mendoza"));
    usersList.add(new User("Bruce", "Lee"));
    usersList.add(new User("Bruce", "Willi"));

    Flux.fromIterable(usersList)
            .collectList()
            .subscribe(list -> list.forEach(item -> logger.info(item.toString())));

  }

  public void exampleToString() throws Exception {

    List<User> usersList = new ArrayList<>();
    usersList.add(new User("Vicse", "Ore"));
    usersList.add(new User("Maria", "Estrada"));
    usersList.add(new User("Carlos", "Soto"));
    usersList.add(new User("Cristian", "Mendoza"));
    usersList.add(new User("Bruce", "Lee"));
    usersList.add(new User("Bruce", "Willi"));

    Flux.fromIterable(usersList)
            .map(user ->  user.getName().toUpperCase().concat(" ").concat(user.getSurname().toUpperCase()))
            .flatMap(name -> {
              if (name.contains("bruce".toUpperCase())) {
                return Mono.just(name);
              } else {
                return Mono.empty();
              }
            })
            .map(String::toLowerCase)
            .subscribe(logger::info);

  }

  public void exampleIterable() throws Exception {

    List<String> usersList = new ArrayList<>();
    usersList.add("Vicse Ore");
    usersList.add("Maria Estrada");
    usersList.add("Carlos Soto");
    usersList.add("Cristian Mendoza");
    usersList.add("Bruce Lee");
    usersList.add("Bruce Willi");

    Flux<String> names = Flux.fromIterable(usersList);/*Flux.just("Vicse Ore", "Maria Estrada","Carlos Soto", "Cristian Mendoza", "Bruce Lee", "Bruce Willi"); */

    Flux<User> users = names.map(name -> new User(name.split(" ")[0].toUpperCase(), name.split(" ")[1].toUpperCase()))
            .filter(user -> user.getName().equalsIgnoreCase("bruce"))
            .doOnNext(user -> {
              if (user == null) {
                throw new RuntimeException("Names can not be empty");
              }

              System.out.println(user.getName().concat(" ").concat(user.getSurname()));

            })
            .map(user -> {
              String newName = user.getName().toLowerCase();
              user.setName(newName);
              return user;
            })
            ;

    users.subscribe(
            e -> logger.info(e.toString()),
            error -> logger.error(error.getMessage()),

            new Runnable() {
              @Override
              public void run() {
                logger.info("The observable has been successfully completed");
              }
            }
    );

  }

  public void exampleFlatMap() throws Exception {

    List<String> usersList = new ArrayList<>();
    usersList.add("Vicse Ore");
    usersList.add("Maria Estrada");
    usersList.add("Carlos Soto");
    usersList.add("Cristian Mendoza");
    usersList.add("Bruce Lee");
    usersList.add("Bruce Willi");

    Flux.fromIterable(usersList)
            .map(name -> new User(name.split(" ")[0].toUpperCase(), name.split(" ")[1].toUpperCase()))
            .flatMap(user -> {
              if (user.getName().equalsIgnoreCase("bruce")) {
                return Mono.just(user);
              } else {
                return Mono.empty();
              }
            })
            .map(user -> {
              String newName = user.getName().toLowerCase();
              user.setName(newName);
              return user;
            }).subscribe(u -> logger.info(u.toString()));
  }


}
