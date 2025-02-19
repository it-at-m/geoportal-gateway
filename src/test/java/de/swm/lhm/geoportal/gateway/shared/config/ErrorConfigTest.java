package de.swm.lhm.geoportal.gateway.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ErrorConfigTest {

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private WebProperties webProperties;

    @Mock
    private ObjectProvider<ViewResolver> viewResolversProvider;

    @Mock
    private ServerCodecConfigurer serverCodecConfigurer;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private ErrorConfig errorConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Ensure ClassLoader is correctly provided
        when(applicationContext.getClassLoader()).thenReturn(this.getClass().getClassLoader());
    }

    @Test
    void testErrorWebExceptionHandler() {
        ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
        WebProperties.Resources resources = new WebProperties.Resources();
        resources.setStaticLocations(new String[]{"classpath:/static/"});

        when(webProperties.getResources()).thenReturn(resources);

        ErrorProperties errorProperties = new ErrorProperties();  // Create a new instance instead of mocking
        when(serverProperties.getError()).thenReturn(errorProperties);

        when(viewResolversProvider.orderedStream()).thenReturn(Stream.empty());
        when(serverCodecConfigurer.getReaders()).thenReturn(Collections.emptyList());
        when(serverCodecConfigurer.getWriters()).thenReturn(Collections.emptyList());

        ErrorWebExceptionHandler exceptionHandler = errorConfig.errorWebExceptionHandler(
                errorAttributes, webProperties, viewResolversProvider, serverCodecConfigurer, applicationContext);

        assertNotNull(exceptionHandler);
        assertInstanceOf(DefaultErrorWebExceptionHandler.class, exceptionHandler);
    }

    @Test
    void testErrorAttributes() {
        DefaultErrorAttributes errorAttributes = errorConfig.errorAttributes();
        assertNotNull(errorAttributes);
        assertInstanceOf(DefaultErrorAttributes.class, errorAttributes);
    }
}