package com.anode.workflow.spring.autoconfigure.registry;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import com.anode.workflow.service.SlaQueueManager;
import com.anode.workflow.spring.autoconfigure.util.BeanNameUtils;

/**
 * Registers SLA queue manager beans discovered via classpath scanning.
 *
 * <p>This registrar automatically discovers classes annotated with
 * {@code @SlaQueueManagerComponent} and registers them as Spring beans.
 * SLA queue managers handle priority-based task queuing and ensure workflow
 * tasks are processed within specified time constraints.
 *
 * @see com.anode.workflow.spring.autoconfigure.annotations.SlaQueueManagerComponent
 */
public class SlaQueueManagerRegistrar implements ImportBeanDefinitionRegistrar {

    /**
     * Registers bean definitions for all discovered SLA queue manager classes.
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

            // Scan for annotated classes - limit scope for better performance
            var resources = resolver.getResources("classpath*:com/**/*.class");

            for (var resource : resources) {
                if (!resource.isReadable()) continue;

                var reader = readerFactory.getMetadataReader(resource);
                var annTypes = reader.getAnnotationMetadata().getAnnotationTypes();

                // Look only for the custom annotation
                if (!annTypes.contains("com.anode.workflow.spring.autoconfigure.annotations.SlaQueueManagerComponent"))
                    continue;

                String className = reader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(className);

                // Ensure it implements the interface
                if (!SlaQueueManager.class.isAssignableFrom(clazz))
                    continue;

                GenericBeanDefinition bd = new GenericBeanDefinition();
                bd.setBeanClass(clazz);

                // Create a standard bean name using utility
                String beanName = BeanNameUtils.deriveBeanName(clazz);

                registry.registerBeanDefinition(beanName, bd);
            }

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to scan classpath for @SlaQueueManagerComponent classes", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load @SlaQueueManagerComponent class", e);
        }
    }
}
