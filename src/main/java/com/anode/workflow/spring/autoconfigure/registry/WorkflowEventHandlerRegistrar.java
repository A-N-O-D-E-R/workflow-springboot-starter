package com.anode.workflow.spring.autoconfigure.registry;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import com.anode.workflow.service.EventHandler;

public class WorkflowEventHandlerRegistrar implements ImportBeanDefinitionRegistrar {

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
                var ann = reader.getAnnotationMetadata().getAnnotationTypes();

                // Does it have our annotation?
                if (!ann.contains("com.anode.workflow.spring.autoconfigure.annotations.WorkflowEventHandler"))
                    continue;

                String className = reader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(className);

                // Must implement EventHandler
                if (!EventHandler.class.isAssignableFrom(clazz))
                    continue;

                GenericBeanDefinition bd = new GenericBeanDefinition();
                bd.setBeanClass(clazz);

                // Bean name = simple class name camelCase
                String beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0))
                        + clazz.getSimpleName().substring(1);

                registry.registerBeanDefinition(beanName, bd);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed scanning @WorkflowEventHandler classes", e);
        }
    }
}
