package com.mmo.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The actual service connection (The Bridge)
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: game_bridge.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AIEngineGrpc {

  private AIEngineGrpc() {}

  public static final java.lang.String SERVICE_NAME = "mmo.AIEngine";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mmo.grpc.GameBridgeProto.GameState,
      com.mmo.grpc.GameBridgeProto.BotScript> getSendStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendState",
      requestType = com.mmo.grpc.GameBridgeProto.GameState.class,
      responseType = com.mmo.grpc.GameBridgeProto.BotScript.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mmo.grpc.GameBridgeProto.GameState,
      com.mmo.grpc.GameBridgeProto.BotScript> getSendStateMethod() {
    io.grpc.MethodDescriptor<com.mmo.grpc.GameBridgeProto.GameState, com.mmo.grpc.GameBridgeProto.BotScript> getSendStateMethod;
    if ((getSendStateMethod = AIEngineGrpc.getSendStateMethod) == null) {
      synchronized (AIEngineGrpc.class) {
        if ((getSendStateMethod = AIEngineGrpc.getSendStateMethod) == null) {
          AIEngineGrpc.getSendStateMethod = getSendStateMethod =
              io.grpc.MethodDescriptor.<com.mmo.grpc.GameBridgeProto.GameState, com.mmo.grpc.GameBridgeProto.BotScript>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mmo.grpc.GameBridgeProto.GameState.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mmo.grpc.GameBridgeProto.BotScript.getDefaultInstance()))
              .setSchemaDescriptor(new AIEngineMethodDescriptorSupplier("SendState"))
              .build();
        }
      }
    }
    return getSendStateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AIEngineStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AIEngineStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AIEngineStub>() {
        @java.lang.Override
        public AIEngineStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AIEngineStub(channel, callOptions);
        }
      };
    return AIEngineStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AIEngineBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AIEngineBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AIEngineBlockingStub>() {
        @java.lang.Override
        public AIEngineBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AIEngineBlockingStub(channel, callOptions);
        }
      };
    return AIEngineBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AIEngineFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AIEngineFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AIEngineFutureStub>() {
        @java.lang.Override
        public AIEngineFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AIEngineFutureStub(channel, callOptions);
        }
      };
    return AIEngineFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The actual service connection (The Bridge)
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void sendState(com.mmo.grpc.GameBridgeProto.GameState request,
        io.grpc.stub.StreamObserver<com.mmo.grpc.GameBridgeProto.BotScript> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendStateMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AIEngine.
   * <pre>
   * The actual service connection (The Bridge)
   * </pre>
   */
  public static abstract class AIEngineImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AIEngineGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AIEngine.
   * <pre>
   * The actual service connection (The Bridge)
   * </pre>
   */
  public static final class AIEngineStub
      extends io.grpc.stub.AbstractAsyncStub<AIEngineStub> {
    private AIEngineStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AIEngineStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AIEngineStub(channel, callOptions);
    }

    /**
     */
    public void sendState(com.mmo.grpc.GameBridgeProto.GameState request,
        io.grpc.stub.StreamObserver<com.mmo.grpc.GameBridgeProto.BotScript> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendStateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AIEngine.
   * <pre>
   * The actual service connection (The Bridge)
   * </pre>
   */
  public static final class AIEngineBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AIEngineBlockingStub> {
    private AIEngineBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AIEngineBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AIEngineBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.mmo.grpc.GameBridgeProto.BotScript sendState(com.mmo.grpc.GameBridgeProto.GameState request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendStateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AIEngine.
   * <pre>
   * The actual service connection (The Bridge)
   * </pre>
   */
  public static final class AIEngineFutureStub
      extends io.grpc.stub.AbstractFutureStub<AIEngineFutureStub> {
    private AIEngineFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AIEngineFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AIEngineFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mmo.grpc.GameBridgeProto.BotScript> sendState(
        com.mmo.grpc.GameBridgeProto.GameState request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendStateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_STATE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_STATE:
          serviceImpl.sendState((com.mmo.grpc.GameBridgeProto.GameState) request,
              (io.grpc.stub.StreamObserver<com.mmo.grpc.GameBridgeProto.BotScript>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mmo.grpc.GameBridgeProto.GameState,
              com.mmo.grpc.GameBridgeProto.BotScript>(
                service, METHODID_SEND_STATE)))
        .build();
  }

  private static abstract class AIEngineBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AIEngineBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mmo.grpc.GameBridgeProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AIEngine");
    }
  }

  private static final class AIEngineFileDescriptorSupplier
      extends AIEngineBaseDescriptorSupplier {
    AIEngineFileDescriptorSupplier() {}
  }

  private static final class AIEngineMethodDescriptorSupplier
      extends AIEngineBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AIEngineMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AIEngineGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AIEngineFileDescriptorSupplier())
              .addMethod(getSendStateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
