package com.oembedler.moon.graphql.boot.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import org.springframework.stereotype.Component;

@Component
class Query implements GraphQLQueryResolver {

    Post getPost(Long id) {
        return new Post(id);
    }

}
