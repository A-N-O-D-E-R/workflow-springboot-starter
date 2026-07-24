package com.anode.workflow.spring.autoconfigure.registry;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.spring.autoconfigure.util.BeanNameUtils;

/**
 * Registers workflow component factory beans discovered via classpath scanning.
 *
 * <p>This registrar automatically discovers classes annotated with
 * {@code @WorkflowComponentFactory} and registers them as Spring beans.
 * It scans the classpath during application startup and registers all
 * component factories for injection into other Spring components.
 *
 * @see com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory
 */
public class WorkflowComponentFactoryRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String DEFAULT_BASE_PACKAGE = "com.anode";
    private static final String SCAN_PACKAGE_PROPERTY = "workflow.component-factory.scan-base-package";

    /**
     * Registers bean definitions for all discovered workflow component factory classes.
     *
     * @param metadata the annotation metadata
     * @param registry the bean definition registry
     * @throws RuntimeException if classpath scanning or bean registration fails
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();

            // Get base package from system property or use default
            String basePackage = System.getProperty(SCAN_PACKAGE_PROPERTY, DEFAULT_BASE_PACKAGE);
            String searchPath = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";

            // Scan for annotated classes with more specific scope
            var resources = resolver.getResources(searchPath);

            for (var resource : resources) {
                if (!resource.isReadable()) continue;

                var reader = readerFactory.getMetadataReader(resource);
                var anns = reader.getAnnotationMetadata().getAnnotationTypes();

                if (!anns.contains("com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory"))
                    continue;

                String className = reader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(className);

                // Must implement WorkflowComponantFactory
                if (!WorkflowComponantFactory.class.isAssignableFrom(clazz))
                    continue;

                GenericBeanDefinition bd = new GenericBeanDefinition();
                bd.setBeanClass(clazz);

                String beanName = BeanNameUtils.deriveBeanName(clazz);

                registry.registerBeanDefinition(beanName, bd);
            }

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to scan classpath for @WorkflowComponentFactory classes", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load @WorkflowComponentFactory class", e);
        }
    }
}
