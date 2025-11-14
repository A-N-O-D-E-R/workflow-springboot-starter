package com.anode.workflow.spring.autoconfigure.registry;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import com.anode.workflow.service.SlaQueueManager;

public class SlaQueueManagerRegistrar implements ImportBeanDefinitionRegistrar {

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

                // Create a standard bean name
                String beanName =
                    Character.toLowerCase(clazz.getSimpleName().charAt(0))
                    + clazz.getSimpleName().substring(1);

                registry.registerBeanDefinition(beanName, bd);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed scanning @SlaQueueManagerComponent classes", e);
        }
    }
}
