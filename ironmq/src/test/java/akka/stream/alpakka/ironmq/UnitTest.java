/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ironmq;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static scala.collection.JavaConverters.*;
import static scala.compat.java8.FutureConverters.*;

public abstract class UnitTest {

    private ActorSystem system;
    private ActorMaterializer materializer;
    private Config config;
    private IronMqClient ironMqClient;

    @Before
    public void setup() throws Exception {
        config = initConfig();
        system = ActorSystem.create("TestActorSystem", config);
        materializer = ActorMaterializer.create(system);
        ironMqClient = new IronMqClient(IronMqSettings.create(config.getConfig("akka.stream.alpakka.ironmq")), system, materializer);

    }

    @After
    public void teardown() throws Exception {
        materializer.shutdown();
        JavaTestKit.shutdownActorSystem(system);
    }

    protected Config initConfig() {
        String projectId = "project-" + System.currentTimeMillis();
        return ConfigFactory.parseString("akka.stream.alpakka.ironmq.credentials.project-id = " + projectId).withFallback(ConfigFactory.load());
    }

    protected ActorSystem getActorSystem() {
        if(system == null) throw new IllegalStateException("The ActorSystem is not yet initialized");
        return system;
    }

    protected Materializer getMaterializer() {
        if(materializer == null) throw new IllegalStateException("The Materializer is not yet initialized");
        return materializer;
    }

    public IronMqClient getIronMqClient() {
        if(ironMqClient == null) throw new IllegalStateException("The IronMqClient is not yet initialized");
        return ironMqClient;
    }

    protected Queue givenQueue() {
        return givenQueue("test-" + UUID.randomUUID());
    }

    protected Queue givenQueue(String name) {
        try {
            return toJava(ironMqClient.createQueue(name, system.dispatcher())).toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Message.Ids givenMessages(String queueName, int n) {


        List<PushMessage> messages = IntStream.rangeClosed(1, n)
                .mapToObj(i -> PushMessage.create("test-" + i))
                .collect(Collectors.toList());

        try {
            return toJava(ironMqClient.pushMessages(queueName, asScalaBufferConverter(messages).asScala().toSeq(), system.dispatcher())).toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }

}
