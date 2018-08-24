package com.oembedler.moon.graphiql.boot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Andrew Potter
 */
@Controller
public class GraphiQLController {

    private static final String CDNJS_CLOUDFLARE_COM_AJAX_LIBS_GRAPHIQL = "//cdnjs.cloudflare.com/ajax/libs/graphiql/";
    @Value("${graphiql.endpoint:/graphql}")
    private String graphqlEndpoint;

    @Value("${graphiql.pageTitle:GraphiQL}")
    private String pageTitle;

    @Value("${graphiql.cdn.enabled:false}")
    private Boolean graphiqlCdnEnabled;

    @Value("${graphiql.cdn.version:0.11.11}")
    private String graphiqlCdnVersion;

    @Autowired
    private Environment environment;

    private String template;
    private String props;
    private String headers;

    @PostConstruct
    public void onceConstructed() throws IOException {
        loadTemplate();
        loadProps();
        loadHeaders();
    }

    private void loadTemplate() throws IOException {
        try (InputStream inputStream = new ClassPathResource("graphiql.html").getInputStream()) {
            template = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
        }
    }

    private void loadProps() throws IOException {
        props = new PropsLoader(environment).load();
    }

    private void loadHeaders() throws JsonProcessingException {
        PropertyGroupReader propertyReader = new PropertyGroupReader(environment, "graphiql.headers.");
        Properties headerProperties = propertyReader.load();
        addIfAbsent(headerProperties, "Accept");
        addIfAbsent(headerProperties, "Content-Type");
        this.headers = new ObjectMapper().writeValueAsString(headerProperties);
    }

    private void addIfAbsent(Properties headerProperties, String header) {
        if (!headerProperties.containsKey(header)) {
            headerProperties.setProperty(header, MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @RequestMapping(value = "${graphiql.mapping:/graphiql}")
    public void graphiql(HttpServletRequest request, HttpServletResponse response, @PathVariable Map<String, String> params) throws IOException {
        response.setContentType("text/html; charset=UTF-8");

        String endpoint = constructGraphQlEndpoint(request, params);
        Map<String, String> replacements = getReplacements(endpoint);

        String populatedTemplate = StrSubstitutor.replace(template, replacements);
        populatedTemplate = addContextPathIfEnabled(request, populatedTemplate);
        response.getOutputStream().write(populatedTemplate.getBytes(Charset.defaultCharset()));
    }

    private Map<String, String> getReplacements(String endpoint) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("graphqlEndpoint", endpoint);
        replacements.put("pageTitle", pageTitle);
        replacements.put("graphiqlCssUrl", graphiqlUrl("graphiql.min.css"));
        replacements.put("graphiqlJsUrl", graphiqlUrl("graphiql.min.js"));
        replacements.put("props", props);
        replacements.put("headers", headers);
        return replacements;
    }

    private String graphiqlUrl(String filename) {
        if (graphiqlCdnEnabled && StringUtils.isNotBlank(graphiqlCdnVersion)) {
            return CDNJS_CLOUDFLARE_COM_AJAX_LIBS_GRAPHIQL + graphiqlCdnVersion + "/" + filename;
        }
        return "/vendor/" + filename;
    }

    private String addContextPathIfEnabled(HttpServletRequest request, String populatedTemplate) {
        if (StringUtils.isNotBlank(request.getContextPath())) {
            String vendorPathWithContext = String.format("%s/vendor", request.getContextPath());
            populatedTemplate = populatedTemplate
                    .replaceAll("src=\"/vendor", "src=\"" + vendorPathWithContext)
                    .replaceAll("href=\"/vendor", "href=\"" + vendorPathWithContext);
        }
        return populatedTemplate;
    }

    private String constructGraphQlEndpoint(HttpServletRequest request, @RequestParam Map<String, String> params) {
        String endpoint = graphqlEndpoint;
        for (Map.Entry<String, String> param : params.entrySet()) {
            endpoint = endpoint.replaceAll("\\{" + param.getKey() + "}", param.getValue());
        }
        if (StringUtils.isNotBlank(request.getContextPath()) && !endpoint.startsWith(request.getContextPath())) {
            return request.getContextPath() + endpoint;
        }
        return endpoint;
    }

}
