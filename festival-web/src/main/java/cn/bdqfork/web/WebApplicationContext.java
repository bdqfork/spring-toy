package cn.bdqfork.web;

import cn.bdqfork.context.AnnotationApplicationContext;
import cn.bdqfork.core.exception.BeansException;
import cn.bdqfork.core.exception.NoSuchBeanException;
import cn.bdqfork.core.factory.BeanFactory;
import cn.bdqfork.core.factory.definition.BeanDefinition;
import cn.bdqfork.core.factory.registry.BeanDefinitionRegistry;
import cn.bdqfork.web.processor.VerticleProxyProcessor;
import cn.bdqfork.web.server.DefaultWebServer;
import cn.bdqfork.web.server.WebServer;
import cn.bdqfork.web.server.WebVerticle;
import cn.bdqfork.web.service.HessianMessageCodec;
import cn.bdqfork.web.util.VertxUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * @author bdq
 * @since 2020/1/21
 */
@Slf4j
public class WebApplicationContext extends AnnotationApplicationContext {
    private static final String SERVER_OPTIONS_NAME = "serverOptions";
    private Vertx vertx;
    private Router router;

    public WebApplicationContext(String... scanPaths) throws BeansException {
        super(scanPaths);
    }

    @Override
    public void start() throws Exception {
        super.start();

        BeanFactory beanFactory = getBeanFactory();

        vertx.eventBus().registerCodec(new HessianMessageCodec());

        DeploymentOptions options = getDeploymentOptions(beanFactory);

        WebServer webServer = beanFactory.getBean(WebServer.class);

        WebVerticle webVerticle = new WebVerticle(webServer);
        vertx.deployVerticle(webVerticle, options, res -> {
            if (res.failed()) {
                if (log.isErrorEnabled()) {
                    log.error("failed to deploy web verticle!", res.cause());
                }
                vertx.close();
            }
        });
    }

    private DeploymentOptions getDeploymentOptions(BeanFactory beanFactory) throws BeansException {
        DeploymentOptions options;
        try {
            options = beanFactory.getSpecificBean(SERVER_OPTIONS_NAME, DeploymentOptions.class);

            if (log.isInfoEnabled()) {
                log.info("server options find, will use it's options!");
            }

        } catch (NoSuchBeanException e) {

            if (log.isWarnEnabled()) {
                log.warn("no server options find, so will use default options, " +
                        "but we recommend you using customer options!");
            }

            options = new DeploymentOptions();

        }
        return options;
    }

    @Override
    protected void registerProxyProcessorBean() throws BeansException {
        BeanDefinition beanDefinition = BeanDefinition.builder()
                .setBeanName("verticleProxyProcessor")
                .setBeanClass(VerticleProxyProcessor.class)
                .setScope(BeanDefinition.SINGLETON)
                .build();
        getBeanFactory().registerBeanDefinition(beanDefinition.getBeanName(), beanDefinition);
    }

    @Override
    protected void registerBean() throws BeansException {
        super.registerBean();
        registerVertx();
        registerRouter();
        registerWebServer();
    }

    private void registerVertx() throws BeansException {
        try {
            vertx = getBeanFactory().getBean(Vertx.class);
        } catch (NoSuchBeanException e) {
            if (log.isDebugEnabled()) {
                log.debug("can't find Vertx, will use default!");
            }
            getBeanFactory().registerSingleton("vertx", VertxUtils.getVertx());
            vertx = getBeanFactory().getBean(Vertx.class);
        }
    }

    private void registerRouter() throws BeansException {
        try {
            router = getBeanFactory().getBean(Router.class);
        } catch (NoSuchBeanException e) {
            if (log.isDebugEnabled()) {
                log.debug("can't find Vertx, will use default!");
            }
            getBeanFactory().registerSingleton("router", Router.router(vertx));
            router = getBeanFactory().getBean(Router.class);
        }
    }

    private void registerWebServer() throws BeansException {
        BeanDefinitionRegistry registry = getBeanFactory();
        BeanDefinition beanDefinition = BeanDefinition.builder()
                .setScope(BeanDefinition.SINGLETON)
                .setBeanClass(DefaultWebServer.class)
                .setBeanName("webserver")
                .build();
        registry.registerBeanDefinition(beanDefinition.getBeanName(), beanDefinition);
    }

    @Override
    protected void processEnvironment() throws BeansException {
        super.processEnvironment();
        try {
            for (VertxAware vertxAware : getBeanFactory().getBeans(VertxAware.class).values()) {
                vertxAware.setVertx(vertx);
            }
        } catch (NoSuchBeanException e) {
            if (log.isDebugEnabled()) {
                log.debug("no vertx aware found!");
            }
        }

        try {
            for (RouterAware routerAware : getBeanFactory().getBeans(RouterAware.class).values()) {
                routerAware.setRouter(router);
            }
        } catch (NoSuchBeanException e) {
            if (log.isDebugEnabled()) {
                log.debug("no router aware found!");
            }
        }

    }

    @Override
    protected void doClose() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.close(res -> {
            if (res.succeeded()) {
                if (log.isInfoEnabled()) {
                    log.info("closed vertx!");
                }
            }
            latch.countDown();
        });
        latch.await();
        super.doClose();
    }

}
