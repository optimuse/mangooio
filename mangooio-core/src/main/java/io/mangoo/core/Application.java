package io.mangoo.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronExpression;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.mangoo.admin.AdminController;
import io.mangoo.annotations.Schedule;
import io.mangoo.cache.Cache;
import io.mangoo.configuration.Config;
import io.mangoo.configuration.ConfigFactory;
import io.mangoo.core.yaml.YamlRoute;
import io.mangoo.core.yaml.YamlRouter;
import io.mangoo.enums.CacheName;
import io.mangoo.enums.Default;
import io.mangoo.enums.Jvm;
import io.mangoo.enums.Key;
import io.mangoo.enums.Mode;
import io.mangoo.enums.Required;
import io.mangoo.enums.RouteType;
import io.mangoo.exceptions.MangooSchedulerException;
import io.mangoo.interfaces.MangooLifecycle;
import io.mangoo.providers.CacheProvider;
import io.mangoo.routing.Route;
import io.mangoo.routing.Router;
import io.mangoo.routing.handlers.DispatcherHandler;
import io.mangoo.routing.handlers.ExceptionHandler;
import io.mangoo.routing.handlers.FallbackHandler;
import io.mangoo.routing.handlers.MetricsHandler;
import io.mangoo.routing.handlers.ServerSentEventHandler;
import io.mangoo.routing.handlers.WebSocketHandler;
import io.mangoo.scheduler.Scheduler;
import io.mangoo.utils.BootstrapUtils;
import io.mangoo.utils.CryptoUtils;
import io.mangoo.utils.SchedulerUtils;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

/**
 * Main class that starts all components of a mangoo I/O application
 *
 * @author svenkubiak
 *
 */
public final class Application {
    private static Logger LOG; //NOSONAR
    private static final int BUFFERSIZE = 255;
    private static volatile String httpHost;
    private static volatile String ajpHost;
    private static volatile int httpPort;
    private static volatile int ajpPort;
    private static volatile Undertow undertow;
    private static volatile Mode mode;
    private static volatile Injector injector;
    private static volatile LocalDateTime start = LocalDateTime.now();
    private static volatile boolean started;
    private static volatile boolean error;
    private static volatile PathHandler pathHandler;
    private static volatile ResourceHandler resourceHandler;

    private Application() {
    }

    public static void main(String... args) {
        start(Mode.PROD);
    }

    public static void start(Mode mode) {
        Objects.requireNonNull(mode, Required.MODE.toString());

        if (!started) {
            prepareMode(mode);
            System.setProperty("log4j.configurationFactory", ConfigFactory.class.getName());

            resourceHandler = Handlers.
                    resource(new ClassPathResourceManager(Thread.currentThread().getContextClassLoader(), Default.FILES_FOLDER.toString() + '/'));

            prepareLogger();
            prepareInjector();
            applicationInitialized();
            prepareConfig();
            prepareRoutes();
            createRoutes();
            prepareScheduler();
            prepareUndertow();

            if (!error) {
                sanityChecks();
                showLogo();
                applicationStarted();
                Runtime.getRuntime().addShutdownHook(getInstance(Shutdown.class));
                started = true;
            } else {
                System.out.print("Failed to start mangoo I/O application"); //NOSONAR
                System.exit(1); //NOSONAR
            }
        }
    }

    /**
     * Checks if the application is running in dev mode
     *
     * @return True if the application is running in dev mode, false otherwise
     */
    public static boolean inDevMode() {
        return Mode.DEV == mode;
    }

    /**
     * Checks if the application is running in prod mode
     *
     * @return True if the application is running in prod mode, false otherwise
     */
    public static boolean inProdMode() {
        return Mode.PROD == mode;
    }

    /**
     * Checks if the application is running in test mode
     *
     * @return True if the application is running in test mode, false otherwise
     */
    public static boolean inTestMode() {
        return Mode.TEST == mode;
    }

    /**
     * Returns the current mode the application is running in
     *
     * @return Enum Mode
     */
    public static Mode getMode() {
        return mode;
    }

    /**
     * Returns the Google Guice Injector
     *
     * @return Google Guice injector instance
     */
    public static Injector getInjector() {
        return injector;
    }

    /**
     * @return True if the application started successfully, false otherwise
     */
    public static boolean isStarted() {
        return started;
    }

    /**
     * @return The LocalDateTime of the application start
     */
    public static LocalDateTime getStart() {
        return start;
    }

    /**
     * @return The duration of the application uptime
     */
    public static Duration getUptime() {
        Objects.requireNonNull(start, Required.START.toString());

        return Duration.between(start, LocalDateTime.now());
    }

    /**
     * Short form for getting an Goolge Guice injected class by
     * calling injector.getInstance(...)
     *
     * @param clazz The class to retrieve from the injector
     * @param <T> JavaDoc requires this (just ignore it)
     *
     * @return An instance of the requested class
     */
    public static <T> T getInstance(Class<T> clazz) {
        Objects.requireNonNull(clazz, Required.CLASS.toString());

        return injector.getInstance(clazz);
    }

    /**
     * Stops the underlying undertow server
     */
    public static void stopUndertow() {
        undertow.stop();
    }

    /**
     * Sets the mode the application is running in
     *
     * @param providedMode A given mode or null
     */
    private static void prepareMode(Mode providedMode) {
        final String applicationMode = System.getProperty(Jvm.APPLICATION_MODE.toString());
        if (StringUtils.isNotBlank(applicationMode)) {
            switch (applicationMode.toLowerCase(Locale.ENGLISH)) {
                case "dev"  : mode = Mode.DEV;
                break;
                case "test" : mode = Mode.TEST;
                break;
                default     : mode = Mode.PROD;
                break;
            }
        } else {
            mode = providedMode;
        }
    }

    /**
     * Sets the injector wrapped through netflix Governator
     */
    private static void prepareInjector() {
        if (!error) {
            injector = LifecycleInjector.builder()
                    .withModules(getModules())
                    .usingBasePackages(".")
                    .build()
                    .createInjector();

            try {
                injector.getInstance(LifecycleManager.class).start();
            } catch (Exception e) {
                LOG.error("Failed to start Governator LifecycleManager", e);
                error = true;
            }
        }
    }

    /**
     * Callback to MangooLifecycle applicationInitialized
     */
    private static void applicationInitialized() {
        if (!error) {
            injector.getInstance(MangooLifecycle.class).applicationInitialized();
        }
    }

    /**
     * Checks for config failures that pervent the application from starting
     */
    private static void prepareConfig() {
        if (!error) {
            Config config = injector.getInstance(Config.class);
            if (!CryptoUtils.isValidSecret(config.getApplicationSecret())) {
                LOG.error("Please make sure that your application.yaml has an application.secret property which has at least 32 characters");
                error = true;
            }

            if (!config.isDecrypted()) {
                LOG.error("Found encrypted config values in application.yaml but decryption was not successful!");
                error = true;
            }
        }
    }

    /**
     * Do sanity checks on the configuration an warn about it in the log
     */
    private static void sanityChecks() {
        if (!error) {
            Config config = injector.getInstance(Config.class);
            Cache cache = injector.getInstance(CacheProvider.class).getCache(CacheName.APPLICATION);
            List<String> warnings = new ArrayList<>();
            if (!config.isAuthenticationCookieSecure()) {
                String warning = "Authentication cookie has secure flag set to false. It is highly recommended to set auth.cookie.secure to true.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (config.getAuthenticationCookieName().equals(Default.AUTHENTICATION_COOKIE_NAME.toString())) {
                String warning = "Authentication cookie name has default value. Consider changeing auth.cookie.name to an application specific value.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (!config.isSessionCookieSecure()) {
                String warning = "Session cookie has secure flag set to false. It is highly recommended to set cookie.secure to true.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (config.getSessionCookieName().equals(Default.SESSION_COOKIE_NAME.toString())) {
                String warning = "Session cookie name has default value. Consider changeing cookie.name to an application specific value.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (config.getSessionCookieSignKey().equals(config.getApplicationSecret())) {
                String warning = "Session cookie sign key is using application secret. It is highly recommend to set a dedicated value to session.cookie.signkey.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (config.isSessionCookieEncrypt() && config.getSessionCookieEncryptionKey().equals(config.getApplicationSecret())) {
                String warning = "Session cookie encryption is enabled and encryption is using application secret. It is highly recommend to set a dedicated value to session.cookie.encryptionkey.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (!CryptoUtils.isValidSecret(config.getSessionCookieSignKey())) {
                String warning = "Session cookie sign key is not a valid key. A valid sign key has to be at least 32 characters.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (config.isSessionCookieEncrypt() && !CryptoUtils.isValidSecret(config.getSessionCookieEncryptionKey())) {
                String warning = "Session cookie encryption is enabled and encryption key is not a valid secret. A valid key has to be at least 32 characters.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (!CryptoUtils.isValidSecret(config.getAuthenticationCookieSignKey())) {
                String warning = "Authentication cookie sign key is not a valid key. A valid sign key has to be at least 32 characters.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            if (config.isAuthenticationCookieEncrypt() && !CryptoUtils.isValidSecret(config.getAuthenticationCookieEncryptionKey())) {
                String warning = "Authentication cookie encryption is enabled and encryption key is not a valid key. A valid key has to be at least 32 characters.";
                warnings.add(warning);
                LOG.warn(warning);
            }

            cache.put("mangooio-warnings", warnings);
        }
    }

    /**
     * Parse routes from routes.yaml file and set up dispatcher
     */
    private static void prepareRoutes() {
        if (!error) {
            Config config = injector.getInstance(Config.class);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            YamlRouter yamlRouter = null;
            try {
                yamlRouter = objectMapper.readValue(Resources.getResource(Default.ROUTES_FILE.toString()).openStream(), YamlRouter.class);
            } catch (IOException e) {
                LOG.error("Failed to load routes.yaml Please make sure that your routes.yaml exists in your application src/main/resources folder", e);
                error = true;
            }

            if (!error && yamlRouter != null) {
                for (final YamlRoute yamlRoute : yamlRouter.getRoutes()) {
                    RouteType routeType = BootstrapUtils.getRouteType(yamlRoute.getMethod());
                    final Route route = new Route(routeType)
                            .toUrl(yamlRoute.getUrl().trim())
                            .withRequest(HttpString.tryFromString(yamlRoute.getMethod()))
                            .withUsername(yamlRoute.getUsername())
                            .withPassword(yamlRoute.getPassword())
                            .withAuthentication(yamlRoute.isAuthentication())
                            .withTimer(yamlRoute.isTimer())
                            .withLimit(yamlRoute.getLimit())
                            .allowBlocking(yamlRoute.isBlocking());

                    try {
                        String mapping = yamlRoute.getMapping();
                        if (StringUtils.isNotBlank(mapping)) {
                            if (routeType == RouteType.REQUEST) {
                                int lastIndexOf = mapping.trim().lastIndexOf('.');
                                String controllerClass = BootstrapUtils.getPackageName(config.getControllerPackage()) + mapping.substring(0, lastIndexOf);
                                route.withClass(Class.forName(controllerClass));

                                String methodName = mapping.substring(lastIndexOf + 1);
                                if (BootstrapUtils.methodExists(methodName, route.getControllerClass())) {
                                    route.withMethod(methodName);
                                } else {
                                    error = true;
                                }
                            } else {
                                route.withClass(Class.forName(BootstrapUtils.getPackageName(config.getControllerPackage()) + mapping));
                            }
                        }
                       Router.addRoute(route);
                    } catch (final ClassNotFoundException e) {
                        LOG.error("Failed to create routes from routes.yaml");
                        LOG.error("Please verify that your routes.yaml mapping is correct", e);
                        error = true;
                    }
                }
            }
        }
    }

    /**
     * Create routes for WebSockets ServerSentEvent and Resource files
     */
    private static void createRoutes() {
        if (!error) {
            pathHandler = new PathHandler(getRoutingHandler());
            for (final Route route : Router.getRoutes()) {
                if (RouteType.WEBSOCKET == route.getRouteType()) {
                    pathHandler.addExactPath(route.getUrl(),
                            Handlers.websocket(injector.getInstance(WebSocketHandler.class)
                                    .withControllerClass(route.getControllerClass())
                                    .withAuthentication(route.isAuthenticationRequired())));
                } else if (RouteType.SERVER_SENT_EVENT == route.getRouteType()) {
                    pathHandler.addExactPath(route.getUrl(),
                            Handlers.serverSentEvents(injector.getInstance(ServerSentEventHandler.class)
                                    .withAuthentication(route.isAuthenticationRequired())));
                } else if (RouteType.RESOURCE_PATH == route.getRouteType()) {
                    pathHandler.addPrefixPath(route.getUrl(),
                            new ResourceHandler(new ClassPathResourceManager(Thread.currentThread().getContextClassLoader(), Default.FILES_FOLDER.toString() + route.getUrl())));
                }
            }
        }
    }

    private static RoutingHandler getRoutingHandler() {
        final RoutingHandler routingHandler = Handlers.routing();
        routingHandler.setFallbackHandler(Application.getInstance(FallbackHandler.class));

        Config config = injector.getInstance(Config.class);
        if (config.isAdminEnabled()) {
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin").withRequest(Methods.GET).withClass(AdminController.class).withMethod("index").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/health").withRequest(Methods.GET).withClass(AdminController.class).withMethod("health").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/scheduler").withRequest(Methods.GET).withClass(AdminController.class).withMethod("scheduler").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/logger").withRequest(Methods.GET).withClass(AdminController.class).withMethod("logger").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/logger/ajax").withRequest(Methods.POST).withClass(AdminController.class).withMethod("loggerajax").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/routes").withRequest(Methods.GET).withClass(AdminController.class).withMethod("routes").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/metrics").withRequest(Methods.GET).withClass(AdminController.class).withMethod("metrics").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/metrics/reset").withRequest(Methods.GET).withClass(AdminController.class).withMethod("resetMetrics").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/tools").withRequest(Methods.GET).withClass(AdminController.class).withMethod("tools").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/tools/ajax").withRequest(Methods.POST).withClass(AdminController.class).withMethod("toolsajax").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/scheduler/execute/{name}").withRequest(Methods.GET).withClass(AdminController.class).withMethod("execute").useInternalTemplateEngine());
            Router.addRoute(new Route(RouteType.REQUEST).toUrl("/@admin/scheduler/state/{name}").withRequest(Methods.GET).withClass(AdminController.class).withMethod("state").useInternalTemplateEngine());
        }

        Router.getRoutes().parallelStream().forEach((Route route) -> {
            if (RouteType.REQUEST == route.getRouteType()) {
                DispatcherHandler dispatcherHandler = Application.getInstance(DispatcherHandler.class).dispatch(route.getControllerClass(), route.getControllerMethod())
                        .isBlocking(route.isBlockingAllowed())
                        .withTimer(route.isTimerEnabled())
                        .withUsername(route.getUsername())
                        .withPassword(route.getPassword())
                        .withLimit(route.getLimit());

                routingHandler.add(route.getRequestMethod(),route.getUrl(), dispatcherHandler);
            } else if (RouteType.RESOURCE_FILE == route.getRouteType()) {
                routingHandler.add(Methods.GET, route.getUrl(), resourceHandler);
            }
        });

        return routingHandler;
    }

    @SuppressWarnings("all")
    private static void prepareLogger() {
        LOG = LogManager.getLogger(Application.class);
        LOG.info(System.getProperty(Key.LOGGER_MESSAGE.toString()));
    }

    private static void prepareUndertow() {
        if (!error) {
            Config config = injector.getInstance(Config.class);

            HttpHandler httpHandler;
            if (config.isMetricsEnabled()) {
                httpHandler = MetricsHandler.HANDLER_WRAPPER.wrap(Handlers.exceptionHandler(pathHandler)
                        .addExceptionHandler(Throwable.class, Application.getInstance(ExceptionHandler.class)));
            } else {
                httpHandler = Handlers.exceptionHandler(pathHandler)
                        .addExceptionHandler(Throwable.class, Application.getInstance(ExceptionHandler.class));
            }

            Builder builder = Undertow.builder()
                    .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, config.getUndertowMaxEntitySize())
                    .setHandler(httpHandler);

            httpHost = config.getConnectorHttpHost();
            httpPort = config.getConnectorHttpPort();
            ajpHost = config.getConnectorAjpHost();
            ajpPort = config.getConnectorAjpPort();

            boolean hasConnector = false;
            if (httpPort > 0 && StringUtils.isNotBlank(httpHost)) {
                builder.addHttpListener(httpPort, httpHost);
                hasConnector = true;
            }

            if (ajpPort > 0 && StringUtils.isNotBlank(ajpHost)) {
                builder.addAjpListener(ajpPort, ajpHost);
                hasConnector = true;
            }

            if (hasConnector) {
                undertow = builder.build();
                undertow.start();
            } else {
                LOG.error("No connector found! Please configure a HTTP and/or AJP connector in your application.yaml");
                error = true;
            }
        }
    }

    private static void showLogo() {
        if (!error) {
            final StringBuilder buffer = new StringBuilder(BUFFERSIZE);
            buffer.append('\n')
                .append(BootstrapUtils.getLogo())
                .append("\n\nhttps://mangoo.io | @mangoo_io | ")
                .append(BootstrapUtils.getVersion())
                .append('\n');

            LOG.info(buffer.toString()); //NOSONAR

            if (httpPort > 0 && StringUtils.isNotBlank(httpHost)) {
                LOG.info("HTTP connector listening @{}:{}", httpHost, httpPort);
            }

            if (ajpPort > 0 && StringUtils.isNotBlank(ajpHost)) {
                LOG.info("AJP connector listening @{}:{}", ajpHost, ajpPort);
            }

            LOG.info("mangoo I/O application started in {} ms in {} mode. Enjoy.", ChronoUnit.MILLIS.between(start, LocalDateTime.now()), mode.toString());
        }
    }

    private static List<Module> getModules() {
        final List<Module> modules = new ArrayList<>();
        try {
            final Class<?> applicationModule = Class.forName(Default.MODULE_CLASS.toString());
            modules.add(new io.mangoo.core.Module());
            modules.add((AbstractModule) applicationModule.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            LOG.error("Failed to load modules. Check that conf/Module.java exists in your application", e);
            error = true;
        }

        return modules;
    }

    private static void applicationStarted() {
        if (!error) {
            injector.getInstance(MangooLifecycle.class).applicationStarted();
        }
    }

    private static void prepareScheduler() {
        if (!error) {
            Config config = injector.getInstance(Config.class);

            List<Class<?>> jobs = new ArrayList<>();
            new FastClasspathScanner(config.getSchedulerPackage())
                .matchClassesWithAnnotation(Schedule.class, jobs::add)
                .scan();

            if (!jobs.isEmpty() && config.isSchedulerAutostart()) {
                final Scheduler mangooScheduler = injector.getInstance(Scheduler.class);
                mangooScheduler.initialize();

                for (Class<?> clazz : jobs) {
                    final Schedule schedule = clazz.getDeclaredAnnotation(Schedule.class);
                    if (CronExpression.isValidExpression(schedule.cron())) {
                        final JobDetail jobDetail = SchedulerUtils.createJobDetail(clazz.getName(), Default.SCHEDULER_JOB_GROUP.toString(), clazz.asSubclass(Job.class));
                        final Trigger trigger = SchedulerUtils.createTrigger(clazz.getName() + "-trigger", Default.SCHEDULER_TRIGGER_GROUP.toString(), schedule.description(), schedule.cron());
                        try {
                            mangooScheduler.schedule(jobDetail, trigger);
                        } catch (MangooSchedulerException e) {
                            LOG.error("Failed to add a job to the scheduler", e);
                        }
                        LOG.info("Successfully scheduled job " + clazz.getName() + " with cron " + schedule.cron());
                    } else {
                        LOG.error("Invalid or missing cron expression for job: " + clazz.getName());
                        error = true;
                    }
                }

                if (!error) {
                    try {
                        mangooScheduler.start();
                    } catch (MangooSchedulerException e) {
                        LOG.error("Failed to start the scheduler", e);
                        error = true;
                    }
                }
            }
        }
    }
}
