package com.oembedler.moon.graphql.boot.test.instrumentation;

import com.oembedler.moon.graphql.boot.GraphQLInstrumentationAutoConfiguration;
import com.oembedler.moon.graphql.boot.GraphQLWebAutoConfiguration;
import com.oembedler.moon.graphql.boot.test.AbstractAutoConfigurationTest;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.servlet.AbstractGraphQLHttpServlet;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * @author Marcel Overdijk
 */
public class GraphQLInstrumentationAutoConfigurationTest extends AbstractAutoConfigurationTest {

    public GraphQLInstrumentationAutoConfigurationTest() {
        super(AnnotationConfigWebApplicationContext.class, GraphQLInstrumentationAutoConfiguration.class);
    }

    @Configuration
    static class DefaultConfiguration {

        @Bean
        GraphQLSchema schema() {
            return GraphQLSchema.newSchema().query(GraphQLObjectType.newObject().name("Query").build()).build();
        }

    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void noDefaultInstrumentations() {
        load(DefaultConfiguration.class);

        this.getContext().getBean(Instrumentation.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void tracingInstrumentationDisabled() {
        load(DefaultConfiguration.class, "graphql.servlet.tracingEnabled=false");

        this.getContext().getBean(TracingInstrumentation.class);
    }

    @Test
    public void tracingInstrumentationEnabled() {
        load(DefaultConfiguration.class, "graphql.servlet.tracingEnabled=true");

        Assert.assertNotNull(this.getContext().getBean(TracingInstrumentation.class));
    }

    @Test
    public void maxQueryComplexityEnabled() {
        load(DefaultConfiguration.class, "graphql.servlet.maxQueryComplexity=10");

        Assert.assertNotNull(this.getContext().getBean(MaxQueryComplexityInstrumentation.class));
    }

    @Test
    public void maxQueryDepthEnabled() {
        load(DefaultConfiguration.class, "graphql.servlet.maxQueryDepth=10");

        Assert.assertNotNull(this.getContext().getBean(MaxQueryDepthInstrumentation.class));
    }
}
