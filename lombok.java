PS C:\Users\USER\Desktop\InventAlert\notificationService> ./mvnw test
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------< com.inventalert:notificationService >-----------------
[INFO] Building notificationService 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ notificationService ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.14.1:compile (default-compile) @ notificationService ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 23 source files with javac [debug parameters release 25] to target\classes
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit
WARNING: Please consider reporting this to the maintainers of class lombok.permit.Permit
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
[INFO] /C:/Users/USER/Desktop/InventAlert/notificationService/src/main/java/com/inventalert/notificationService/security/filter/JwtAuthFilter.java: C:\Users\USER\Desktop\InventAlert\notificationService\src\main\java\com\inventalert\notificationService\security\filter\JwtAuthFilter.java uses or overrides a deprecated API.
[INFO] /C:/Users/USER/Desktop/InventAlert/notificationService/src/main/java/com/inventalert/notificationService/security/filter/JwtAuthFilter.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ notificationService ---
[INFO] Copying 1 resource from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.14.1:testCompile (default-testCompile) @ notificationService ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 9 source files with javac [debug parameters release 25] to target\test-classes
[INFO] /C:/Users/USER/Desktop/InventAlert/notificationService/src/test/java/com/inventalert/notificationService/NotificationEventConsumerIT.java: C:\Users\USER\Desktop\InventAlert\notificationService\src\test\java\com\inventalert\notificationService\NotificationEventConsumerIT.java uses or overrides a deprecated API.
[INFO] /C:/Users/USER/Desktop/InventAlert/notificationService/src/test/java/com/inventalert/notificationService/NotificationEventConsumerIT.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- surefire:3.5.5:test (default-test) @ notificationService ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.inventalert.notificationService.consumer.NotificationEventConsumerTest
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (C:\Users\USER\.m2\repository\net\bytebuddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
10:50:46.890 [main] ERROR com.inventalert.notificationService.consumer.NotificationEventConsumer -- Failed to parse notification event: Unexpected character ('n' (code 110)): was expecting double-quote to start field name
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 2]
10:50:46.904 [main] ERROR com.inventalert.notificationService.consumer.NotificationEventConsumer -- Failed to parse notification event: Cannot deserialize value of type `com.inventalert.notificationService.model.NotificationType` from String "UNKNOWN_EVENT_TYPE": not one of the values accepted for Enum class: [RESTOCK_ALERT, RECONCILIATION_REQUESTED, TRANSFER_SUGGESTION, TRANSFER_APPROVED, TRANSFER_REJECTED, TRANSFER_ACCEPTED]
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 6, column: 11] (through reference chain: com.inventalert.notificationService.dto.event.NotificationEvent["type"])
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.926 s -- in com.inventalert.notificationService.consumer.NotificationEventConsumerTest
[INFO] Running com.inventalert.notificationService.controller.NotificationControllerTest
10:50:47.152 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.inventalert.notificationService.controller.NotificationControllerTest]: NotificationControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
10:50:47.439 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.inventalert.notificationService.NotificationServiceApplication for test class com.inventalert.notificationService.controller.NotificationControllerTest
10:50:47.444 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.inventalert.notificationService.controller.NotificationControllerTest]: NotificationControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
10:50:47.467 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.inventalert.notificationService.NotificationServiceApplication for test class com.inventalert.notificationService.controller.NotificationControllerTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v4.0.6)

2026-05-11T10:50:48.165+01:00  INFO 16420 --- [           main] c.i.n.c.NotificationControllerTest       : Starting NotificationControllerTest using Java 25.0.1 with PID 16420 (started by USER in C:\Users\USER\Desktop\InventAlert\notificationService)
2026-05-11T10:50:48.167+01:00  INFO 16420 --- [           main] c.i.n.c.NotificationControllerTest       : No active profile set, falling back to 1 default profile: "default"
2026-05-11T10:50:51.316+01:00  INFO 16420 --- [           main] o.s.b.t.m.w.SpringBootMockServletContext : Initializing Spring TestDispatcherServlet ''
2026-05-11T10:50:51.316+01:00  INFO 16420 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Initializing Servlet ''
2026-05-11T10:50:51.320+01:00  INFO 16420 --- [           main] o.s.t.web.servlet.TestDispatcherServlet  : Completed initialization in 3 ms
2026-05-11T10:50:51.377+01:00  INFO 16420 --- [           main] c.i.n.c.NotificationControllerTest       : Started NotificationControllerTest in 3.781 seconds (process running for 7.95)
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.086 s -- in com.inventalert.notificationService.controller.NotificationControllerTest
[INFO] Running com.inventalert.notificationService.NotificationServiceApplicationTests
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.002 s -- in com.inventalert.notificationService.NotificationServiceApplicationTests
[INFO] Running com.inventalert.notificationService.security.WebSocketAuthInterceptorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.260 s -- in com.inventalert.notificationService.security.WebSocketAuthInterceptorTest
[INFO] Running com.inventalert.notificationService.service.EmailServiceTest
2026-05-11T10:50:52.473+01:00 ERROR 16420 --- [           main] c.i.n.service.impl.EmailServiceImpl      : Email delivery failed after 3 attempts for emeka@heritage.ng: SMTP server unavailable
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.166 s -- in com.inventalert.notificationService.service.EmailServiceTest
[INFO] Running com.inventalert.notificationService.service.NotificationBroadcasterTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.185 s -- in com.inventalert.notificationService.service.NotificationBroadcasterTest
[INFO] Running com.inventalert.notificationService.service.NotificationServiceTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.305 s -- in com.inventalert.notificationService.service.NotificationServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 35, Failures: 0, Errors: 0, Skipped: 1
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  21.458 s
[INFO] Finished at: 2026-05-11T10:50:53+01:00
[INFO] ------------------------------------------------------------------------
PS C:\Users\USER\Desktop\InventAlert\notificationService> cd ..
PS C:\Users\USER\Desktop\InventAlert> cd .\identityService\
PS C:\Users\USER\Desktop\InventAlert\identityService> ./mvnw test          
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< com.inventalert:identityService >-------------------
[INFO] Building identityService 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ identityService ---
[INFO] Copying 1 resource from src\main\resources to target\classes
[INFO] Copying 6 resources from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.14.1:compile (default-compile) @ identityService ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ identityService ---
[INFO] Copying 1 resource from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.14.1:testCompile (default-testCompile) @ identityService ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 14 source files with javac [debug parameters release 25] to target\test-classes
WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
WARNING: sun.misc.Unsafe::objectFieldOffset has been called by lombok.permit.Permit
WARNING: Please consider reporting this to the maintainers of class lombok.permit.Permit
WARNING: sun.misc.Unsafe::objectFieldOffset will be removed in a future release
[INFO] /C:/Users/USER/Desktop/InventAlert/identityService/src/test/java/com/inventalert/identityService/IdentityServiceApplicationTests.java: Some input files use or override a deprecated API.
[INFO] /C:/Users/USER/Desktop/InventAlert/identityService/src/test/java/com/inventalert/identityService/IdentityServiceApplicationTests.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- surefire:3.5.5:test (default-test) @ identityService ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.inventalert.identityService.controller.AuthControllerTest
10:51:24.016 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.inventalert.identityService.controller.AuthControllerTest]: AuthControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
10:51:24.470 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.inventalert.identityService.IdentityServiceApplication for test class com.inventalert.identityService.controller.AuthControllerTest
10:51:24.625 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.inventalert.identityService.controller.AuthControllerTest]: AuthControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
10:51:24.645 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.inventalert.identityService.IdentityServiceApplication for test class com.inventalert.identityService.controller.AuthControllerTest
10:51:24.766 [main] INFO org.testcontainers.images.PullPolicy -- Image pull policy will be performed by: DefaultPullPolicy()
10:51:24.769 [main] INFO org.testcontainers.utility.ImageNameSubstitutor -- Image name substitution will be performed by: DefaultImageNameSubstitutor (composite of 'ConfigurationFileImageNameSubstitutor' and 'PrefixingImageNameSubstitutor')
10:51:24.849 [main] INFO org.testcontainers.DockerClientFactory -- Testcontainers version: 2.0.5
10:51:36.114 [main] INFO org.testcontainers.dockerclient.DockerClientProviderStrategy -- Found Docker environment with local Npipe socket (npipe:////./pipe/docker_engine)
10:51:36.131 [main] INFO org.testcontainers.DockerClientFactory -- Docker host IP address is localhost
10:51:36.154 [main] INFO org.testcontainers.DockerClientFactory -- Connected to docker: 
  Server Version: 29.4.2
  API Version: 1.54
  Operating System: Docker Desktop
  Total Memory: 1964 MB
  Labels: 
    com.docker.desktop.address=npipe://\\.\pipe\docker_cli
[ERROR] Tests run: 14, Failures: 0, Errors: 14, Skipped: 0, Time elapsed: 15.69 s <<< FAILURE! -- in com.inventalert.identityService.controller.AuthControllerTest
[ERROR] com.inventalert.identityService.controller.AuthControllerTest.suspendedCompany_login_returns403 -- Time elapsed: 0.037 s <<< ERROR!
java.lang.ExceptionInInitializerError
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403)
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)
Caused by: com.github.dockerjava.api.exception.BadRequestException: Status 400: {"message":"client version 1.32 is too old. Minimum supported API version is 1.40, please upgrade your client to a newer version"}

        at org.testcontainers.shaded.com.github.dockerjava.core.DefaultInvocationBuilder.execute(DefaultInvocationBuilder.java:237)
        at org.testcontainers.shaded.com.github.dockerjava.core.DefaultInvocationBuilder.get(DefaultInvocationBuilder.java:202)
        at org.testcontainers.shaded.com.github.dockerjava.core.DefaultInvocationBuilder.get(DefaultInvocationBuilder.java:74)
        at org.testcontainers.shaded.com.github.dockerjava.core.exec.InspectImageCmdExec.execute(InspectImageCmdExec.java:28)
        at org.testcontainers.shaded.com.github.dockerjava.core.exec.InspectImageCmdExec.execute(InspectImageCmdExec.java:13)
        at org.testcontainers.shaded.com.github.dockerjava.core.exec.AbstrSyncDockerCmdExec.exec(AbstrSyncDockerCmdExec.java:21)
        at org.testcontainers.shaded.com.github.dockerjava.core.command.AbstrDockerCmd.exec(AbstrDockerCmd.java:33)
        at org.testcontainers.shaded.com.github.dockerjava.core.command.InspectImageCmdImpl.exec(InspectImageCmdImpl.java:39)
        at org.testcontainers.images.LocalImagesCache.refreshCache(LocalImagesCache.java:42)
        at org.testcontainers.images.AbstractImagePullPolicy.shouldPull(AbstractImagePullPolicy.java:24)
        at org.testcontainers.images.RemoteDockerImage.resolve(RemoteDockerImage.java:79)
        at org.testcontainers.images.RemoteDockerImage.resolve(RemoteDockerImage.java:35)
        at org.testcontainers.utility.LazyFuture.getResolvedValue(LazyFuture.java:20)
        at org.testcontainers.utility.LazyFuture.get(LazyFuture.java:41)
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1306)
        ... 9 more

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.signup_missingPassword_returns400 -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.superAdminLogin_wrongPassword_returns401 -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.signup_validRequest_returns201AndToken -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.superAdminLogin_correctCredentials_returns200NoCompanyId -- Time elapsed: 0.004 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.signup_invalidEmail_returns400 -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.login_correctCredentials_returns200AndToken -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.login_unknownEmail_returns401 -- Time elapsed: 0.006 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.login_missingEmail_returns400 -- Time elapsed: 0.004 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.suspendedCompany_validTokenIsBlocked_returns403 -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.login_wrongPassword_returns401 -- Time elapsed: 0.004 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.activeCompany_withValidToken_protectedEndpointReturns2xx -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.activeCompany_login_returns200 -- Time elapsed: 0.002 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[ERROR] com.inventalert.identityService.controller.AuthControllerTest.signup_duplicateEmail_returns409 -- Time elapsed: 0.001 s <<< ERROR!
java.lang.NoClassDefFoundError: Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized0(Native Method)
        at java.base/jdk.internal.misc.Unsafe.ensureClassInitialized(Unsafe.java:1169)
        at java.base/java.lang.reflect.Constructor.acquireConstructorAccessor(Constructor.java:546)
        at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:496)
        at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
        at java.base/java.util.Optional.orElseGet(Optional.java:364)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: java.lang.ExceptionInInitializerError: Exception org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403) [in thread "main"]
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)

[INFO] Running com.inventalert.identityService.IdentityServiceApplicationTests
10:51:39.504 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.inventalert.identityService.IdentityServiceApplicationTests]: IdentityServiceApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
10:51:39.549 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.inventalert.identityService.IdentityServiceApplication for test class com.inventalert.identityService.IdentityServiceApplicationTests
10:51:39.553 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.inventalert.identityService.IdentityServiceApplicationTests]: IdentityServiceApplicationTests does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
10:51:39.567 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.inventalert.identityService.IdentityServiceApplication for test class com.inventalert.identityService.IdentityServiceApplicationTests
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.089 s <<< FAILURE! -- in com.inventalert.identityService.IdentityServiceApplicationTests
[ERROR] com.inventalert.identityService.IdentityServiceApplicationTests -- Time elapsed: 0.089 s <<< ERROR!
org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403)
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1308)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:346)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:317)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:245)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:108)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:111)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:316)
        at com.inventalert.identityService.controller.AuthControllerTest.<clinit>(AuthControllerTest.java:38)
Caused by: com.github.dockerjava.api.exception.BadRequestException: Status 400: {"message":"client version 1.32 is too old. Minimum supported API version is 1.40, please upgrade your client to a newer version"}

        at org.testcontainers.shaded.com.github.dockerjava.core.DefaultInvocationBuilder.execute(DefaultInvocationBuilder.java:237)
        at org.testcontainers.shaded.com.github.dockerjava.core.DefaultInvocationBuilder.get(DefaultInvocationBuilder.java:202)
        at org.testcontainers.shaded.com.github.dockerjava.core.DefaultInvocationBuilder.get(DefaultInvocationBuilder.java:74)
        at org.testcontainers.shaded.com.github.dockerjava.core.exec.InspectImageCmdExec.execute(InspectImageCmdExec.java:28)
        at org.testcontainers.shaded.com.github.dockerjava.core.exec.InspectImageCmdExec.execute(InspectImageCmdExec.java:13)
        at org.testcontainers.shaded.com.github.dockerjava.core.exec.AbstrSyncDockerCmdExec.exec(AbstrSyncDockerCmdExec.java:21)
        at org.testcontainers.shaded.com.github.dockerjava.core.command.AbstrDockerCmd.exec(AbstrDockerCmd.java:33)
        at org.testcontainers.shaded.com.github.dockerjava.core.command.InspectImageCmdImpl.exec(InspectImageCmdImpl.java:39)
        at org.testcontainers.images.LocalImagesCache.refreshCache(LocalImagesCache.java:42)
        at org.testcontainers.images.AbstractImagePullPolicy.shouldPull(AbstractImagePullPolicy.java:24)
        at org.testcontainers.images.RemoteDockerImage.resolve(RemoteDockerImage.java:79)
        at org.testcontainers.images.RemoteDockerImage.resolve(RemoteDockerImage.java:35)
        at org.testcontainers.utility.LazyFuture.getResolvedValue(LazyFuture.java:20)
        at org.testcontainers.utility.LazyFuture.get(LazyFuture.java:41)
        at org.testcontainers.containers.GenericContainer.getDockerImageName(GenericContainer.java:1306)
        ... 9 more

[INFO] Running com.inventalert.identityService.security.JwtUtilTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.766 s -- in com.inventalert.identityService.security.JwtUtilTest
[INFO] Running com.inventalert.identityService.service.AuthServicePasswordResetTest
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.987 s -- in com.inventalert.identityService.service.AuthServicePasswordResetTest
[INFO] Running com.inventalert.identityService.service.AuthServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.121 s -- in com.inventalert.identityService.service.AuthServiceTest
[INFO] Running com.inventalert.identityService.service.CompanyServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.inventalert.identityService.service.CompanyServiceTest
[INFO] Running com.inventalert.identityService.service.UserServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.146 s -- in com.inventalert.identityService.service.UserServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Errors: 
[ERROR]   IdentityServiceApplicationTests » ContainerFetch Can't get Docker image: RemoteDockerImage(imageName=testcontainers/ryuk:0.14.0, imagePullPolicy=DefaultPullPolicy(), imageNameSubstitutor=org.testcontainers.utility.ImageNameSubstitutor$LogWrappedImageNameSubstitutor@e280403)                                                                
[ERROR]   AuthControllerTest.activeCompany_login_returns200 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.activeCompany_withValidToken_protectedEndpointReturns2xx » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest                                                                                                                                                            
[ERROR]   AuthControllerTest.login_correctCredentials_returns200AndToken » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest                                                                                                                                                                         
[ERROR]   AuthControllerTest.login_missingEmail_returns400 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.login_unknownEmail_returns401 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.login_wrongPassword_returns401 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.signup_duplicateEmail_returns409 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.signup_invalidEmail_returns400 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.signup_missingPassword_returns400 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.signup_validRequest_returns201AndToken » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest
[ERROR]   AuthControllerTest.superAdminLogin_correctCredentials_returns200NoCompanyId » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest                                                                                                                                                            
[ERROR]   AuthControllerTest.superAdminLogin_wrongPassword_returns401 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest                                                                                                                                                                            
[ERROR]   AuthControllerTest.suspendedCompany_login_returns403 » ExceptionInInitializer
[ERROR]   AuthControllerTest.suspendedCompany_validTokenIsBlocked_returns403 » NoClassDefFound Could not initialize class com.inventalert.identityService.controller.AuthControllerTest                                                                                                                                                                     
[INFO] 
[ERROR] Tests run: 53, Failures: 0, Errors: 15, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  31.126 s
[INFO] Finished at: 2026-05-11T10:51:43+01:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.5:test (default-test) on project identityService: 
[ERROR] 
[ERROR] See C:\Users\USER\Desktop\InventAlert\identityService\target\surefire-reports for the individual test results.
[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException