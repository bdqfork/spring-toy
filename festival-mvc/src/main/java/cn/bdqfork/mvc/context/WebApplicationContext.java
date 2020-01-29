package cn.bdqfork.mvc.context;

import cn.bdqfork.context.AnnotationApplicationContext;
import cn.bdqfork.core.exception.BeansException;
import cn.bdqfork.core.exception.NoSuchBeanException;
import cn.bdqfork.core.factory.BeanFactory;
import cn.bdqfork.core.factory.ConfigurableBeanFactory;
import cn.bdqfork.core.factory.definition.BeanDefinition;
import cn.bdqfork.core.factory.registry.BeanDefinitionRegistry;
import cn.bdqfork.mvc.context.service.HessianMessageCodec;
import cn.bdqfork.mvc.processor.VerticleProxyProcessor;
import cn.bdqfork.mvc.util.VertxUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.Vertx;
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

    public WebApplicationContext(String... scanPaths) throws BeansException {
        super(scanPaths);

        BeanFactory beanFactory = getConfigurableBeanFactory();

        vertx.eventBus().registerCodec(new HessianMessageCodec());

        WebSeverRunner runner = beanFactory.getBean(WebSeverRunner.class);

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
        vertx.deployVerticle(runner, options);
    }

    @Override
    protected void registerProxyProcessorBean() throws BeansException {
        BeanDefinition beanDefinition = BeanDefinition.builder()
                .setBeanName("verticleProxyProcessor")
                .setBeanClass(VerticleProxyProcessor.class)
                .setScope(BeanDefinition.SINGLETON)
                .build();
        getConfigurableBeanFactory().registerBeanDefinition(beanDefinition.getBeanName(), beanDefinition);
    }

    @Override
    protected void registerBean() throws BeansException {
        super.registerBean();
        registerVertx();
        registerWebServer();
    }

    private void registerVertx() throws BeansException {
        try {
            vertx = getConfigurableBeanFactory().getBean(Vertx.class);
        } catch (NoSuchBeanException e) {
            if (log.isTraceEnabled()) {
                log.trace("can't find Vertx, will use default!");
            }
            getConfigurableBeanFactory().registerSingleton("vertx", VertxUtils.getVertx());
            vertx = getConfigurableBeanFactory().getBean(Vertx.class);
        }
    }

    private void registerWebServer() throws BeansException {
        BeanDefinitionRegistry registry = getConfigurableBeanFactory();
        if (registry.containBeanDefinition("webserver")) {
            return;
        }
        BeanDefinition beanDefinition = BeanDefinition.builder()
                .setScope(BeanDefinition.SINGLETON)
                .setBeanClass(WebSeverRunner.class)
                .setBeanName("webserver")
                .build();
        registry.registerBeanDefinition(beanDefinition.getBeanName(), beanDefinition);
    }

    @Override
    protected void processEnvironment() throws BeansException {
        super.processEnvironment();
        for (VertxAware vertxAware : getConfigurableBeanFactory().getBeans(VertxAware.class).values()) {
            vertxAware.setVertx(vertx);
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
