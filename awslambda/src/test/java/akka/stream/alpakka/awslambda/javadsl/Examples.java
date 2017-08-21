/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.awslambda.javadsl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

public class Examples {

    //#init-mat
    ActorSystem system = ActorSystem.create();
    ActorMaterializer materializer = ActorMaterializer.create(system);
    //#init-mat

    //#init-client
    BasicAWSCredentials credentials = new BasicAWSCredentials("x", "x");
    AWSLambdaAsyncClient awsLambdaClient = new AWSLambdaAsyncClient(credentials, Executors.newFixedThreadPool(50));
    //#init-client

    //#run
    InvokeRequest request = new InvokeRequest()
            .withFunctionName("lambda-function-name")
            .withPayload("test-payload");
    Flow<InvokeRequest, InvokeResult, NotUsed> flow = AwsLambdaFlow.create(awsLambdaClient, 1);
    final CompletionStage<List<InvokeResult>> stage = Source.single(request).via(flow).runWith(Sink.seq(), materializer);
    //#run
}
